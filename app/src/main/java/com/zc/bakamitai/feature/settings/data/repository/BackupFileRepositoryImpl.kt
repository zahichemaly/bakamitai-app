package com.zc.bakamitai.feature.settings.data.repository

import com.zc.bakamitai.common.domain.util.TimeZoneProvider
import com.zc.bakamitai.feature.settings.data.dto.BackupDTO
import com.zc.bakamitai.feature.settings.data.local.BackupDocument
import com.zc.bakamitai.feature.settings.data.local.BackupDocumentsLocalDataSource
import com.zc.bakamitai.feature.settings.data.mapper.BACKUP_FORMAT_VERSION
import com.zc.bakamitai.feature.settings.data.mapper.toDTO
import com.zc.bakamitai.feature.settings.data.mapper.toDomain
import com.zc.bakamitai.feature.settings.domain.model.Backup
import com.zc.bakamitai.feature.settings.domain.model.BackupError
import com.zc.bakamitai.feature.settings.domain.model.BackupFolder
import com.zc.bakamitai.feature.settings.domain.repository.BackupFileRepository
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import java.io.IOException
import kotlin.time.Instant

/** A backup file's MIME type. */
private const val BACKUP_MIME_TYPE = "application/json"

/** `bakamitai_2026-11-26_10-47-05.json`: only files named like this are ever deleted. */
private val BackupFileNamePattern = Regex("""bakamitai_\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.json""")

private val BackupTimestampFormat = LocalDateTime.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    day()
    char('_')
    hour()
    char('-')
    minute()
    char('-')
    second()
}

/**
 * Backup files as JSON (docs/7-backup.md §4.1) in the user's folder, through the Storage Access Framework. Platform
 * failures become [BackupError]s: on the folder, [BackupError.FolderUnavailable]; on a file to restore,
 * [BackupError.InvalidFile]. Cancellation isn't caught.
 */
@Single
class BackupFileRepositoryImpl(
    private val documents: BackupDocumentsLocalDataSource,
    private val json: Json,
    private val timeZoneProvider: TimeZoneProvider,
) : BackupFileRepository {

    override suspend fun grantFolder(uri: String, previous: BackupFolder?): Result<BackupFolder> = folderCall {
        val name = documents.takeFolder(uri)
        if (previous != null && previous.uri != uri) documents.releaseFolder(previous.uri)
        BackupFolder(uri = uri, name = name)
    }

    override suspend fun write(folder: BackupFolder, backup: Backup): Result<String> = folderCall {
        val name = backupFileName(backup.createdAt, timeZoneProvider.current())
        val text = json.encodeToString(BackupDTO.serializer(), backup.toDTO())
        documents.createFile(folder.uri, name, BACKUP_MIME_TYPE, text)
        name
    }

    override suspend fun read(uri: String): Result<Backup> {
        val text = fileCall { documents.readText(uri) }.getOrElse { return Result.failure(it) }
        return decode(text)
    }

    override suspend fun deleteOldBackups(folder: BackupFolder, keep: Int): Result<Unit> = folderCall {
        documents.listFiles(folder.uri).backupsToDelete(keep).forEach { documents.delete(it.uri) }
    }

    private fun decode(text: String): Result<Backup> = try {
        val dto = json.decodeFromString(BackupDTO.serializer(), text)
        if (dto.version > BACKUP_FORMAT_VERSION) {
            Result.failure(BackupError.UnsupportedVersion)
        } else {
            Result.success(dto.toDomain())
        }
    } catch (e: SerializationException) {
        Result.failure(BackupError.InvalidFile(e))
    } catch (e: IllegalArgumentException) {
        Result.failure(BackupError.InvalidFile(e))
    }

    /** Runs a call on the backup folder; platform failures are [BackupError.FolderUnavailable]. */
    private suspend fun <T> folderCall(block: suspend () -> T): Result<T> =
        platformCall(block) { BackupError.FolderUnavailable(it) }

    /** Runs a call on a file to restore; platform failures are [BackupError.InvalidFile]. */
    private suspend fun <T> fileCall(block: suspend () -> T): Result<T> =
        platformCall(block) { BackupError.InvalidFile(it) }

    private suspend fun <T> platformCall(block: suspend () -> T, toError: (Throwable) -> BackupError): Result<T> = try {
        Result.success(block())
    } catch (e: IOException) {
        Result.failure(toError(e))
    } catch (e: SecurityException) {
        Result.failure(toError(e))
    } catch (e: IllegalArgumentException) {
        Result.failure(toError(e))
    } catch (e: UnsupportedOperationException) {
        Result.failure(toError(e))
    }
}

/** The name of a backup created at [createdAt], in [timeZone]: `bakamitai_2026-11-26_10-47-05.json`. */
fun backupFileName(createdAt: Instant, timeZone: TimeZone): String =
    "bakamitai_${BackupTimestampFormat.format(createdAt.toLocalDateTime(timeZone))}.json"

/**
 * The BakaMitai backups among [this] folder's files that go so that the newest [keep] stay (newest by name, which
 * is by date). Other files are never included.
 */
fun List<BackupDocument>.backupsToDelete(keep: Int): List<BackupDocument> =
    filter { BackupFileNamePattern.matches(it.name) }
        .sortedByDescending { it.name }
        .drop(keep.coerceAtLeast(0))

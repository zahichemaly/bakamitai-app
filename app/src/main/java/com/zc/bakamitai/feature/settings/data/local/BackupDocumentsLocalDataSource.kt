package com.zc.bakamitai.feature.settings.data.local

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.zc.bakamitai.common.domain.util.DispatcherProvider
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Files in a folder the user chose through the Storage Access Framework (a document tree), read and written with
 * the `ContentResolver` on the I/O dispatcher. Folder and file ids are URI strings.
 *
 * Failures are the platform's: `IOException` (incl. `FileNotFoundException`), `SecurityException` (the app's access
 * is gone), `IllegalArgumentException` or `UnsupportedOperationException` (a URI the provider doesn't accept).
 */
@Single
class BackupDocumentsLocalDataSource(context: Context, private val dispatchers: DispatcherProvider) {

    private val resolver: ContentResolver = context.contentResolver

    /** Keeps read and write access to the folder [treeUri] across restarts; returns the folder's display name. */
    suspend fun takeFolder(treeUri: String): String = withContext(dispatchers.io) {
        val uri = Uri.parse(treeUri)
        resolver.takePersistableUriPermission(uri, FOLDER_ACCESS)
        val folder = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        queryDisplayName(folder) ?: DocumentsContract.getTreeDocumentId(uri).substringAfterLast(':')
    }

    /** Gives up the access kept by [takeFolder]. A folder that wasn't held is ignored. */
    suspend fun releaseFolder(treeUri: String) {
        withContext(dispatchers.io) {
            val uri = Uri.parse(treeUri)
            if (resolver.persistedUriPermissions.any { it.uri == uri }) {
                resolver.releasePersistableUriPermission(uri, FOLDER_ACCESS)
            }
        }
    }

    /** Creates the file [name] in the folder [treeUri] and writes [text] to it (UTF-8). Returns its URI. */
    suspend fun createFile(treeUri: String, name: String, mimeType: String, text: String): String =
        withContext(dispatchers.io) {
            val tree = Uri.parse(treeUri)
            val folder = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val file = DocumentsContract.createDocument(resolver, folder, mimeType, name)
                ?: throw IOException("The provider couldn't create $name")
            val stream = resolver.openOutputStream(file, WRITE_MODE)
                ?: throw FileNotFoundException("The provider couldn't open $name")
            stream.bufferedWriter().use { it.write(text) }
            file.toString()
        }

    /** The text (UTF-8) of the file [uri]. */
    suspend fun readText(uri: String): String = withContext(dispatchers.io) {
        val stream = resolver.openInputStream(Uri.parse(uri)) ?: throw FileNotFoundException("Can't open $uri")
        stream.bufferedReader().use { it.readText() }
    }

    /** The files directly in the folder [treeUri]. */
    suspend fun listFiles(treeUri: String): List<BackupDocument> = withContext(dispatchers.io) {
        val tree = Uri.parse(treeUri)
        val children =
            DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )
        val cursor = resolver.query(children, columns, null, null, null)
            ?: throw IOException("The provider couldn't list $treeUri")
        cursor.use {
            buildList {
                while (it.moveToNext()) {
                    val name = it.getString(1) ?: continue
                    val uri = DocumentsContract.buildDocumentUriUsingTree(tree, it.getString(0))
                    add(BackupDocument(name = name, uri = uri.toString()))
                }
            }
        }
    }

    /** Deletes the file [uri]. */
    suspend fun delete(uri: String) {
        withContext(dispatchers.io) {
            if (!DocumentsContract.deleteDocument(resolver, Uri.parse(uri))) throw IOException("Can't delete $uri")
        }
    }

    private fun queryDisplayName(document: Uri): String? =
        resolver.query(document, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    private companion object {
        const val FOLDER_ACCESS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        /** A new, empty file: plain "w" (some providers reject "wt"). */
        const val WRITE_MODE = "w"
    }
}

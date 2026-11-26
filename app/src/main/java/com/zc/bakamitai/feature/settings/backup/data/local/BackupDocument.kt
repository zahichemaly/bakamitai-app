package com.zc.bakamitai.feature.settings.backup.data.local

/**
 * A file in the backup folder, as the documents provider lists it.
 *
 * @property uri the file's document URI.
 */
data class BackupDocument(val name: String, val uri: String)

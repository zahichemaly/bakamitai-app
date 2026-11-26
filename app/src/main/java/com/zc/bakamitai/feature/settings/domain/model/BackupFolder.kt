package com.zc.bakamitai.feature.settings.domain.model

/**
 * The folder backups are written to, chosen by the user.
 *
 * @property uri the folder's opaque id (a document tree URI); only `data` reads it.
 * @property name the folder's name as the user saw it when choosing it ("Backups").
 */
data class BackupFolder(val uri: String, val name: String)

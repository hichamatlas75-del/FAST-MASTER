package com.example.hichamjeunemaster.data

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Collections

class DriveServiceHelper(private val driveService: Drive) {

    companion object {
        private const val BACKUP_FILE_NAME = "fastmaster_backup.json"

        fun getDriveService(credential: GoogleAccountCredential): Drive {
            return Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
            .setApplicationName("Fast Master")
            .build()
        }
    }

    /**
     * Sauvegarde les données dans le dossier appDataFolder caché de l'utilisateur.
     */
    suspend fun backupData(jsonData: String): Boolean = withContext(Dispatchers.IO) {
        // Chercher si le fichier existe déjà
        val fileId = getBackupFileId()

        val content = ByteArrayContent.fromString("application/json", jsonData)

        if (fileId != null) {
            // Mettre à jour le fichier existant
            val fileMeta = com.google.api.services.drive.model.File().setName(BACKUP_FILE_NAME)
            driveService.files().update(fileId, fileMeta, content).execute()
        } else {
            // Créer un nouveau fichier dans appDataFolder
            val fileMeta = com.google.api.services.drive.model.File()
                .setName(BACKUP_FILE_NAME)
                .setParents(Collections.singletonList("appDataFolder"))
            driveService.files().create(fileMeta, content).execute()
        }
        true
    }

    /**
     * Restaure les données depuis le dossier appDataFolder.
     */
    suspend fun restoreData(): String? = withContext(Dispatchers.IO) {
        try {
            val fileId = getBackupFileId() ?: return@withContext null
            val outputStream = ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.toString("UTF-8")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getBackupFileId(): String? {
        val fileList = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name='$BACKUP_FILE_NAME'")
            .setFields("files(id, name)")
            .execute()
        return fileList.files.firstOrNull()?.id
    }
}

package com.vozluma.app

import android.content.Context
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.zip.ZipInputStream

/**
 * Ubicación: app/src/main/java/com/vozluma/app/ModelManager.kt
 * Descarga el modelo oficial una sola vez; después Vosk funciona sin Internet.
 */
object ModelManager {
    private const val MODEL_DIRECTORY = "model-es"
    private const val DOWNLOAD_FILE = "model-es.zip.part"
    private const val MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip"
    private const val BUFFER_SIZE = 16 * 1024

    private val executor = Executors.newSingleThreadExecutor()

    fun modelDirectory(context: Context): File = File(context.filesDir, MODEL_DIRECTORY)

    fun isReady(context: Context): Boolean =
        File(modelDirectory(context), "am/final.mdl").exists() &&
            File(modelDirectory(context), "graph/HCLr.fst").exists()

    fun download(
        context: Context,
        onProgress: (Int) -> Unit,
        onComplete: (Result<File>) -> Unit
    ) {
        if (isReady(context)) {
            onComplete(Result.success(modelDirectory(context)))
            return
        }

        executor.execute {
            val result = runCatching { downloadAndExtract(context, onProgress) }
            onComplete(result)
        }
    }

    private fun downloadAndExtract(context: Context, onProgress: (Int) -> Unit): File {
        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            requestMethod = "GET"
        }
        connection.connect()
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("No se pudo descargar el modelo: HTTP ${connection.responseCode}")
        }

        val zipFile = File(context.cacheDir, DOWNLOAD_FILE)
        val contentLength = connection.contentLengthLong
        connection.inputStream.use { input ->
            BufferedInputStream(input).use { buffered ->
                FileOutputStream(zipFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var downloaded = 0L
                    var read: Int
                    while (buffered.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (contentLength > 0) {
                            onProgress(((downloaded * 50L) / contentLength).toInt().coerceIn(0, 50))
                        }
                    }
                }
            }
        }
        connection.disconnect()

        val destination = modelDirectory(context)
        val temporaryDirectory = File(context.filesDir, "$MODEL_DIRECTORY.tmp")
        temporaryDirectory.deleteRecursively()
        temporaryDirectory.mkdirs()
        unzipSafely(zipFile, temporaryDirectory, onProgress)

        val extractedRoot = temporaryDirectory.listFiles()?.firstOrNull { it.isDirectory }
            ?: throw IllegalStateException("El paquete del modelo no tiene una carpeta válida")
        destination.deleteRecursively()
        if (!extractedRoot.renameTo(destination)) {
            extractedRoot.copyRecursively(destination, overwrite = true)
            temporaryDirectory.deleteRecursively()
        }
        zipFile.delete()

        if (!isReady(context)) throw IllegalStateException("El modelo descargado está incompleto")
        onProgress(100)
        return destination
    }

    private fun unzipSafely(zipFile: File, destination: File, onProgress: (Int) -> Unit) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            val buffer = ByteArray(BUFFER_SIZE)
            var entry = zip.nextEntry
            var count = 0
            while (entry != null) {
                val target = File(destination, entry.name)
                val canonicalDestination = destination.canonicalPath + File.separator
                if (!target.canonicalPath.startsWith(canonicalDestination)) {
                    throw SecurityException("Entrada de modelo no válida")
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { output ->
                        var read: Int
                        while (zip.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                        }
                    }
                }
                zip.closeEntry()
                count++
                onProgress((50 + (count % 50)).coerceAtMost(99))
                entry = zip.nextEntry
            }
        }
    }
}

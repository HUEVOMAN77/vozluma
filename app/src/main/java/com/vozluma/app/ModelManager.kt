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
 * Instala el modelo oficial español incluido en la APK y conserva una descarga
 * de respaldo para futuras compilaciones que no incluyan el asset.
 * Después de la instalación, Vosk funciona sin Internet.
 */
object ModelManager {
    private const val MODEL_DIRECTORY = "model-es"
    private const val DOWNLOAD_FILE = "model-es.zip.part"
    private const val BUNDLED_MODEL_ASSET = "vosk-model-small-es-0.42.zip"
    private const val MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip"
    private const val BUFFER_SIZE = 16 * 1024
    private const val MAX_ATTEMPTS = 3

    private val executor = Executors.newSingleThreadExecutor()

    fun modelDirectory(context: Context): File = File(context.filesDir, MODEL_DIRECTORY)

    fun isReady(context: Context): Boolean {
        val model = modelDirectory(context)
        return File(model, "am/final.mdl").isFile &&
            File(model, "graph/HCLr.fst").isFile &&
            File(model, "graph/Gr.fst").isFile
    }

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
            val result = runCatching {
                onProgress(1)
                // La APK nueva ya contiene el modelo. Así la activación puede
                // quedar funcional aunque el teléfono no tenga Internet.
                installBundledModel(context, onProgress)
                    ?: downloadFromNetwork(context, onProgress)
            }
            onComplete(result)
        }
    }

    private fun installBundledModel(context: Context, onProgress: (Int) -> Unit): File? {
        val assetExists = runCatching {
            context.assets.open(BUNDLED_MODEL_ASSET).use { }
            true
        }.getOrDefault(false)
        if (!assetExists) return null

        val zipFile = File(context.cacheDir, "bundled-$DOWNLOAD_FILE")
        return try {
            context.assets.open(BUNDLED_MODEL_ASSET).use { input ->
                copyWithProgress(input, zipFile, onProgress, 1, 42)
            }
            extractAndInstall(context, zipFile, onProgress, 42, 100)
        } finally {
            zipFile.delete()
        }
    }

    private fun downloadFromNetwork(context: Context, onProgress: (Int) -> Unit): File {
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                return downloadAndExtract(context, onProgress, attempt)
            } catch (error: Throwable) {
                lastError = error
                File(context.cacheDir, DOWNLOAD_FILE).delete()
                if (attempt + 1 < MAX_ATTEMPTS) Thread.sleep(1_000L * (attempt + 1))
            }
        }
        throw IllegalStateException(
            "No se pudo instalar el modelo después de $MAX_ATTEMPTS intentos: ${lastError?.message}",
            lastError
        )
    }

    private fun downloadAndExtract(
        context: Context,
        onProgress: (Int) -> Unit,
        attempt: Int
    ): File {
        val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 120_000
            requestMethod = "GET"
            instanceFollowRedirects = true
            useCaches = false
            setRequestProperty("Accept", "application/zip")
            setRequestProperty("User-Agent", "VozLumaPremium/3.1 Android")
        }

        try {
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Servidor del modelo respondió HTTP ${connection.responseCode}")
            }

            val zipFile = File(context.cacheDir, DOWNLOAD_FILE)
            val contentLength = connection.contentLengthLong
            connection.inputStream.use { input ->
                copyWithProgress(
                    BufferedInputStream(input),
                    zipFile,
                    onProgress,
                    attempt * 2,
                    48
                ) { downloaded ->
                    if (contentLength > 0) ((downloaded * 48L) / contentLength).toInt() else 0
                }
            }
            return extractAndInstall(context, zipFile, onProgress, 48, 100)
        } finally {
            connection.disconnect()
        }
    }

    private fun copyWithProgress(
        input: java.io.InputStream,
        destination: File,
        onProgress: (Int) -> Unit,
        startProgress: Int,
        endProgress: Int,
        progressForBytes: ((Long) -> Int)? = null
    ) {
        destination.parentFile?.mkdirs()
        FileOutputStream(destination).use { output ->
            val buffer = ByteArray(BUFFER_SIZE)
            var copied = 0L
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                copied += read
                val calculated = progressForBytes?.invoke(copied)
                onProgress(
                    (calculated ?: (startProgress + (copied / 1_000_000L).toInt()))
                        .coerceIn(startProgress, endProgress)
                )
            }
        }
    }

    private fun extractAndInstall(
        context: Context,
        zipFile: File,
        onProgress: (Int) -> Unit,
        startProgress: Int,
        endProgress: Int
    ): File {
        if (!zipFile.isFile || zipFile.length() < 1_000_000L) {
            throw IllegalStateException("El archivo del modelo está vacío o incompleto")
        }

        val temporaryDirectory = File(context.filesDir, "$MODEL_DIRECTORY.tmp")
        temporaryDirectory.deleteRecursively()
        temporaryDirectory.mkdirs()
        unzipSafely(zipFile, temporaryDirectory, onProgress, startProgress, endProgress)

        val extractedRoot = temporaryDirectory.listFiles()?.firstOrNull { it.isDirectory }
            ?: throw IllegalStateException("El paquete del modelo no tiene una carpeta válida")
        val destination = modelDirectory(context)
        destination.deleteRecursively()
        if (!extractedRoot.renameTo(destination)) {
            extractedRoot.copyRecursively(destination, overwrite = true)
        }
        temporaryDirectory.deleteRecursively()

        if (!isReady(context)) throw IllegalStateException("El modelo instalado está incompleto")
        onProgress(100)
        return destination
    }

    private fun unzipSafely(
        zipFile: File,
        destination: File,
        onProgress: (Int) -> Unit,
        startProgress: Int,
        endProgress: Int
    ) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            val buffer = ByteArray(BUFFER_SIZE)
            var entry = zip.nextEntry
            var count = 0
            while (entry != null) {
                val target = File(destination, entry.name)
                val canonicalDestination = destination.canonicalPath + File.separator
                if (!target.canonicalPath.startsWith(canonicalDestination)) {
                    throw SecurityException("Entrada del modelo no válida")
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
                onProgress((startProgress + (count % (endProgress - startProgress + 1))).coerceAtMost(endProgress - 1))
                entry = zip.nextEntry
            }
        }
    }
}

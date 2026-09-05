package dev1503.browseevo.download

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.kongzue.baseokhttp.x.Get
import com.kongzue.baseokhttp.x.util.BaseHttpRequest
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object DownloadController {
    private val activeRequests = ConcurrentHashMap<Long, BaseHttpRequest>()
    private val pauseFlags = ConcurrentHashMap<Long, AtomicBoolean>()
    private val activeDownloads = AtomicInteger(0)
    private lateinit var appContext: Context

    private const val PART_SUFFIX = ".evopart"

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
    }

    fun isActive(timestamp: Long): Boolean = activeRequests.containsKey(timestamp)

    fun cancel(context: Context, timestamp: Long, path: String?) {
        init(context)
        val appCtx = context.applicationContext
        pauseFlags.remove(timestamp)?.set(true)
        val removed = activeRequests.remove(timestamp)
        removed?.cancel()
        DownloadNotifier.cancelNotification(appCtx, timestamp)
        if (path != null) {
            activePaths.remove(path)
            File(path + PART_SUFFIX).delete()
        }
        if (removed != null && activeDownloads.decrementAndGet() <= 0 && ::appContext.isInitialized) {
            DownloadService.stop(appCtx)
        }
    }

    fun openFile(context: Context, record: DownloadRecord): Boolean {
        val file = File(record.path)
        if (!file.exists() || !file.isFile) return false
        if (record.filename.endsWith(".apk", ignoreCase = true)) {
            return installApk(context, file)
        }
        val uri = contentUri(context, file)
        val ext = record.filename.substringAfterLast('.', "").lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: "application/octet-stream"
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openFileWithApps(context: Context, record: DownloadRecord): Boolean {
        val file = File(record.path)
        if (!file.exists() || !file.isFile) return false
        val uri = contentUri(context, file)
        val ext = record.filename.substringAfterLast('.', "").lowercase()
        val mime = if (ext == "apk") {
            "application/vnd.android.package-archive"
        } else {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun installApk(context: Context, file: File): Boolean {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            try {
                val settings = Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    android.net.Uri.parse("package:${context.packageName}")
                )
                settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settings)
                return true
            } catch (e: Exception) {
            }
        }
        val uri = contentUri(context, file)
        val primary = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(primary)
            true
        } catch (e: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    private fun contentUri(context: Context, file: File) = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file
    )

    fun activeTimestamps(): Set<Long> = activeRequests.keys.toSet()

    fun interface OnProgressListener {
        fun onProgress(record: DownloadRecord)
    }

    private val progressListeners = java.util.concurrent.CopyOnWriteArrayList<OnProgressListener>()

    fun addProgressListener(listener: OnProgressListener) {
        progressListeners.addIfAbsent(listener)
    }

    fun removeProgressListener(listener: OnProgressListener) {
        progressListeners.remove(listener)
    }

    private fun dispatchProgress(record: DownloadRecord) {
        for (listener in progressListeners) {
            try {
                listener.onProgress(record)
            } catch (_: Exception) {
            }
        }
    }

    fun suggestFilename(url: String): String {
        return try {
            val httpUrl = url.toHttpUrlOrNull()
            val path = httpUrl?.pathSegments?.lastOrNull { it.isNotBlank() }
                ?: url.substringBefore('?').substringBefore('#').substringAfterLast('/')
            dev1503.browseevo.Utils.decodeUrlEncoded(path)
                .ifEmpty { "download_${System.currentTimeMillis()}" }
        } catch (e: Exception) {
            "download_${System.currentTimeMillis()}"
        }
    }

    fun publicDownloadDir(): File {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun uniqueDestination(dir: File, filename: String): File {
        val safe = filename.replace('/', '_').replace('\\', '_').ifEmpty { "download" }
        var file = File(dir, safe)
        if (!file.exists()) return file
        val dotIndex = safe.lastIndexOf('.')
        val base = if (dotIndex > 0) safe.substring(0, dotIndex) else safe
        val ext = if (dotIndex > 0) safe.substring(dotIndex) else ""
        var index = 1
        while (file.exists()) {
            file = File(dir, "$base($index)$ext")
            index++
        }
        return file
    }

    private val activePaths: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    @Synchronized
    fun start(context: Context, original: DownloadRecord): Boolean {
        init(context)
        val manager = DownloadManager(appContext)
        var record = original.copy(timestamp = System.currentTimeMillis(), paused = false)
        while (manager.get(record.timestamp) != null) {
            record = record.copy(timestamp = record.timestamp + 1)
        }
        var candidate = File(original.path)
        if (pathTaken(candidate)) {
            val dir = candidate.parentFile ?: publicDownloadDir()
            val baseName = original.filename
            var index = 0
            do {
                index++
                candidate = File(dir, uniqueNameFor(baseName, index))
            } while (pathTaken(candidate))
            record = record.copy(filename = candidate.name, path = candidate.absolutePath)
        }
        activePaths.add(candidate.absolutePath)
        manager.add(record)
        return launchDownload(record)
    }

    private fun pathTaken(candidate: File): Boolean =
        candidate.exists() ||
            File(candidate.absolutePath + PART_SUFFIX).exists() ||
            activePaths.contains(candidate.absolutePath)

    private fun uniqueNameFor(name: String, index: Int): String {
        val dot = name.lastIndexOf('.')
        return if (dot > 0) "${name.substring(0, dot)}($index)${name.substring(dot)}" else "$name($index)"
    }

    internal fun launchDownload(record: DownloadRecord, forceEnhancedTls: Boolean = false): Boolean {
        android.util.Log.i(TAG, "launchDownload url=${record.url} savedBytes=${record.savedBytes} path=${record.path}")
        activeDownloads.incrementAndGet()
        val pauseFlag = AtomicBoolean(false)
        pauseFlags[record.timestamp] = pauseFlag
        DownloadNotifier.notifyProgress(appContext, record)

        val finalFile = File(record.path)
        finalFile.parentFile?.mkdirs()
        val partFile = File(record.path + PART_SUFFIX)

        val resuming = record.savedBytes > 0 && finalFile.exists()
        val base = if (resuming) record.savedBytes else 0L
        if (!resuming) partFile.delete()

        val responseCode = AtomicInteger(-1)
        val absoluteTotal = AtomicLong(if (resuming && record.totalBytes > 0) record.totalBytes else -1L)
        val handled = AtomicBoolean(false)
        val merged = AtomicBoolean(false)

        val manager = DownloadManager(appContext)
        var lastNotify = 0L
        var lastCsvWrite = 0L
        val requestRef = java.util.concurrent.atomic.AtomicReference<BaseHttpRequest?>(null)

        fun mergePartIntoFinal(): Long {
            if (!merged.compareAndSet(false, true)) return finalFile.length()
            try {
                when (responseCode.get()) {
                    206 -> if (partFile.exists()) FileOutputStream(finalFile, true).use { output ->
                        partFile.inputStream().use { it.copyTo(output) }
                    }
                    200 -> {
                        finalFile.delete()
                        if (!partFile.renameTo(finalFile) && partFile.exists()) {
                            partFile.copyTo(finalFile, overwrite = true)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "mergePartIntoFinal failed", e)
            } finally {
                partFile.delete()
            }
            return finalFile.length()
        }

        fun cleanup(currentSession: Boolean) {
            if (currentSession && activeRequests[record.timestamp] === requestRef.get()) {
                pauseFlags.remove(record.timestamp)
                activeRequests.remove(record.timestamp)
                activePaths.remove(record.path)
            }
            if (activeDownloads.decrementAndGet() <= 0 && ::appContext.isInitialized) {
                DownloadService.stop(appContext)
            }
        }

        val useBundledCAs = forceEnhancedTls || Build.VERSION.SDK_INT < 24
        val trustManager =
            if (useBundledCAs) TlsCompat.enhancedTrustManager(appContext)
            else TlsCompat.defaultTrustManager()
        val sslContext = TlsCompat.newSslContext(trustManager)

        val client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                val code = response.code
                responseCode.set(code)
                val length = response.body?.contentLength() ?: -1L
                if (code !in 200..299) {
                    response.close()
                    throw IOException("HTTP $code")
                }
                when {
                    code == 206 && length >= 0 -> absoluteTotal.set(base + length)
                    code == 200 && length >= 0 -> absoluteTotal.set(length)
                }
                response
            }
            .build()

        val request = Get.create(record.url)
            .setOkHttpClient(client)
            .setTimeoutDuration(86400)
            .apply {
                if (resuming) addHeader("Range", "bytes=$base-")
            }
        requestRef.set(request)
        request.downloadToFile(partFile) { _, _, _, current, _, done, error ->
            if (handled.get()) return@downloadToFile
            if (activeRequests[record.timestamp] !== requestRef.get()) {
                return@downloadToFile
            }
                if (error == null && !done && !pauseFlag.get() && responseCode.get() in 200..299) {
                    val now = System.currentTimeMillis()
                    val saved = base + current
                    if (now - lastNotify > 500) {
                        lastNotify = now
                        val updated = record.copy(totalBytes = absoluteTotal.get(), savedBytes = saved, paused = false)
                        DownloadNotifier.notifyProgress(appContext, updated)
                        dispatchProgress(updated)
                    }
                    if (now - lastCsvWrite > 3000) {
                        lastCsvWrite = now
                        manager.update(record.copy(totalBytes = absoluteTotal.get(), savedBytes = saved, paused = false))
                    }
                    return@downloadToFile
                }

                if (!handled.compareAndSet(false, true)) return@downloadToFile

                val isCurrent = activeRequests[record.timestamp] === requestRef.get()
                if (error != null || responseCode.get() !in 200..299 || pauseFlag.get()) {
                    val savedBytes = mergePartIntoFinal()
                    if (!forceEnhancedTls && !pauseFlag.get() && error is javax.net.ssl.SSLHandshakeException) {
                        cleanup(isCurrent)
                        android.util.Log.w(TAG, "TLS handshake failed, retrying with bundled root CAs")
                        launchDownload(record.copy(savedBytes = savedBytes), forceEnhancedTls = true)
                        return@downloadToFile
                    }
                    cleanup(isCurrent)
                    val failed = !pauseFlag.get()
                    val reason = error?.message?.takeIf { it.isNotBlank() } ?: "网络错误"
                    val stateRecord = record.copy(
                        totalBytes = absoluteTotal.get(),
                        savedBytes = savedBytes,
                        paused = true,
                        error = if (failed) reason else ""
                    )
                    manager.update(stateRecord)
                    DownloadNotifier.notifyPaused(appContext, stateRecord)
                    dispatchProgress(stateRecord)
                    return@downloadToFile
                }

                Thread.sleep(100)
                val savedBytes = mergePartIntoFinal()
                cleanup(isCurrent)
                val finishedRecord = record.copy(
                    totalBytes = absoluteTotal.get(),
                    savedBytes = savedBytes,
                    paused = false,
                    error = ""
                )
                manager.update(finishedRecord)
                DownloadNotifier.notifyCompleted(appContext, finishedRecord)
                dispatchProgress(finishedRecord)
            }
        activeRequests[record.timestamp] = request
        request.go()
        return true
    }

    fun pause(context: Context, timestamp: Long) {
        init(context)
        val appCtx = context.applicationContext
        pauseFlags[timestamp]?.set(true)
        activeRequests[timestamp]?.cancel()
        val manager = DownloadManager(appCtx)
        val record = manager.get(timestamp)
        if (record != null && !record.paused) {
            val pausedRecord = record.copy(paused = true, error = "")
            manager.update(pausedRecord)
            DownloadNotifier.notifyPaused(appCtx, pausedRecord)
        }
    }

    fun resume(context: Context, timestamp: Long) {
        init(context)
        android.util.Log.i(TAG, "resume timestamp=$timestamp")
        val manager = DownloadManager(context.applicationContext)
        val record = manager.get(timestamp)?.copy(paused = false, error = "")
        if (record == null) {
            android.util.Log.w(TAG, "resume: record not found for $timestamp")
            return
        }
        manager.update(record)
        launchDownload(record)
    }

    private const val TAG = "DownloadController"
}

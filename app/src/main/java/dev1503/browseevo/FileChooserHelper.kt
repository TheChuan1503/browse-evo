package dev1503.browseevo

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import java.io.File
import kotlin.concurrent.thread

/**
 * 连接 GeckoView 的 onFilePrompt 与宿主 Activity 的系统文件选择器。
 * MainActivity 在 onCreate 时通过 [launcher] 注入启动能力。
 */
object FileChooserHelper {

    /** 当前可用于启动选择器的宿主，由 Activity 注册/注销。 */
    @Volatile
    var launcher: ((Intent) -> Unit)? = null

    /** 运行时权限请求能力，由 Activity 注册/注销。 */
    @Volatile
    var permissionRequester: ((Array<String>) -> Unit)? = null

    private class PendingRequest(
        val context: Context,
        val prompt: GeckoSession.PromptDelegate.FilePrompt,
        val result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
    )

    private var pending: PendingRequest? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 弹出系统文件选择器。返回 false 表示当前无法启动（例如无宿主 Activity），
     * 调用方应自行 dismiss。
     */
    fun launch(
        context: Context,
        prompt: GeckoSession.PromptDelegate.FilePrompt,
        result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
    ): Boolean {
        val start = launcher ?: return false
        pending?.let { old ->
            pending = null
            old.result.complete(old.prompt.dismiss())
        }
        pending = PendingRequest(context.applicationContext, prompt, result)
        if (hasStoragePermission(context)) {
            return startPicker()
        }
        val requester = permissionRequester ?: return startPicker()
        return try {
            requester(requiredStoragePermissions())
            true
        } catch (e: Exception) {
            startPicker()
        }
    }

    /** 宿主 Activity 收到权限请求结果后回调；无论授予与否都继续打开选择器。 */
    fun onStoragePermissionResult() {
        startPicker()
    }

    private fun startPicker(): Boolean {
        val request = pending ?: return false
        val start = launcher ?: return false
        return try {
            start(buildIntent(request.prompt))
            true
        } catch (e: ActivityNotFoundException) {
            pending = null
            false
        } catch (e: Exception) {
            pending = null
            false
        }
    }

    private fun requiredStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun hasStoragePermission(context: Context): Boolean {
        return requiredStoragePermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** 由宿主 Activity 在收到选择结果后回调；uris 为空表示用户取消。 */
    fun onResult(uris: List<Uri>?) {
        val request = pending ?: return
        pending = null
        if (uris.isNullOrEmpty()) {
            request.result.complete(request.prompt.dismiss())
            return
        }
        thread {
            val resolved = uris.mapNotNull { resolveToReadableUri(request.context, it) }
            mainHandler.post {
                if (resolved.isEmpty()) {
                    request.result.complete(request.prompt.dismiss())
                    return@post
                }
                val selected = if (request.prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE) {
                    resolved.toTypedArray()
                } else {
                    arrayOf(resolved.first())
                }
                try {
                    request.result.complete(request.prompt.confirm(request.context, selected))
                } catch (e: Exception) {
                    request.result.complete(request.prompt.dismiss())
                }
            }
        }
    }

    private fun resolveToReadableUri(context: Context, uri: Uri): Uri? {
        if (uri.scheme == "file") return uri
        if (uri.scheme != "content") return null
        queryRealPath(context, uri)?.let { return Uri.fromFile(it) }
        return copyToCache(context, uri)?.let { Uri.fromFile(it) }
    }

    private fun queryRealPath(context: Context, uri: Uri): File? {
        return try {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (index >= 0 && cursor.moveToFirst()) {
                        val path = cursor.getString(index)
                        if (!path.isNullOrEmpty()) {
                            val file = File(path)
                            if (file.exists() && file.canRead()) return file
                        }
                    }
                }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun copyToCache(context: Context, uri: Uri): File? {
        return try {
            cleanOldUploads(context)
            val dir = File(context.cacheDir, "uploads").apply { mkdirs() }
            val name = queryDisplayName(context, uri)
                ?.replace('/', '_')?.replace('\\', '_')
                ?.takeIf { it.isNotBlank() }
                ?: "upload_${System.currentTimeMillis()}"
            val target = uniqueFile(dir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            } ?: return null
            target
        } catch (e: Exception) {
            null
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                }
        } catch (e: Exception) {
            null
        }
    }

    private fun uniqueFile(dir: File, name: String): File {
        var file = File(dir, name)
        if (!file.exists()) return file
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var index = 1
        while (file.exists()) {
            file = File(dir, "$base($index)$ext")
            index++
        }
        return file
    }

    private fun cleanOldUploads(context: Context) {
        val dir = File(context.cacheDir, "uploads")
        val files = dir.listFiles() ?: return
        val expire = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        for (file in files) {
            if (file.lastModified() < expire) file.delete()
        }
    }

    private fun buildIntent(prompt: GeckoSession.PromptDelegate.FilePrompt): Intent {
        if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.FOLDER) {
            return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        }
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        val mimes = normalizeMimeTypes(prompt.mimeTypes)
        val primary = mimes.firstOrNull { it != "*/*" }
        if (primary != null) {
            intent.type = primary
            val extras = mimes.filter { it != primary && it != "*/*" }.toTypedArray()
            if (extras.isNotEmpty()) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, extras)
            }
        }
        if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        return intent
    }

    /** accept 属性可能传扩展名（如 ".pdf"）或通配符，统一转成可用的 MIME 类型。 */
    private fun normalizeMimeTypes(mimeTypes: Array<String>?): List<String> {
        if (mimeTypes == null) return emptyList()
        return mimeTypes.asSequence()
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .map { mime ->
                if (mime.contains('/')) {
                    mime
                } else {
                    MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(mime.removePrefix(".").lowercase()) ?: "*/*"
                }
            }
            .distinct()
            .toList()
    }
}

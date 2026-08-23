package dev1503.browseevo

import android.content.Context
import android.content.Intent
import android.view.View
import com.google.android.material.button.MaterialButton
import dev1503.browseevo.data.NeoSettings

object Utils {
    var neoSettings: NeoSettings? = null

    fun openHistoryActivity(context: Context, tab: String) {
        context.startActivity(
            Intent(context, HistoryActivity::class.java).putExtra(HistoryActivity.EXTRA_SELECTED_TAB, tab)
        )
    }

    fun openDownloadManagerActivity(context: Context) {
        context.startActivity(Intent(context, DownloadManagerActivity::class.java))
    }

    fun openSettingsActivity(context: Context) {
        context.startActivity(Intent(context, SettingsActivity::class.java))
    }

    fun shareUrl(context: Context, url: String, title: String? = null) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            if (!title.isNullOrEmpty()) {
                putExtra(Intent.EXTRA_SUBJECT, title)
            }
        }
        context.startActivity(Intent.createChooser(intent, "分享网页"))
    }

    fun bindMenuPageButtons(
        page1View: View,
        onAddBookmarkClick: () -> Unit,
        onShareClick: (() -> Unit)? = null,
    ) {
        page1View.findViewById<MaterialButton>(R.id.btnBookmarks)?.setOnClickListener {
            openHistoryActivity(page1View.context, HistoryActivity.TAB_BOOKMARK)
        }
        page1View.findViewById<MaterialButton>(R.id.btnHistory)?.setOnClickListener {
            openHistoryActivity(page1View.context, HistoryActivity.TAB_HISTORY)
        }
        page1View.findViewById<MaterialButton>(R.id.btnDownloads)?.setOnClickListener {
            openDownloadManagerActivity(page1View.context)
        }
        page1View.findViewById<MaterialButton>(R.id.btnAddBookmark)?.setOnClickListener {
            onAddBookmarkClick()
        }
        page1View.findViewById<MaterialButton>(R.id.btnShare)?.setOnClickListener {
            onShareClick?.invoke()
        }
        page1View.findViewById<MaterialButton>(R.id.btnSettings)?.setOnClickListener {
            openSettingsActivity(page1View.context)
        }
    }

    fun isBuiltInPage(url: String, filesDirPath: String): Boolean {
        if (url.startsWith("evo://index", ignoreCase = true)) return true
        if (url.startsWith("evo://page", ignoreCase = true)) return true
        if (url.startsWith("resource://android/assets/built-in_page")) return true
        if (url.startsWith("file:///android_asset/built-in_page")) return true
        if (url.startsWith("file://$filesDirPath/built-in_page")) return true
        return false
    }

    fun looksLikeHost(input: String): Boolean {
        if (input.contains("://") || input.any { it.isWhitespace() }) return false
        return Regex("""^(\d{1,3}\.){3}\d{1,3}(:\d+)?(/.*)?$""").matches(input) ||
            Regex("""^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)+(:\d+)?(/.*)?$""").matches(input) ||
            Regex("""^localhost(:\d+)?(/.*)?$""", RegexOption.IGNORE_CASE).matches(input)
    }

    fun schemeOf(input: String): String? {
        val match = Regex("""^([a-zA-Z][a-zA-Z0-9+.-]*):""").find(input) ?: return null
        val candidate = match.groupValues[1]
        if (candidate.contains('.') || candidate.equals("localhost", ignoreCase = true)) return null
        return candidate.lowercase()
    }
}

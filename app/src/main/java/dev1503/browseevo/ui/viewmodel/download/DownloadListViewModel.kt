package dev1503.browseevo.ui.viewmodel.download

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import dev1503.browseevo.R
import dev1503.browseevo.download.DownloadController
import dev1503.browseevo.download.DownloadManager
import dev1503.browseevo.download.DownloadNotifier
import dev1503.browseevo.download.DownloadRecord
import dev1503.browseevo.ui.viewmodel.ViewModel
import dev1503.browseevo.ui.widgets.EvoPopupMenu
import java.io.File
import kotlin.concurrent.thread

class DownloadListViewModel(override val activity: AppCompatActivity) : ViewModel(activity) {
    companion object {
        const val PAGE_SIZE = 25
        private const val PREFETCH_THRESHOLD = 5
        private const val MAX_ICON_SIZE = 128
        private const val PAYLOAD_PROGRESS = "progress"
        private val IMAGE_EXTENSIONS = setOf("webp", "png", "jpg", "jpeg")
    }

    private val downloadManager = DownloadManager(activity)
    private val rows = mutableListOf<DownloadRecord>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val iconCache = HashMap<String, Drawable>()
    private var offset = 0
    private var allLoaded = false
    private var loading = false
    private var generation = 0

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: DownloadAdapter

    private val progressListener = object : DownloadController.OnProgressListener {
        override fun onProgress(record: DownloadRecord) {
            mainHandler.post { applyProgress(record) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        _view = View.inflate(activity, R.layout.view_model_download_list, null)
        recycler = _view.findViewById(R.id.recyclerDownloads)
        recycler.layoutManager = LinearLayoutManager(activity)
        adapter = DownloadAdapter()
        recycler.adapter = adapter
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || allLoaded || loading) return
                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (lm.findLastVisibleItemPosition() >= rows.size - PREFETCH_THRESHOLD) {
                    loadMore()
                }
            }
        })
        DownloadController.addProgressListener(progressListener)
        loadMore()
        handleOpenRequest()
    }

    private fun handleOpenRequest() {
        val timestamp = activity.intent.getLongExtra(
            dev1503.browseevo.DownloadManagerActivity.EXTRA_OPEN_TIMESTAMP, -1L
        )
        if (timestamp <= 0L) return
        activity.intent.removeExtra(dev1503.browseevo.DownloadManagerActivity.EXTRA_OPEN_TIMESTAMP)
        thread {
            val record = downloadManager.get(timestamp) ?: return@thread
            mainHandler.post {
                if (!DownloadController.openFile(activity, record)) {
                    Snackbar.make(recycler, "文件不存在", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        DownloadController.removeProgressListener(progressListener)
        super.onDestroy()
    }

    private fun applyProgress(record: DownloadRecord) {
        val index = rows.indexOfFirst { it.timestamp == record.timestamp }
        if (index < 0) return
        rows[index] = record
        adapter.notifyItemChanged(index, PAYLOAD_PROGRESS)
    }

    fun refresh() {
        generation++
        offset = 0
        allLoaded = false
        rows.clear()
        adapter.notifyDataSetChanged()
        loadMore()
    }

    private fun loadMore() {
        if (loading || allLoaded) return
        loading = true
        val gen = generation
        thread {
            val page = downloadManager.readPage(offset, PAGE_SIZE)
            mainHandler.post {
                if (gen != generation) {
                    loading = false
                    loadMore()
                    return@post
                }
                loading = false
                val fresh = page.filterNot { p -> rows.any { it.timestamp == p.timestamp } }
                if (fresh.isEmpty()) {
                    if (page.size < PAGE_SIZE) allLoaded = true
                    return@post
                }
                offset += page.size
                rows.addAll(fresh)
                if (page.size < PAGE_SIZE) allLoaded = true
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun isCompleted(record: DownloadRecord): Boolean =
        record.totalBytes > 0 && record.savedBytes >= record.totalBytes

    private fun statusText(record: DownloadRecord): String = when {
        isCompleted(record) -> "下载完成 · ${DownloadNotifier.formatBytes(record.savedBytes)}"
        record.error.isNotBlank() -> "下载失败: ${record.error}"
        record.paused -> "已暂停 · ${DownloadNotifier.formatBytes(record.savedBytes)}" +
            if (record.totalBytes > 0) " / ${DownloadNotifier.formatBytes(record.totalBytes)}" else ""
        else -> if (record.totalBytes > 0) {
            "${DownloadNotifier.formatBytes(record.savedBytes)} / ${DownloadNotifier.formatBytes(record.totalBytes)}"
        } else {
            DownloadNotifier.formatBytes(record.savedBytes)
        }
    }

    private fun extensionOf(filename: String): String =
        filename.substringAfterLast('.', "").lowercase()

    private fun isApkFile(filename: String): Boolean = extensionOf(filename) == "apk"

    private fun isImageFile(filename: String): Boolean = extensionOf(filename) in IMAGE_EXTENSIONS

    private fun fallbackIconRes(record: DownloadRecord): Int = when {
        isApkFile(record.filename) -> R.drawable.apk_document_24px
        isImageFile(record.filename) -> R.drawable.image_24px
        isCompleted(record) -> R.drawable.download_done_24px
        else -> R.drawable.download_24px
    }

    private fun canParseIcon(record: DownloadRecord): Boolean {
        if (!isCompleted(record)) return false
        val file = File(record.path)
        return file.exists() && file.isFile && file.length() > 0
    }

    private fun resolveIconAsync(holder: DownloadEntryViewHolder, record: DownloadRecord) {
        val cached = iconCache[record.path]
        if (cached != null) {
            holder.icon.setImageDrawable(cached)
            return
        }
        holder.icon.setImageResource(fallbackIconRes(record))
        if (!(isApkFile(record.filename) || isImageFile(record.filename))) return
        if (!canParseIcon(record)) return
        val path = record.path
        val timestamp = record.timestamp
        thread {
            val drawable = try {
                when {
                    isApkFile(record.filename) -> loadApkIcon(path)
                    else -> decodeImageIcon(path)
                }
            } catch (e: Exception) {
                null
            }
            if (drawable != null) {
                synchronized(iconCache) { iconCache[path] = drawable }
            }
            mainHandler.post {
                if (holder.itemView.tag == timestamp && holder.itemView.isAttachedToWindow) {
                    if (drawable != null) {
                        holder.icon.setImageDrawable(drawable)
                    } else {
                        holder.icon.setImageResource(fallbackIconRes(record))
                    }
                }
            }
        }
    }

    private fun loadApkIcon(path: String): Drawable? {
        val pm = activity.packageManager
        val info = pm.getPackageArchiveInfo(path, 0) ?: return null
        val appInfo = info.applicationInfo ?: return null
        appInfo.sourceDir = path
        appInfo.publicSourceDir = path
        return appInfo.loadIcon(pm)
    }

    private fun decodeImageIcon(path: String): Drawable? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_ICON_SIZE && bounds.outHeight / (sample * 2) >= MAX_ICON_SIZE) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeFile(path, opts) ?: return null
        return BitmapDrawable(activity.resources, bitmap)
    }

    private fun showItemMenu(anchor: View, timestamp: Long) {
        val index = rows.indexOfFirst { it.timestamp == timestamp }
        if (index < 0) return
        val record = rows[index]
        val popup = EvoPopupMenu(anchor)
        if (!isCompleted(record)) {
            popup.menu.add(if (record.paused) activity.getString(R.string.download_action_resume) else activity.getString(R.string.download_action_pause))
        }
        popup.menu.add("复制链接")
        if (isCompleted(record) && File(record.path).exists()) {
            popup.menu.add("用其他应用打开")
        }
        popup.menu.add("删除")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                activity.getString(R.string.download_action_pause) -> {
                    DownloadController.pause(activity, timestamp)
                    updateRow(timestamp)
                }
                activity.getString(R.string.download_action_resume) -> {
                    DownloadController.resume(activity, timestamp)
                    updateRow(timestamp)
                }
                "复制链接" -> {
                    val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText("url", record.url)
                    )
                    Toast.makeText(activity, "已复制链接", Toast.LENGTH_SHORT).show()
                }
                "用其他应用打开" -> {
                    if (!DownloadController.openFileWithApps(activity, record)) {
                        Snackbar.make(recycler, "文件不存在", Snackbar.LENGTH_SHORT).show()
                    }
                }
                "删除" -> confirmDelete(record, index)
            }
            true
        }
        popup.show()
    }

    private fun updateRow(timestamp: Long) {
        thread {
            val fresh = downloadManager.get(timestamp) ?: return@thread
            mainHandler.post {
                val index = rows.indexOfFirst { it.timestamp == timestamp }
                if (index >= 0) {
                    rows[index] = fresh
                    adapter.notifyItemChanged(index)
                }
            }
        }
    }

    private fun confirmDelete(record: DownloadRecord, index: Int) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("删除下载记录")
            .setMessage("确定删除“${record.filename}”吗？")
            .setNeutralButton("取消", null)
            .setNegativeButton("和文件一起") { _, _ ->
                DownloadController.cancel(activity, record.timestamp, record.path)
                File(record.path).delete()
                removeRow(record)
            }
            .setPositiveButton("确定") { _, _ ->
                DownloadController.cancel(activity, record.timestamp, record.path)
                removeRow(record)
            }
            .show()
    }

    private fun removeRow(record: DownloadRecord) {
        if (!downloadManager.remove(record.timestamp)) return
        synchronized(iconCache) { iconCache.remove(record.path) }
        val index = rows.indexOfFirst { it.timestamp == record.timestamp }
        if (index < 0) return
        rows.removeAt(index)
        adapter.notifyItemRemoved(index)
        adapter.notifyItemRangeChanged(index, rows.size - index)
    }

    private inner class DownloadAdapter : RecyclerView.Adapter<DownloadEntryViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadEntryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_download_entry, parent, false)
            return DownloadEntryViewHolder(view)
        }

        override fun getItemCount(): Int = rows.size

        override fun onBindViewHolder(holder: DownloadEntryViewHolder, position: Int) {
            holder.bind(rows[position])
        }

        override fun onBindViewHolder(
            holder: DownloadEntryViewHolder,
            position: Int,
            payloads: List<Any>
        ) {
            if (payloads.isEmpty()) {
                holder.bind(rows[position])
            } else {
                holder.bindProgressOnly(rows[position])
            }
        }
    }

    private inner class DownloadEntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgEntryIcon)
        private val title: TextView = view.findViewById(R.id.textEntryTitle)
        private val status: TextView = view.findViewById(R.id.textEntryStatus)
        private val progressBar: LinearProgressIndicator = view.findViewById(R.id.progressEntry)
        private val actionButton: ImageButton = view.findViewById(R.id.btnAction)

        fun bind(record: DownloadRecord) {
            title.text = record.filename
            status.text = statusText(record)
            itemView.tag = record.timestamp
            bindClick()
            itemView.setOnLongClickListener {
                showItemMenu(it, record.timestamp)
                true
            }
            resolveIconAsync(this, record)
            bindProgressViews(record)
            bindActionButton(record)
        }

        fun bindProgressOnly(record: DownloadRecord) {
            status.text = statusText(record)
            bindProgressViews(record)
            bindActionButton(record)
            if (isCompleted(record)) resolveIconAsync(this, record)
            bindClick()
        }

        private fun bindClick() {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                val current = rows.getOrNull(position) ?: return@setOnClickListener
                if (isCompleted(current)) {
                    if (!DownloadController.openFile(activity, current)) {
                        Snackbar.make(recycler, "文件不存在", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

        private fun bindProgressViews(record: DownloadRecord) {
            when {
                isCompleted(record) || record.paused -> progressBar.isVisible = false
                record.totalBytes > 0 -> {
                    progressBar.isVisible = true
                    progressBar.isIndeterminate = false
                    progressBar.max = 100
                    progressBar.setProgressCompat(((record.savedBytes * 100) / record.totalBytes).toInt(), true)
                }
                else -> {
                    progressBar.isVisible = true
                    progressBar.isIndeterminate = true
                }
            }
        }

        private fun bindActionButton(record: DownloadRecord) {
            if (isCompleted(record)) {
                actionButton.isVisible = false
                actionButton.setOnClickListener(null)
                return
            }
            actionButton.isVisible = true
            val active = DownloadController.isActive(record.timestamp)
            if (active && !record.paused) {
                actionButton.setImageResource(R.drawable.pause_24px)
                actionButton.contentDescription = activity.getString(R.string.download_action_pause)
                actionButton.setOnClickListener {
                    DownloadController.pause(activity, record.timestamp)
                    updateRow(record.timestamp)
                }
            } else {
                actionButton.setImageResource(R.drawable.play_arrow_24px)
                actionButton.contentDescription = activity.getString(R.string.download_action_resume)
                actionButton.setOnClickListener {
                    DownloadController.resume(activity, record.timestamp)
                    updateRow(record.timestamp)
                }
            }
        }
    }
}

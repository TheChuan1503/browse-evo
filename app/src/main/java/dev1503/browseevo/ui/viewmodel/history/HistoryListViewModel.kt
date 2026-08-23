package dev1503.browseevo.ui.viewmodel.history

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev1503.browseevo.PendingNavigation
import dev1503.browseevo.R
import dev1503.browseevo.data.HistoryManager
import dev1503.browseevo.ui.viewmodel.ViewModel
import dev1503.browseevo.ui.widgets.EvoPopupMenu
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class HistoryListViewModel(override val activity: AppCompatActivity) : ViewModel(activity) {
    companion object {
        const val PAGE_SIZE = 25
        private const val PREFETCH_THRESHOLD = 5
    }

    private sealed class Row {
        class DateLine(val text: String) : Row()
        class Entry(val url: String, val title: String) : Row()
    }

    private val historyManager = HistoryManager(activity)
    private val rows = mutableListOf<Row>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private var offset = 0
    private var lastDayKey: String? = null
    private var allLoaded = false
    private var loading = false
    private val seenUrls = mutableSetOf<String>()

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        _view = View.inflate(activity, R.layout.view_model_history_list, null)
        recycler = _view.findViewById(R.id.recyclerHistory)
        recycler.layoutManager = LinearLayoutManager(activity)
        adapter = HistoryAdapter()
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
        loadMore()
    }

    private fun loadMore() {
        loading = true
        thread {
            val page = historyManager.readPage(offset, PAGE_SIZE)
            mainHandler.post {
                offset += page.size
                if (page.size < PAGE_SIZE) allLoaded = true
                for ((url, title, timestamp) in page) {
                    if (!seenUrls.add(url)) continue
                    val dayKey = dayFormat.format(Date(timestamp))
                    if (dayKey != lastDayKey) {
                        rows.add(Row.DateLine(dayKey))
                        lastDayKey = dayKey
                    }
                    rows.add(Row.Entry(url, title))
                }
                adapter.notifyDataSetChanged()
                loading = false
                if (rows.isEmpty() && !allLoaded) loadMore()
            }
        }
    }

    private fun showDeleteMenu(anchor: View, url: String, title: String) {
        val popup = EvoPopupMenu(anchor)
        popup.menu.add("删除")
        popup.setOnMenuItemClickListener { _ ->
            MaterialAlertDialogBuilder(activity)
                .setTitle("删除历史记录")
                .setMessage("确定删除“${title.ifEmpty { url }}”吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除") { _, _ ->
                    if (historyManager.remove(url)) {
                        val index = rows.indexOfFirst { it is Row.Entry && it.url == url }
                        if (index >= 0) {
                            rows.removeAt(index)
                            adapter.notifyItemRemoved(index)
                            removeOrphanDividers()
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
                .show()
            true
        }
        popup.show()
    }

    private fun removeOrphanDividers() {
        var i = 0
        while (i < rows.size) {
            if (rows[i] is Row.DateLine && (i + 1 >= rows.size || rows[i + 1] !is Row.Entry)) {
                rows.removeAt(i)
            } else {
                i++
            }
        }
    }

    private fun faviconFile(url: String): File? {
        val host = try {
            Uri.parse(url).host ?: return null
        } catch (e: Exception) {
            return null
        }
        return File(File(activity.cacheDir, "favicons"), "$host.png")
    }

    private inner class HistoryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int): Int =
            if (rows[position] is Row.DateLine) 0 else 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == 0) {
                DateViewHolder(inflater.inflate(R.layout.item_history_divider, parent, false))
            } else {
                EntryViewHolder(inflater.inflate(R.layout.item_history_entry, parent, false))
            }
        }

        override fun getItemCount(): Int = rows.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val row = rows[position]) {
                is Row.DateLine -> (holder as DateViewHolder).text.text = row.text
                is Row.Entry -> (holder as EntryViewHolder).bind(row.url, row.title)
            }
        }
    }

    private class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.textDateDivider)
    }

    private inner class EntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgEntryIcon)
        val title: TextView = view.findViewById(R.id.textEntryTitle)
        val urlText: TextView = view.findViewById(R.id.textEntryUrl)

        fun bind(url: String, title: String) {
            this.title.text = title.ifEmpty { url }
            urlText.text = url
            icon.setImageResource(R.drawable.globe_24px)
            itemView.tag = url
            itemView.setOnClickListener {
                PendingNavigation.url = url
                activity.finish()
            }
            itemView.setOnLongClickListener {
                showDeleteMenu(it, url, title)
                true
            }
            val file = faviconFile(url)
            thread {
                val bitmap = if (file?.exists() == true) {
                    try {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } catch (e: Exception) {
                        null
                    }
                } else null
                if (bitmap != null) {
                    mainHandler.post {
                        if (itemView.tag == url && itemView.isAttachedToWindow) {
                            this@EntryViewHolder.icon.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        }
    }
}

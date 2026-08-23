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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dev1503.browseevo.ui.viewmodel.ViewModel
import dev1503.browseevo.ui.widgets.EvoPopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev1503.browseevo.PendingNavigation
import dev1503.browseevo.R
import dev1503.browseevo.data.BookmarkManager
import java.io.File
import kotlin.concurrent.thread

class BookmarkListViewModel(override val activity: AppCompatActivity) : ViewModel(activity) {
    private sealed class Row {
        class DirLine(val text: String) : Row()
        class Entry(val url: String, val title: String) : Row()
    }

    private val bookmarkManager = BookmarkManager(activity)
    private val rows = mutableListOf<Row>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: BookmarkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        _view = View.inflate(activity, R.layout.view_model_bookmark_list, null)
        recycler = _view.findViewById(R.id.recyclerBookmarks)
        recycler.layoutManager = LinearLayoutManager(activity)
        adapter = BookmarkAdapter()
        recycler.adapter = adapter
        loadBookmarks()
    }

    fun loadBookmarks() {
        thread {
            val bookmarks = bookmarkManager.loadAll()
            mainHandler.post {
                rows.clear()
                var lastDir: String? = null
                for (bookmark in bookmarks) {
                    if (bookmark.dir != lastDir) {
                        rows.add(Row.DirLine(bookmark.dir))
                        lastDir = bookmark.dir
                    }
                    rows.add(Row.Entry(bookmark.address, bookmark.title))
                }
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun showEntryMenu(anchor: View, url: String, title: String) {
        val popup = EvoPopupMenu(anchor)
        popup.menu.add("编辑")
        popup.menu.add("删除")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "编辑" -> showEditDialog(url, title)
                "删除" -> confirmDelete(url, title)
            }
            true
        }
        popup.show()
    }

    private fun confirmDelete(url: String, title: String) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("删除书签")
            .setMessage("确定删除“${title.ifEmpty { url }}”吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                if (bookmarkManager.remove(url)) {
                    loadBookmarks()
                }
            }
            .show()
    }

    private fun showEditDialog(url: String, oldTitle: String) {
        val contentView = LayoutInflater.from(activity)
            .inflate(R.layout.view_add_bookmark, null)
        val editTitle = contentView.findViewById<TextInputEditText>(R.id.editBookmarkTitle)
        val editAddress = contentView.findViewById<TextInputEditText>(R.id.editBookmarkAddress)
        val editDir = contentView.findViewById<TextInputEditText>(R.id.editBookmarkDir)
        val original = bookmarkManager.loadAll().firstOrNull { it.address == url } ?: return
        editTitle.setText(original.title)
        editAddress.setText(original.address)
        editDir.setText(original.dir)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("编辑书签")
            .setView(contentView)
            .setNegativeButton("取消", null)
            .setPositiveButton("确定", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newTitle = editTitle.text?.toString()?.trim().orEmpty()
                val newAddress = editAddress.text?.toString()?.trim().orEmpty()
                val newDir = BookmarkManager.sanitizeDir(editDir.text?.toString()?.trim().orEmpty())
                val others = bookmarkManager.loadAll().filterNot { it.address == url }
                if (others.any { it.dir == newDir && it.title == newTitle }) {
                    Toast.makeText(activity, "标题已存在", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (others.any { it.address == newAddress }) {
                    Toast.makeText(activity, "地址已存在", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (bookmarkManager.update(url, BookmarkManager.Bookmark(newTitle, newAddress, newDir))) {
                    Toast.makeText(activity, "已保存", Toast.LENGTH_SHORT).show()
                    loadBookmarks()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun faviconFile(url: String): File? {
        val host = try {
            Uri.parse(url).host ?: return null
        } catch (e: Exception) {
            return null
        }
        return File(File(activity.cacheDir, "favicons"), "$host.png")
    }

    private inner class BookmarkAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int): Int =
            if (rows[position] is Row.DirLine) 0 else 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == 0) {
                DirViewHolder(inflater.inflate(R.layout.item_history_divider, parent, false))
            } else {
                EntryViewHolder(inflater.inflate(R.layout.item_history_entry, parent, false))
            }
        }

        override fun getItemCount(): Int = rows.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val row = rows[position]) {
                is Row.DirLine -> (holder as DirViewHolder).text.text = row.text
                is Row.Entry -> (holder as EntryViewHolder).bind(row.url, row.title)
            }
        }
    }

    private class DirViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
                showEntryMenu(it, url, title)
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

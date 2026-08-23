package dev1503.browseevo.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class BookmarkManager(private val context: Context) {

    data class Bookmark(val title: String, val address: String, val dir: String)

    private val bookmarkFile: File get() = File(context.filesDir, "bookmarks.json")

    fun loadAll(): List<Bookmark> {
        if (!bookmarkFile.exists()) return emptyList()
        return try {
            val array = JSONArray(bookmarkFile.readText())
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                Bookmark(
                    title = obj.optString("title"),
                    address = obj.optString("address"),
                    dir = obj.optString("dir", "/").ifEmpty { "/" }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(bookmark: Bookmark): Boolean {
        return try {
            val list = loadAll().toMutableList()
            list.add(bookmark)
            writeAll(list)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun remove(address: String): Boolean {
        return try {
            val list = loadAll()
            val filtered = list.filterNot { it.address == address }
            if (filtered.size == list.size) return false
            writeAll(filtered)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun update(address: String, bookmark: Bookmark): Boolean {
        return try {
            val list = loadAll().toMutableList()
            val index = list.indexOfFirst { it.address == address }
            if (index < 0) return false
            list[index] = bookmark
            writeAll(list)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun writeAll(list: List<Bookmark>) {
        val array = JSONArray()
        for (b in list) {
            array.put(JSONObject().apply {
                put("title", b.title)
                put("address", b.address)
                put("dir", b.dir)
            })
        }
        bookmarkFile.parentFile?.mkdirs()
        bookmarkFile.writeText(array.toString(2))
    }

    companion object {
        fun sanitizeDir(input: String): String {
            val cleaned = input.filter {
                it.isLetterOrDigit() || it == '/' || it == '_' || it == '-' || it == '.' || it == ' '
            }.replace(Regex("/+"), "/").trim()
            if (cleaned.isEmpty() || cleaned == "/") return "/"
            var path = cleaned
            if (!path.startsWith("/")) path = "/$path"
            while (path.length > 1 && path.endsWith("/")) path = path.dropLast(1)
            return path
        }
    }
}

package dev1503.browseevo.data

import android.content.Context
import java.io.File
import java.util.concurrent.Executors

class HistoryManager(private val context: Context) {

    private val historyFile = File(context.filesDir, "history.csv")
    private val executor = Executors.newSingleThreadExecutor()

    fun record(url: String, title: String) {
        executor.execute {
            try {
                val rows = readAll()
                val filtered = rows.filterNot { it.first == url }
                writeAll(listOf(Triple(url, title, System.currentTimeMillis())) + filtered)
            } catch (e: Exception) {
            }
        }
    }

    fun readPage(offset: Int, limit: Int): List<Triple<String, String, Long>> =
        readAll().drop(offset).take(limit)

    fun remove(url: String): Boolean {
        return try {
            val rows = readAll()
            val filtered = rows.filterNot { it.first == url }
            if (filtered.size == rows.size) return false
            writeAll(filtered)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun readAll(): MutableList<Triple<String, String, Long>> {
        if (!historyFile.exists()) return mutableListOf()
        return try {
            historyFile.readLines().mapNotNull { line ->
                val fields = CsvUtil.parseLine(line) ?: return@mapNotNull null
                if (fields.size < 3) return@mapNotNull null
                Triple(fields[0], fields[1], fields[2].toLongOrNull() ?: 0L)
            }.toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun writeAll(rows: List<Triple<String, String, Long>>) {
        try {
            historyFile.parentFile?.mkdirs()
            historyFile.writeText(rows.joinToString("\n") { (url, title, ts) ->
                "${CsvUtil.escape(url)},${CsvUtil.escape(title)},$ts"
            })
        } catch (e: Exception) {
        }
    }
}

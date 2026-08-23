package dev1503.browseevo.download

import android.content.Context
import dev1503.browseevo.data.CsvUtil
import java.io.File

class DownloadManager(context: Context) {

    private val store = DownloadStore.get(context.applicationContext)

    fun loadAll(): List<DownloadRecord> = store.loadAll()

    fun readPage(offset: Int, limit: Int): List<DownloadRecord> = store.readPage(offset, limit)

    fun get(timestamp: Long): DownloadRecord? = store.get(timestamp)

    fun add(record: DownloadRecord): Boolean = store.add(record)

    fun update(record: DownloadRecord): Boolean = store.update(record)

    fun remove(timestamp: Long): Boolean = store.remove(timestamp)
}

internal object DownloadStore {

    private const val MAX_LINE_LENGTH = 512 * 1024
    private const val MAX_FIELD_LENGTH = 128 * 1024

    private val lock = Any()
    private var file: File? = null

    fun get(appContext: Context): DownloadStore {
        synchronized(lock) {
            if (file == null) {
                file = File(appContext.filesDir, "downloads.csv")
            }
        }
        return this
    }

    private fun downloadFile(): File = file ?: throw IllegalStateException("DownloadStore not initialized")

    fun loadAll(): List<DownloadRecord> = synchronized(lock) { readInternal() }

    fun readPage(offset: Int, limit: Int): List<DownloadRecord> = synchronized(lock) {
        readInternal().drop(offset).take(limit)
    }

    fun get(timestamp: Long): DownloadRecord? = synchronized(lock) {
        readInternal().firstOrNull { it.timestamp == timestamp }
    }

    fun add(record: DownloadRecord): Boolean = synchronized(lock) {
        try {
            writeInternal(listOf(record) + readInternal())
            true
        } catch (e: Exception) {
            false
        }
    }

    fun update(record: DownloadRecord): Boolean = synchronized(lock) {
        try {
            val list = readInternal().toMutableList()
            val index = list.indexOfFirst { it.timestamp == record.timestamp }
            if (index < 0) return false
            list[index] = record
            writeInternal(list)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun remove(timestamp: Long): Boolean = synchronized(lock) {
        try {
            writeInternal(readInternal().filterNot { it.timestamp == timestamp })
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun readInternal(): List<DownloadRecord> {
        val f = downloadFile()
        if (!f.exists()) return emptyList()
        return try {
            val result = mutableListOf<DownloadRecord>()
            f.bufferedReader().useLines { lines ->
                for (line in lines) {
                    if (line.length > MAX_LINE_LENGTH) continue
                    val fields = CsvUtil.parseLine(line, MAX_FIELD_LENGTH) ?: continue
                    if (fields.size < 7) continue
                    result.add(
                        DownloadRecord(
                            filename = fields[0],
                            url = fields[1],
                            path = fields[2],
                            totalBytes = fields[3].toLongOrNull() ?: 0L,
                            savedBytes = fields[4].toLongOrNull() ?: 0L,
                            paused = fields[5] == "true",
                            timestamp = fields[6].toLongOrNull() ?: 0L,
                            error = if (fields.size > 7) fields[7] else ""
                        )
                    )
                }
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeInternal(records: List<DownloadRecord>) {
        val f = downloadFile()
        f.parentFile?.mkdirs()
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(records.joinToString("\n") { r ->
            "${CsvUtil.escape(r.filename, MAX_FIELD_LENGTH)},${CsvUtil.escape(r.url, MAX_FIELD_LENGTH)},${CsvUtil.escape(r.path, MAX_FIELD_LENGTH)},${r.totalBytes},${r.savedBytes},${r.paused},${r.timestamp},${CsvUtil.escape(r.error, MAX_FIELD_LENGTH)}"
        })
        if (!tmp.renameTo(f)) {
            f.delete()
            if (!tmp.renameTo(f)) {
                tmp.copyTo(f, overwrite = true)
                tmp.delete()
            }
        }
    }
}

package dev1503.browseevo.data

import android.app.Application
import org.json.JSONObject
import java.io.File

class NeoSettings(application: Application?, filePath: String) {

    private val file: File = File(filePath)
    private val map = LinkedHashMap<String, Any>()

    init {
        synchronized(map) {
            try {
                file.parentFile?.mkdirs()
                if (file.exists()) {
                    val obj = JSONObject(file.readText())
                    for (key in obj.keys()) {
                        map[key] = obj.get(key)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun getString(key: String, defValue: String?): String? =
        synchronized(map) { map[key] as? String ?: defValue }

    fun getBoolean(key: String, defValue: Boolean): Boolean =
        synchronized(map) { map[key] as? Boolean ?: defValue }

    fun getInt(key: String, defValue: Int): Int =
        synchronized(map) { (map[key] as? Number)?.toInt() ?: defValue }

    fun getFloat(key: String, defValue: Float): Float =
        synchronized(map) { (map[key] as? Number)?.toFloat() ?: defValue }

    fun putString(key: String, value: String?) = put(key, value ?: "")

    fun putBoolean(key: String, value: Boolean) = put(key, value)

    fun putInt(key: String, value: Int) = put(key, value)

    fun putFloat(key: String, value: Float) = put(key, value.toDouble())

    private fun put(key: String, value: Any) {
        synchronized(map) {
            map[key] = value
            persist()
        }
    }

    private fun persist() {
        try {
            val obj = JSONObject()
            for ((key, value) in map) {
                obj.put(key, value)
            }
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(obj.toString())
            if (!tmp.renameTo(file)) {
                file.delete()
                if (!tmp.renameTo(file)) {
                    tmp.copyTo(file, overwrite = true)
                    tmp.delete()
                }
            }
        } catch (_: Exception) {
        }
    }
}

package dev1503.browseevo.data

import android.app.Application
import android.util.Log
import org.json.JSONObject
import java.io.File

class NeoSettings(application: Application?, filePath: String) {
    private val TAG = "NeoSettings"

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

    fun getString(key: String, defValue: String?): String? {
        val v = synchronized(map) { map[key] as? String ?: defValue }
        Log.d(TAG, "GET[STRING] $key: $v")
        return v
    }

    fun getBoolean(key: String, defValue: Boolean): Boolean {
        val v = synchronized(map) { map[key] as? Boolean ?: defValue }
        Log.d(TAG, "GET[BOOLEAN] $key: $v")
        return v
    }

    fun getInt(key: String, defValue: Int): Int {
        val v = synchronized(map) { (map[key] as? Number)?.toInt() ?: defValue }
        Log.d(TAG, "GET[INT] $key: $v")
        return v
    }

    fun getFloat(key: String, defValue: Float): Float {
        val v = synchronized(map) { (map[key] as? Number)?.toFloat() ?: defValue }
        Log.d(TAG, "GET[FLOAT] $key: $v")
        return v
    }

    fun putString(key: String, value: String?) {
        val v = value ?: ""
        Log.d(TAG, "PUT[STRING] $key = $v")
        put(key, v)
    }

    fun putBoolean(key: String, value: Boolean) {
        Log.d(TAG, "PUT[BOOLEAN] $key = $value")
        put(key, value)
    }

    fun putInt(key: String, value: Int) {
        Log.d(TAG, "PUT[INT] $key = $value")
        put(key, value)
    }

    fun putFloat(key: String, value: Float) {
        Log.d(TAG, "PUT[FLOAT] $key = $value")
        put(key, value.toDouble())
    }

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

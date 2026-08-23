package dev1503.browseevo.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.concurrent.Executors

class DownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timestamp = intent.getLongExtra(DownloadNotifier.EXTRA_TIMESTAMP, -1L)
        android.util.Log.i(TAG, "onReceive action=${intent.action} timestamp=$timestamp")
        if (timestamp < 0) return
        val action = intent.action ?: return
        val result = goAsync()
        EXECUTOR.execute {
            try {
                when (action) {
                    DownloadNotifier.ACTION_PAUSE -> DownloadController.pause(context, timestamp)
                    DownloadNotifier.ACTION_RESUME -> DownloadController.resume(context, timestamp)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "download action failed", e)
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        private const val TAG = "DownloadAction"
        private val EXECUTOR = Executors.newSingleThreadExecutor()
    }
}

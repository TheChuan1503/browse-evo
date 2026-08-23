package dev1503.browseevo.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder

class DownloadService : Service() {

    private var foregroundTimestamp = -1L
    private var stopping = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopWithDetach()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val record = intent.getRecordExtra()
                if (record == null) {
                    stopWithDetach()
                    return START_NOT_STICKY
                }
                DownloadNotifier.ensureChannel(this)
                if (foregroundTimestamp < 0) {
                    foregroundTimestamp = record.timestamp
                    startForeground(
                        DownloadNotifier.notificationId(record.timestamp),
                        DownloadNotifier.buildProgress(this, record)
                    )
                }
                DownloadController.launchDownload(record)
                return START_NOT_STICKY
            }
            else -> {
                stopWithDetach()
                return START_NOT_STICKY
            }
        }
    }

    private fun stopWithDetach() {
        if (stopping) return
        stopping = true
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
        stopSelf()
    }

    private fun Intent.getRecordExtra(): DownloadRecord? {
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                getSerializableExtra(EXTRA_RECORD, DownloadRecord::class.java)
            } else {
                @Suppress("DEPRECATION")
                getSerializableExtra(EXTRA_RECORD) as? DownloadRecord
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val ACTION_START = "dev1503.browseevo.download.START"
        const val ACTION_STOP = "dev1503.browseevo.download.STOP"
        const val EXTRA_RECORD = "record"

        fun start(context: Context, record: DownloadRecord) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RECORD, record)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, DownloadService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                try {
                    context.stopService(Intent(context, DownloadService::class.java))
                } catch (ignored: Exception) {
                }
            }
        }
    }
}

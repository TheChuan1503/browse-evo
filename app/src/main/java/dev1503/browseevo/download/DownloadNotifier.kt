package dev1503.browseevo.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dev1503.browseevo.DownloadManagerActivity

object DownloadNotifier {
    const val CHANNEL_ID = "downloads"
    const val ACTION_PAUSE = "dev1503.browseevo.download.PAUSE"
    const val ACTION_RESUME = "dev1503.browseevo.download.RESUME"
    const val EXTRA_TIMESTAMP = "timestamp"

    fun notificationId(timestamp: Long): Int = (timestamp % Int.MAX_VALUE).toInt()

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "下载", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    fun notifyProgress(context: Context, record: DownloadRecord) {
        ensureChannel(context)
        notify(context, notificationId(record.timestamp), buildProgress(context, record))
    }

    fun buildProgress(context: Context, record: DownloadRecord): android.app.Notification {
        val builder = baseBuilder(context, record)
            .setContentText("${formatBytes(record.savedBytes)} / ${if (record.totalBytes > 0) formatBytes(record.totalBytes) else "未知"}")
            .setOngoing(true)
        if (record.totalBytes > 0) {
            builder.setProgress(100, ((record.savedBytes * 100) / record.totalBytes).toInt(), false)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    fun buildSummary(
        context: Context,
        record: DownloadRecord? = null,
        activeCount: Int = 0
    ): android.app.Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)
        if (record == null) {
            builder.setContentTitle("正在下载")
                .setContentText("下载任务进行中")
        } else {
            builder.setContentTitle(record.filename)
            if (activeCount > 1) {
                builder.setContentText("正在下载 ${formatBytes(record.savedBytes)} / ${if (record.totalBytes > 0) formatBytes(record.totalBytes) else "未知"} · 共 ${activeCount} 个任务")
            } else {
                builder.setContentText("正在下载 ${formatBytes(record.savedBytes)} / ${if (record.totalBytes > 0) formatBytes(record.totalBytes) else "未知"}")
            }
            if (record.totalBytes > 0) {
                builder.setProgress(100, ((record.savedBytes * 100) / record.totalBytes).toInt(), false)
            } else {
                builder.setProgress(0, 0, true)
            }
        }
        return builder.build()
    }

    fun cancelNotification(context: Context, timestamp: Long) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId(timestamp))
    }

    fun notifyPaused(context: Context, record: DownloadRecord) {
        ensureChannel(context)
        val builder = baseBuilder(context, record)
            .setContentText("已暂停 ${formatBytes(record.savedBytes)}")
            .setOngoing(false)
        notify(context, notificationId(record.timestamp), builder.build())
    }

    fun notifyCompleted(context: Context, record: DownloadRecord) {
        ensureChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(record.filename)
            .setContentText("下载完成")
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .clearActions()
        builder.setContentIntent(openCompletedIntent(context, record))
        notify(context, notificationId(record.timestamp), builder.build())
    }

    private fun openCompletedIntent(context: Context, record: DownloadRecord): PendingIntent {
        val intent = Intent(context, DownloadManagerActivity::class.java).apply {
            putExtra(DownloadManagerActivity.EXTRA_OPEN_TIMESTAMP, record.timestamp)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context,
            notificationId(record.timestamp),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun baseBuilder(context: Context, record: DownloadRecord): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(record.filename)
            .setOnlyAlertOnce(true)
            .setSilent(true)
        if (Build.VERSION.SDK_INT < 26) {
            builder.setPriority(android.app.Notification.PRIORITY_LOW)
        }
        if (!record.paused) {
            builder.addAction(0, "暂停", actionPendingIntent(context, ACTION_PAUSE, record.timestamp))
        } else {
            builder.addAction(0, "继续", actionPendingIntent(context, ACTION_RESUME, record.timestamp))
        }
        return builder
    }

    private fun actionPendingIntent(context: Context, action: String, timestamp: Long): PendingIntent {
        val intent = Intent(context, DownloadActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_TIMESTAMP, timestamp)
        }
        val requestCode = notificationId(timestamp) * 2 + if (action == ACTION_PAUSE) 1 else 0
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.notify(id, notification)
        } catch (e: Exception) {
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "未知"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.lastIndex) {
            value /= 1024.0
            unitIndex++
        }
        val formatted = if (unitIndex == 0) "%.0f" else "%.2f"
        return String.format(java.util.Locale.US, "$formatted %s", value, units[unitIndex])
    }
}

package dev1503.browseevo.download

data class DownloadRecord(
    val filename: String,
    val url: String,
    val path: String,
    val totalBytes: Long,
    val savedBytes: Long,
    val paused: Boolean,
    val timestamp: Long,
    val error: String = "",
) : java.io.Serializable

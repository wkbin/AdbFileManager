package data.remote.adb

fun interface AdbTransferProgress {
    fun onProgress(percent: Int)
}

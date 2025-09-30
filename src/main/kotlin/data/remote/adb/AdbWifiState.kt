package data.remote.adb

data class AdbWifiState(
    val connected: Boolean,
    val ipAddress: String?,
    val port: String?,
){
    companion object{
        val WIFI_UNAVAILABLE = AdbWifiState(false, null, null)
    }
}
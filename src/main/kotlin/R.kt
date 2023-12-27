import java.util.ArrayList

object R : BaseResource(resRoot = "res") {

    val raw = Raw

    val icons = Icons


    object Raw : ChildResource(resRoot, "raw") {
        val adbWindows by path("windows", "adb.zip")
        val adbLinux by path("linux", "adb.zip")
        val adbMacOs by path("macOs", "adb.zip")
    }

    object Icons : ChildResource(resRoot, "icons") {
        val icAdbDisconnect by path("ic_adb_disconnect.png")

        val iconApk by path("icon_apk.png")
        val iconBack by path("icon_back.png")
        val iconFile by path("icon_file.png")
        val iconFiles by path("icon_files.png")
        val iconImage by path("icon_image.png")
        val iconJson by path("icon_json.png")
        val iconRefresh by path("icon_refresh.png")
        val iconTxt by path("icon_txt.png")
        val iconXml by path("icon_xml.png")
        val folder by path("folder.png")
    }
}


abstract class BaseResource(internal val resRoot: String)
abstract class ChildResource(resRoot: String, private val childRoot: String) : BaseResource(resRoot) {
    fun path(vararg names: String) = lazy {
        val pathArray = ArrayList<String>()
        pathArray.add(resRoot)
        pathArray.add(childRoot)
        val pathIterator = names.iterator()
        while (pathIterator.hasNext()) {
            val path = pathIterator.next()
            pathArray.add(path)
        }
        pathArray.joinToString("/")
    }
}


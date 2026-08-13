package model

import java.util.prefs.Preferences

object FileManagerLayoutPreferences {
    private const val LOCAL_PANE_COLLAPSED = "localPaneCollapsed"
    private val preferences by lazy {
        Preferences.userNodeForPackage(FileManagerLayoutPreferences::class.java)
    }

    fun isLocalPaneCollapsed(): Boolean =
        runCatching { preferences.getBoolean(LOCAL_PANE_COLLAPSED, false) }.getOrDefault(false)

    fun setLocalPaneCollapsed(collapsed: Boolean) {
        runCatching { preferences.putBoolean(LOCAL_PANE_COLLAPSED, collapsed) }
    }
}

package data.remote.adb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShellOutputFilterTest {

    @Test
    fun `normal command output passes through`() {
        var detectedDir: String? = null
        val filter = ShellOutputFilter { detectedDir = it }

        assertEquals("audio  dev  mnt  proc", filter.filter("audio  dev  mnt  proc"))
        assertNull(detectedDir)
    }

    @Test
    fun `pwd marker output updates directory and is filtered out`() {
        var detectedDir: String? = null
        val filter = ShellOutputFilter { detectedDir = it }

        val result = filter.filter("__AFM_PWD__=/system/bin")
        assertNull(result)
        assertEquals("/system/bin", detectedDir)
    }

    @Test
    fun `pwd command echo from shell is filtered out`() {
        var detectedDir: String? = null
        val filter = ShellOutputFilter { detectedDir = it }

        assertNull(filter.filter("printf '__AFM_PWD__=%s\\n' \"\$PWD\""))
        assertNull(filter.filter("[root@r2335_debug:/]# printf '\\n__AFM_PWD__=%s\\n' \"\$PWD\""))
        assertNull(filter.filter("n' \"\$PWD\""))
        assertNull(filter.filter("stty -echo 2>/dev/null; export PS1=''"))
    }

    @Test
    fun `device prompt line is filtered out`() {
        var detectedDir: String? = null
        val filter = ShellOutputFilter { detectedDir = it }

        assertNull(filter.filter("[root@r2335_debug:/]# "))
        assertNull(filter.filter("root@r2335_debug:/ #"))
        assertNull(filter.filter("/ $ "))
        assertNull(filter.filter("$ "))
        assertNull(filter.filter("# "))
    }
}

package data.remote.adb

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AdbFileOperationsTest {
    private val device = AdbDevice("test-device", AdbWifiState.WIFI_UNAVAILABLE)

    @Test
    fun `windows push stages problematic names and restores exact remote name`() = runBlocking {
        val root = createTempDirectory("afm-push-test").toFile()
        try {
            val names = listOf(
                "HEATS.docx",
                "trust you.docx",
                "あんなに一緒だったのに.docx",
                "副 本.docx"
            )

            names.forEachIndexed { index, name ->
                val source = File(root, name).apply { writeText("content-$index") }
                val adb = RecordingAdb()
                val stage = File(root, "afm_push_$index.tmp")
                val operations = AdbFileOperations(
                    adb = adb,
                    currentDeviceProvider = { device },
                    windowsPushWorkaroundEnabled = true,
                    stagingFileFactory = { stage }
                )

                operations.push(source.absolutePath, "storage/emulated/0/backups/$name")

                val pushed = adb.pushes.single()
                assertEquals("content-$index", pushed.content)
                assertTrue(pushed.localPath.substringAfterLast(File.separator).all { it.code < 128 })
                assertTrue(pushed.remotePath.matches(Regex("/data/local/tmp/adb-file-manager-[a-f0-9-]+")))
                assertFalse(stage.exists(), "Local staging file must be deleted")

                val move = adb.shellCalls.single { it.args.firstOrNull() == "mv" }
                assertEquals("'${pushed.remotePath}'", move.args[3])
                assertEquals("'/storage/emulated/0/backups/$name'", move.args[4])
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `progress push uses the same windows staging path`() = runBlocking {
        val root = createTempDirectory("afm-progress-test").toFile()
        try {
            val source = File(root, "副 本.docx").apply { writeText("payload") }
            val stage = File(root, "afm_push_progress.tmp")
            val adb = RecordingAdb()
            val progress = mutableListOf<Int>()
            val operations = AdbFileOperations(
                adb = adb,
                currentDeviceProvider = { device },
                windowsPushWorkaroundEnabled = true,
                stagingFileFactory = { stage }
            )

            val output = mutableListOf<String>()
            operations.pushWithProgress(
                source.absolutePath,
                "storage/emulated/0/backups/${source.name}"
            ) { progress += it }.collect { output += it }

            assertEquals(listOf(100), progress)
            assertEquals(listOf("uploaded"), output)
            assertEquals("payload", adb.pushes.single().content)
            assertFalse(stage.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `installed packages are queried for current user and ignore non package output`() = runBlocking {
        val adb = RecordingAdb().apply {
            shellResponse = { args ->
                when (args.firstOrNull()) {
                    "am" -> listOf("10")
                    "pm" -> listOf(
                        "package:com.example.first",
                        "Error: unrelated diagnostic",
                        "package:com.example.second"
                    )
                    else -> emptyList()
                }
            }
        }
        val operations = AdbFileOperations(
            adb = adb,
            currentDeviceProvider = { device },
            windowsPushWorkaroundEnabled = false
        )

        assertEquals(
            listOf("com.example.first", "com.example.second"),
            operations.getInstalledPackages()
        )
        val packageQuery = adb.shellCalls.single { it.args.firstOrNull() == "pm" }
        assertEquals(listOf("pm", "list", "packages", "-3", "--user", "10"), packageQuery.args)
        assertTrue(packageQuery.throwOnError)
    }

    @Test
    fun `remote file operations shell quote spaces quotes and dollar signs`() = runBlocking {
        val adb = RecordingAdb()
        val operations = AdbFileOperations(
            adb = adb,
            currentDeviceProvider = { device },
            windowsPushWorkaroundEnabled = false
        )

        operations.delete("data/local/tmp/a b'c\$d.txt")
        operations.renameFile("data/local/tmp", "old ' \$ name", "new ' \$ name")
        operations.copyFile("storage/emulated/0/源 文件.txt", "/data/local/tmp/目标 目录")
        operations.moveFile("storage/emulated/0/移动 文件.txt", "/data/local/tmp/目标 目录")

        assertEquals(
            listOf("rm", "-rf", "--", "'/data/local/tmp/a b'\\''c\$d.txt'"),
            adb.shellCalls[0].args
        )
        assertEquals(
            listOf(
                "mv", "--",
                "'/data/local/tmp/old '\\'' \$ name'",
                "'/data/local/tmp/new '\\'' \$ name'"
            ),
            adb.shellCalls[1].args
        )
        assertEquals(
            listOf(
                "cp", "-r", "--",
                "'/storage/emulated/0/源 文件.txt'",
                "'/data/local/tmp/目标 目录'"
            ),
            adb.shellCalls[2].args
        )
        assertEquals(
            listOf(
                "mv", "--",
                "'/storage/emulated/0/移动 文件.txt'",
                "'/data/local/tmp/目标 目录'"
            ),
            adb.shellCalls[3].args
        )
    }

    @Test
    fun `searchFiles constructs find command with maxdepth and stat format`() = runBlocking {
        val adb = RecordingAdb()
        val operations = AdbFileOperations(
            adb = adb,
            currentDeviceProvider = { device },
            windowsPushWorkaroundEnabled = false
        )

        operations.searchFiles("sdcard/Download", "photo", maxDepth = 3)

        val searchCall = adb.shellCalls.single()
        assertEquals("find", searchCall.args[0])
        assertEquals("'/sdcard/Download'", searchCall.args[1])
        assertEquals("-maxdepth", searchCall.args[2])
        assertEquals("3", searchCall.args[3])
        assertEquals("-iname", searchCall.args[4])
        assertEquals("'*photo*'", searchCall.args[5])
    }

    @Test
    fun `launchApp invokes monkey with package and launcher category`() = runBlocking {
        val adb = RecordingAdb()
        val operations = AdbFileOperations(
            adb = adb,
            currentDeviceProvider = { device },
            windowsPushWorkaroundEnabled = false
        )

        operations.launchApp("com.example.app")

        val call = adb.shellCalls.single()
        assertEquals(listOf("monkey", "-p", "'com.example.app'", "-c", "android.intent.category.LAUNCHER", "1"), call.args)
    }

    @Test
    fun `forceStopApp invokes am force-stop`() = runBlocking {
        val adb = RecordingAdb()
        val operations = AdbFileOperations(
            adb = adb,
            currentDeviceProvider = { device },
            windowsPushWorkaroundEnabled = false
        )

        operations.forceStopApp("com.example.app")

        val call = adb.shellCalls.single()
        assertEquals(listOf("am", "force-stop", "'com.example.app'"), call.args)
        assertTrue(call.throwOnError)
    }

    @Test
    fun `clearAppData invokes pm clear`() = runBlocking {
        val adb = RecordingAdb()
        val operations = AdbFileOperations(
            adb = adb,
            currentDeviceProvider = { device },
            windowsPushWorkaroundEnabled = false
        )

        operations.clearAppData("com.example.app")

        val call = adb.shellCalls.single()
        assertEquals(listOf("pm", "clear", "'com.example.app'"), call.args)
        assertTrue(call.throwOnError)
    }

    @Test
    fun `rebootDevice invokes reboot with specified mode`() = runBlocking {
        val adb = RecordingAdb()
        val operations = AdbFileOperations(
            adb = adb,
            currentDeviceProvider = { device },
            windowsPushWorkaroundEnabled = false
        )

        operations.rebootDevice(model.RebootMode.RECOVERY)

        val call = adb.shellCalls.single()
        assertEquals(listOf("reboot", "recovery"), call.args)
    }

    @Test
    fun `clipboard text is transferred as utf8 file and confirmed by helper`() = runBlocking {
        val adb = RecordingAdb().apply {
            shellResponse = { args ->
                when (args.firstOrNull()) {
                    "am" -> listOf("10")
                    else -> if (args.any { it == "top.wkbin.clipboard.ClipboardHelper" }) {
                        listOf("CLIPBOARD_SET_OK")
                    } else emptyList()
                }
            }
        }
        val operations = AdbFileOperations(
            adb = adb,
            currentDeviceProvider = { device },
            windowsPushWorkaroundEnabled = false,
            clipboardHelperLoader = { "fake-dex".encodeToByteArray() }
        )
        val expected = "中文 with spaces, quotes ' and\nnew line"

        operations.pushClipboard(expected)

        assertEquals(2, adb.pushes.size)
        assertEquals("fake-dex", adb.pushes[0].content)
        assertEquals(expected, adb.pushes[1].content)
        val helperCall = adb.shellCalls.single { call ->
            call.args.any { it == "top.wkbin.clipboard.ClipboardHelper" }
        }
        assertTrue(helperCall.args[0].startsWith("CLASSPATH=/data/local/tmp/"))
        assertEquals("app_process", helperCall.args[1])
        assertEquals("10", helperCall.args.last())
        assertTrue(helperCall.throwOnError)
    }

    @Test
    fun `clipboard operation does not report success without helper acknowledgement`() = runBlocking {
        val adb = RecordingAdb().apply {
            shellResponse = { args -> if (args.firstOrNull() == "am") listOf("0") else emptyList() }
        }
        val operations = AdbFileOperations(
            adb = adb,
            currentDeviceProvider = { device },
            windowsPushWorkaroundEnabled = false,
            clipboardHelperLoader = { "fake-dex".encodeToByteArray() }
        )

        assertFailsWith<AdbException> {
            operations.pushClipboard("must be acknowledged")
        }
        Unit
    }

    private data class PushCall(val localPath: String, val remotePath: String, val content: String)
    private data class ShellCall(val args: List<String>, val throwOnError: Boolean)

    private class RecordingAdb : Adb {
        val pushes = mutableListOf<PushCall>()
        val shellCalls = mutableListOf<ShellCall>()
        var shellResponse: (List<String>) -> List<String> = { emptyList() }

        override fun getAdbPath(): String = "adb"
        override suspend fun devices(): List<String> = emptyList()
        override suspend fun wifiState(deviceId: String): AdbWifiState = AdbWifiState.WIFI_UNAVAILABLE
        override suspend fun connect(ipAddress: String, port: String): AdbWifiState = AdbWifiState.WIFI_UNAVAILABLE
        override suspend fun pair(ipAddress: String, port: String, code: String): List<String> = emptyList()
        override suspend fun listAvds(): List<AndroidVirtualDevice> = emptyList()

        override suspend fun shell(
            adbDevice: AdbDevice,
            vararg args: String,
            throwOnError: Boolean
        ): List<String> {
            shellCalls += ShellCall(args.toList(), throwOnError)
            return shellResponse(args.toList())
        }

        override suspend fun push(
            adbDevice: AdbDevice,
            localPath: String,
            remotePath: String,
            throwOnError: Boolean
        ): List<String> {
            recordPush(localPath, remotePath)
            return emptyList()
        }

        override suspend fun pull(
            adbDevice: AdbDevice,
            remotePath: String,
            localPath: String,
            throwOnError: Boolean
        ): List<String> = emptyList()

        override fun pushWithProgress(
            adbDevice: AdbDevice,
            localPath: String,
            remotePath: String,
            progressCallback: AdbTransferProgress
        ): Flow<String> = flow {
            recordPush(localPath, remotePath)
            progressCallback.onProgress(100)
            emit("uploaded")
        }

        override fun pullWithProgress(
            adbDevice: AdbDevice,
            remotePath: String,
            localPath: String,
            progressCallback: AdbTransferProgress
        ): Flow<String> = flowOf()

        private fun recordPush(localPath: String, remotePath: String) {
            pushes += PushCall(localPath, remotePath, File(localPath).readText())
        }
    }
}

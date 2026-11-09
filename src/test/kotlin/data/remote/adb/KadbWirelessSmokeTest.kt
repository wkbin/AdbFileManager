package data.remote.adb

import com.flyfishxu.kadb.cert.KadbCert
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import model.AppSettings
import org.junit.Assume.assumeTrue
import java.io.File
import java.util.UUID
import java.util.prefs.Preferences
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Explicit hardware check. Uses an already authorized host key, never prints its contents. */
class KadbWirelessSmokeTest {
    @Test
    fun `wireless shell and file round trip`() = runBlocking {
        val target = System.getenv("AFM_KADB_SMOKE_TARGET")
        val keyPath = System.getenv("AFM_KADB_SMOKE_KEY")
        val pairingPort = System.getenv("AFM_KADB_PAIR_PORT")
        val pairingCode = System.getenv("AFM_KADB_PAIR_CODE")
        val useStoredIdentity = System.getenv("AFM_KADB_USE_STORED_IDENTITY") == "true"
        assumeTrue("Set AFM_KADB_SMOKE_TARGET and AFM_KADB_SMOKE_KEY for hardware verification", target != null && (keyPath != null || pairingPort != null || useStoredIdentity))
        val host = target!!.substringBeforeLast(':')
        val port = target.substringAfterLast(':')
        val root = createTempDirectory("afm-kadb-smoke").toFile()
        val preferences = Preferences.userRoot().node("afm-kadb-test/${UUID.randomUUID()}")
        val originalSetting = AppSettings.useKadbWireless
        val remote = "/data/local/tmp/afm-kadb-${UUID.randomUUID()}"
        val identity = System.getenv("AFM_KADB_PAIR_IDENTITY")?.let(::File) ?: File(root, "identity")
        val backend = KadbAdb(AdbImpl("adb", Terminal()), identity, preferences)
        var device: AdbDevice? = null
        try {
            AppSettings.useKadbWireless = true
            if (pairingPort != null && pairingCode != null) {
                backend.pair(host, pairingPort, pairingCode)
            } else if (!useStoredIdentity) {
                KadbCert.importPrivateKey(File(keyPath!!).readBytes())
            }
            backend.connect(host, port)
            if (System.getenv("AFM_KADB_TEST_DISCOVERY") == "true") {
                withTimeout(30000) {
                    while (backend.devices().none { it.contains("._adb-tls-connect._tcp") }) delay(500)
                }
                assertEquals(port, backend.connect(host, "").port)
            }
            val connected = AdbDevice(target, AdbWifiState(true, host, port))
            device = connected
            assertEquals("kadb-smoke", backend.shell(connected, "echo kadb-smoke", throwOnError = true).first())
            backend.shell(connected, "mkdir -p $remote", throwOnError = true)
            val source = File(root, "source.bin").apply { writeBytes(ByteArray(262144) { (it % 251).toByte() }) }
            val progress = mutableListOf<Int>()
            backend.pushWithProgress(connected, source.path, "$remote/test.bin", AdbTransferProgress { progress += it }).collect()
            assertEquals(100, progress.last())
            val destination = File(root, "result.bin")
            backend.pull(connected, "$remote/test.bin", destination.path, true)
            assertContentEquals(source.readBytes(), destination.readBytes())
            listOf("concurrent-a", "concurrent-b").map { name ->
                async {
                    val concurrentFile = File(root, name)
                    backend.pull(connected, "$remote/test.bin", concurrentFile.path, true)
                    assertContentEquals(source.readBytes(), concurrentFile.readBytes())
                }
            }.awaitAll()
            val large = File(root, "cancel.bin").apply { writeBytes(ByteArray(8 * 1024 * 1024)) }
            backend.shell(connected, "echo original > $remote/cancel.bin", throwOnError = true)
            val started = CompletableDeferred<Unit>()
            val transfer = launch {
                backend.pushWithProgress(connected, large.path, "$remote/cancel.bin", AdbTransferProgress {
                    if (it > 0) started.complete(Unit)
                }).collect()
            }
            withTimeout(15000) { started.await() }
            transfer.cancel()
            withTimeout(5000) { transfer.join() }
            assertEquals("original", backend.shell(connected, "cat $remote/cancel.bin", throwOnError = true).first())
            assertTrue(backend.shell(connected, "find $remote -name '.afm-kadb-*'", throwOnError = true).isEmpty())
            assertEquals("alive", backend.shell(connected, "echo alive", throwOnError = true).first())
            val directory = File(root, "tree").apply { mkdirs() }
            File(directory, "中文 name.txt").writeText("unicode round trip")
            backend.push(connected, directory.path, "$remote/tree", true)
            val downloaded = File(root, "downloaded")
            backend.pull(connected, "$remote/tree", downloaded.path, true)
            assertEquals("unicode round trip", File(downloaded, "中文 name.txt").readText())
            AdbShellSession("unused-adb", connected) { backend.openClient(connected) }.use { terminal ->
                terminal.send("adb shell")
                withTimeout(15000) { terminal.state.first { it.mode == TerminalMode.SHELL } }
                terminal.send("echo KADB_TERMINAL_OK")
                withTimeout(15000) { terminal.state.first { it.output.contains("KADB_TERMINAL_OK") } }
                terminal.send("cd /data/local/tmp")
                withTimeout(15000) { terminal.state.first { it.currentDirectory == "/data/local/tmp" } }
            }
            val screenshot = File(root, "screen.png")
            assertTrue(backend.screenshot(connected, screenshot))
            assertContentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10), screenshot.readBytes().take(8).toByteArray())
        } finally {
            try {
                device?.let { backend.shell(it, "rm -rf $remote", throwOnError = false) }
            } finally {
                AppSettings.useKadbWireless = originalSetting
                backend.close()
                preferences.removeNode()
                root.deleteRecursively()
            }
        }
    }
}

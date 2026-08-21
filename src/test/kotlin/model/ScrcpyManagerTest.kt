package model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.io.path.createTempDirectory

class ScrcpyManagerTest {
    private val releaseJson = """
        {
          "tag_name": "v4.1",
          "ignored": "value",
          "assets": [
            {"name":"scrcpy-linux-x86_64-v4.1.tar.gz","browser_download_url":"https://example/linux","size":11},
            {"name":"scrcpy-macos-aarch64-v4.1.tar.gz","browser_download_url":"https://example/mac-arm","size":12},
            {"name":"scrcpy-macos-x86_64-v4.1.tar.gz","browser_download_url":"https://example/mac-intel","size":13},
            {"name":"scrcpy-win32-v4.1.zip","browser_download_url":"https://example/win32","size":14},
            {"name":"scrcpy-win64-v4.1.zip","browser_download_url":"https://example/win64","size":15}
          ]
        }
    """.trimIndent()

    @Test
    fun `release json is parsed and unknown fields are ignored`() {
        val release = ScrcpyManager.parseRelease(releaseJson)

        assertEquals("v4.1", release.tag_name)
        assertEquals(5, release.assets.size)
        assertEquals(15L, release.assets.last().size)
    }

    @Test
    fun `selects win64 instead of win32`() {
        val release = ScrcpyManager.parseRelease(releaseJson)

        val asset = ScrcpyManager.selectAsset(
            release.assets,
            ScrcpyManager.Platform.WINDOWS,
            "amd64"
        )

        assertEquals("scrcpy-win64-v4.1.zip", asset?.name)
    }

    @Test
    fun `selects matching mac architecture`() {
        val release = ScrcpyManager.parseRelease(releaseJson)

        val arm = ScrcpyManager.selectAsset(
            release.assets,
            ScrcpyManager.Platform.MACOS,
            "arm64"
        )
        val intel = ScrcpyManager.selectAsset(
            release.assets,
            ScrcpyManager.Platform.MACOS,
            "x86_64"
        )

        assertEquals("scrcpy-macos-aarch64-v4.1.tar.gz", arm?.name)
        assertEquals("scrcpy-macos-x86_64-v4.1.tar.gz", intel?.name)
    }

    @Test
    fun `selects linux package and rejects unsupported architecture`() {
        val release = ScrcpyManager.parseRelease(releaseJson)

        val x64 = ScrcpyManager.selectAsset(
            release.assets,
            ScrcpyManager.Platform.LINUX,
            "amd64"
        )
        val arm = ScrcpyManager.selectAsset(
            release.assets,
            ScrcpyManager.Platform.LINUX,
            "aarch64"
        )

        assertEquals("scrcpy-linux-x86_64-v4.1.tar.gz", x64?.name)
        assertNull(arm)
    }

    @Test
    fun `cache path strips release tag prefix`() {
        val directory = ScrcpyManager.versionDirectory("v4.1", "C:/Users/tester")
            .invariantSeparatorsPath

        assertTrue(directory.endsWith("C:/Users/tester/.adbfilemanager/scrcpy/4.1"))
    }

    @Test
    fun `cached version detection returns newest complete installation`() {
        val root = createTempDirectory("scrcpy-cache-test").toFile()
        try {
            val oldDirectory = root.resolve("3.9/bin").also { it.mkdirs() }
            oldDirectory.resolve("scrcpy.exe").writeText("")
            root.resolve("4.0").mkdirs() // Incomplete download must be ignored.
            val latestDirectory = root.resolve("4.1/bin").also { it.mkdirs() }
            val expected = latestDirectory.resolve("scrcpy.exe").also { it.writeText("") }

            val actual = ScrcpyManager.findCachedExecutable(
                root,
                ScrcpyManager.Platform.WINDOWS
            )

            assertEquals(expected.canonicalPath, actual?.canonicalPath)
        } finally {
            root.deleteRecursively()
        }
    }
}

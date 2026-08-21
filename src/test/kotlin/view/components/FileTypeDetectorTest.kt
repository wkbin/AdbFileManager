package view.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileTypeDetectorTest {

    @Test
    fun `isImageFile correctly identifies image formats`() {
        assertTrue(isImageFile("photo.png"))
        assertTrue(isImageFile("camera.JPG"))
        assertTrue(isImageFile("avatar.jpeg"))
        assertTrue(isImageFile("banner.webp"))
        assertTrue(isImageFile("icon.ico"))
        assertTrue(isImageFile("art.bmp"))
        assertTrue(isImageFile("animation.gif"))

        assertFalse(isImageFile("document.pdf"))
        assertFalse(isImageFile("archive.zip"))
        assertFalse(isImageFile("script.sh"))
        assertFalse(isImageFile("noextension"))
        assertFalse(isImageFile(""))
    }

    @Test
    fun `isEditableFile correctly identifies text formats`() {
        assertTrue(isEditableFile("config.json"))
        assertTrue(isEditableFile("README.md"))
        assertTrue(isEditableFile("build.gradle"))
        assertTrue(isEditableFile("App.kt"))
        assertTrue(isEditableFile("script.sh"))
        assertTrue(isEditableFile("log.txt"))

        assertFalse(isEditableFile("photo.png"))
        assertFalse(isEditableFile("app.apk"))
        assertFalse(isEditableFile("video.mp4"))
    }
}

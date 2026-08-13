package view.components.dnd

import java.awt.datatransfer.DataFlavor
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalFileTransferableTest {
    @Test
    fun `exposes local file using the desktop file list flavor`() {
        val file = File("C:/临时 文件.txt")
        val transferable = LocalFileTransferable(file)

        assertTrue(transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
        assertContentEquals(arrayOf(DataFlavor.javaFileListFlavor), transferable.transferDataFlavors)
        assertEquals(listOf(file), transferable.getTransferData(DataFlavor.javaFileListFlavor))
    }
}

package view.components.dnd

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File

class PlaceholderTransferable(
    private val remoteFilePath: String,
    private val onRequestExport: suspend (String) -> Unit
) : Transferable {

    private val tempFile: File by lazy {
        val fileName = remoteFilePath.substringAfterLast("/")
        val tempDir = File(System.getProperty("java.io.tmpdir"), "adb_drag_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        val localFile = File(tempDir, fileName)

        // Block until adb pull completes so the external drop target receives the full file.
        // Called from AWT DnD thread, not Compose main thread, so runBlocking is safe here.
        runBlocking {
            withTimeout(EXPORT_TIMEOUT_MS) {
                onRequestExport(localFile.absolutePath)
            }
        }

        localFile
    }

    override fun getTransferDataFlavors(): Array<out DataFlavor?> =
        arrayOf(DataFlavor.javaFileListFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean =
        DataFlavor.javaFileListFlavor.isMimeTypeEqual(flavor)

    override fun getTransferData(flavor: DataFlavor?): Any =
        listOf(tempFile)

    companion object {
        private const val EXPORT_TIMEOUT_MS = 60_000L
    }
}
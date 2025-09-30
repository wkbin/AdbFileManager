package view.components.dnd

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File

class PlaceholderTransferable(
    private val remoteFilePath: String,
    private val onRequestExport: (String) -> Unit
) : Transferable {

    // 外部系统需要的文件列表
    private val tempFile: File by lazy {
        val fileName = remoteFilePath.substringAfterLast("/")
        val tempDir = File(System.getProperty("java.io.tmpdir"), "adb_drag_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        val localFile = File(tempDir, fileName)

        // 调用回调，由 ViewModel 执行 adb pull
        onRequestExport(localFile.absolutePath)

        localFile
    }

    override fun getTransferDataFlavors(): Array<out DataFlavor?> =
        arrayOf(DataFlavor.javaFileListFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean =
        DataFlavor.javaFileListFlavor.isMimeTypeEqual(flavor)

    override fun getTransferData(flavor: DataFlavor?): Any =
        listOf(tempFile)
}
package view.components.dnd

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File

class PlaceholderTransferable(adbFile: String, selectionFile: String): Transferable {


    private val tempFile: File by lazy {
        // 获取原始文件名
        val fileName = selectionFile.substringAfterLast("/")
        // 创建临时目录
        val tempDir = File(System.getProperty("java.io.tmpdir"), "adb_drag_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        // 在临时目录中创建文件，保持原始文件名
        val file = File(tempDir, fileName)
        // 执行 adb pull 命令
        val process = ProcessBuilder(adbFile, "pull", selectionFile, file.path)
            .start()
        process.waitFor()
        // 确保临时目录在程序退出时被删除
        tempDir.deleteOnExit()
        file
    }

    override fun getTransferDataFlavors(): Array<out DataFlavor?>? {
        return arrayOf(DataFlavor.javaFileListFlavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
        return DataFlavor.javaFileListFlavor.isMimeTypeEqual(flavor)
    }

    override fun getTransferData(flavor: DataFlavor?): Any {
        return listOf(tempFile)
    }
}
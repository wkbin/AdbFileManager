package view.components.dnd

import org.jetbrains.skiko.hostOs
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File

class PlaceholderTransferable(adbFile: String, selectionFile: String): Transferable {
    private val placeholderFileUnix by lazy {
        File.createTempFile("下载文件", ".sh").also {
            it.writeText(
                """
                #!/usr/bin/sh
                $adbFile pull "$selectionFile"
                rm -- "$0"
            """.trimIndent())
            it.setExecutable(true)
        }
    }
    private val placeholderFileWindows by lazy {
        File.createTempFile("下载文件", ".bat").also {
            it.writeText("""
                $adbFile pull "$selectionFile"
                del %0
            """.trimIndent())
        }
    }

    private val placeholderFile by lazy {
        if (hostOs.isWindows) {
            placeholderFileWindows
        } else {
            placeholderFileUnix
        }.also(File::deleteOnExit)
    }

    override fun getTransferDataFlavors(): Array<out DataFlavor?>? {
        return arrayOf(DataFlavor.javaFileListFlavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
        return DataFlavor.javaFileListFlavor.isMimeTypeEqual(flavor)
    }

    override fun getTransferData(flavor: DataFlavor?): Any {
        return listOf(placeholderFile)
    }
}
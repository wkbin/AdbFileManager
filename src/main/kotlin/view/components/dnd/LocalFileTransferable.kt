package view.components.dnd

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File

internal class LocalFileTransferable(private val file: File) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> =
        arrayOf(DataFlavor.javaFileListFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean =
        DataFlavor.javaFileListFlavor.isMimeTypeEqual(flavor)

    override fun getTransferData(flavor: DataFlavor?): Any = listOf(file)
}

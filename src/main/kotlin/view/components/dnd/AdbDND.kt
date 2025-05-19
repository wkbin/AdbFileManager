package view.components.dnd

import LocalAdb
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTransferable
import androidx.compose.ui.draganddrop.awtTransferable
import java.awt.datatransfer.DataFlavor
import java.io.File

@Composable
fun Modifier.adbDndSource(selectedFile: String): Modifier {
    val adbFile = LocalAdb.current.adbPath
    return dragAndDropSource {
        DragAndDropTransferData(
            transferable = DragAndDropTransferable(PlaceholderTransferable(adbFile, selectedFile)),
            supportedActions = listOf(DragAndDropTransferAction.Copy),
            onTransferCompleted = {
                println("$it transfer completed")
            }
        )
    }
}

@Composable
fun Modifier.adbDndTarget(onDropAction: (List<File>) -> Unit): Modifier {
    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
            }

            override fun onEnded(event: DragAndDropEvent) {
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val transferable = event.awtTransferable
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    val data = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                    if (!data.isNullOrEmpty()) {
                        onDropAction(data)
                        return true
                    }
                }
                return false
            }
        }
    }
    return dragAndDropTarget(shouldStartDragAndDrop = { true }, target = dropTarget)
}
package view.components.dnd

import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTransferable
import androidx.compose.ui.draganddrop.awtTransferable
import java.awt.datatransfer.DataFlavor
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.adbDndSource(
    selectedFile: String,
    onRequestExport: suspend (String) -> Unit
): Modifier {
    return dragAndDropSource {
        DragAndDropTransferData(
            transferable = DragAndDropTransferable(
                PlaceholderTransferable(
                    selectedFile,
                    onRequestExport
                )
            ),
            supportedActions = listOf(DragAndDropTransferAction.Copy),
            onTransferCompleted = {
                println("$it transfer completed")
            }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.adbDndTarget(onDropFiles: (List<File>) -> Unit): Modifier {
    val currentOnDropFiles = rememberUpdatedState(onDropFiles)
    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {}

            override fun onEnded(event: DragAndDropEvent) {}

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val transferable = event.awtTransferable
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    val data =
                        transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                    if (!data.isNullOrEmpty()) {
                        currentOnDropFiles.value(data)
                        return true
                    }
                }
                return false
            }
        }
    }
    return dragAndDropTarget(shouldStartDragAndDrop = { true }, target = dropTarget)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.localFileDndSource(file: File): Modifier {
    return dragAndDropSource {
        DragAndDropTransferData(
            transferable = DragAndDropTransferable(
                LocalFileTransferable(file)
            ),
            supportedActions = listOf(DragAndDropTransferAction.Copy)
        )
    }
}

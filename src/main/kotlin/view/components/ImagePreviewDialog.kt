package view.components

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.ArrowDownload
import io.github.composefluent.icons.regular.ArrowMaximize
import io.github.composefluent.icons.regular.Dismiss
import io.github.composefluent.icons.regular.ZoomIn
import io.github.composefluent.icons.regular.ZoomOut

import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Text
import io.github.composefluent.component.Icon
import view.fluent.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser

@Composable
fun ImagePreviewDialog(
    visible: Boolean,
    imageFile: File?,
    fileName: String,
    fileSize: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onExport: (destinationPath: String) -> Unit
) {
    if (!visible) return

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val imageBitmap = remember(imageFile) {
        if (imageFile != null && imageFile.exists() && imageFile.length() > 0) {
            try {
                val bufferedImage = ImageIO.read(imageFile)
                bufferedImage?.toComposeImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.88f),
            shape = FluentTheme.shapes.overlay,

            color = FluentTheme.colors.background.solid.tertiary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileName,
                            style = FluentTheme.typography.subtitle,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = fileSize,
                                style = FluentTheme.typography.body,
                                color = FluentTheme.colors.text.text.secondary
                            )
                            if (imageBitmap != null) {
                                Text(
                                    text = "  •  ${imageBitmap.width} × ${imageBitmap.height} px",
                                    style = FluentTheme.typography.body,
                                    color = FluentTheme.colors.text.text.secondary
                                )
                            }
                        }
                    }

                    // Zoom controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { scale = (scale - 0.25f).coerceAtLeast(0.5f) }) {
                            Icon(Icons.Regular.ZoomOut, contentDescription = "缩小")
                        }
                        Text(
                            text = "${(scale * 100).toInt()}%",
                            style = FluentTheme.typography.caption,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(onClick = { scale = (scale + 0.25f).coerceAtMost(4f) }) {
                            Icon(Icons.Regular.ZoomIn, contentDescription = "放大")
                        }
                        IconButton(onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        }) {
                            Icon(Icons.Regular.ArrowMaximize, contentDescription = "重置")
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Regular.Dismiss, contentDescription = "关闭")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Image preview canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(FluentTheme.shapes.control)
                        .background(FluentTheme.colors.background.solid.baseAlt)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator()
                        }
                        imageBitmap != null -> {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = fileName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offset.x
                                        translationY = offset.y
                                    }
                            )
                        }
                        else -> {
                            Text(
                                text = "无法加载图片预览",
                                style = FluentTheme.typography.body,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val chooser = JFileChooser().apply {
                                dialogTitle = "导出图片"
                                selectedFile = File(fileName)
                            }
                            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                                onExport(chooser.selectedFile.absolutePath)
                            }
                        }
                    ) {
                        Icon(Icons.Regular.ArrowDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("导出到电脑")
                    }
                }
            }
        }
    }
}

package view.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.window.FrameWindowScope
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter

/**
 * 为修饰符添加窗口拖拽功能 - 使用AWT事件处理实现平滑拖动
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.moveableWindow(window: java.awt.Window) = composed {
    var isDragging by remember { mutableStateOf(false) }
    then(
        Modifier.pointerHoverIcon(PointerIcon(Cursor(if (isDragging) Cursor.MOVE_CURSOR else Cursor.DEFAULT_CURSOR)))
            .onPointerEvent(PointerEventType.Press) {
                // 记录点击位置
                val awtEvent = it.awtEventOrNull
                isDragging = true
                if (awtEvent != null) {
                    // 计算鼠标点击位置相对于窗口的偏移
                    val offsetX = awtEvent.x
                    val offsetY = awtEvent.y

                    // 添加移动监听器
                    val mouseMoveListener = object : MouseMotionAdapter() {
                        override fun mouseDragged(e: MouseEvent) {
                            // 新的窗口位置 = 鼠标在屏幕上的位置 - 初始点击时的偏移
                            val newX = e.locationOnScreen.x - offsetX
                            val newY = e.locationOnScreen.y - offsetY
                            window.setLocation(newX, newY)
                        }
                    }

                    val cleanup = {
                        window.removeMouseMotionListener(mouseMoveListener)
                        isDragging = false
                        window.cursor = Cursor.getDefaultCursor()
                    }

                    // 添加释放监听器移除移动监听器
                    val mouseReleaseListener = object : MouseAdapter() {
                        override fun mouseReleased(e: MouseEvent) {
                            cleanup()
                            window.removeMouseListener(this)
                        }
                    }

                    // 添加监听器
                    window.addMouseMotionListener(mouseMoveListener)
                    window.addMouseListener(mouseReleaseListener)
                }
            }
    )
}


/**
 * 创建一个可拖动的窗口区域
 * 
 * @param modifier 应用于内容的修饰符
 * @param content 要渲染的内容，接收一个带有拖动功能的修饰符
 */
@Composable
fun FrameWindowScope.MoveableWindowArea(
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    // 添加窗口拖动功能
    val dragModifier = remember(window) {
        modifier.moveableWindow(window)
    }
    
    // 渲染内容
    content(dragModifier)
} 
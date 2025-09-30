package view.components.toast

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AccessibilityManager
import kotlinx.coroutines.flow.StateFlow

interface ToastData {
    val message: String
    val icon: ImageVector?
    val animationDuration: StateFlow<Int?>
    val type: ToastModel.Type
    suspend fun run(accessibilityManager: AccessibilityManager?)
    fun pause()
    fun resume()
    fun dismiss()
    fun dismissed()
}
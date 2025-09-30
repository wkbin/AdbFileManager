package view.effect

sealed interface FileManagerEffect {
    data class ShowErrorTips(val message: String) : FileManagerEffect
    data class ShowSuccessTips(val message: String): FileManagerEffect
}
package view.components.toast

data class ToastModel(
    val message: String,
    val type: Type = Type.Normal
) {
    enum class Type {
        Normal, Success, Error,
    }
}
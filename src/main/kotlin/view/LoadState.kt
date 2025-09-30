package view

sealed class LoadState<out T> {
    object Idle : LoadState<Nothing>()
    object Loading : LoadState<Nothing>()
    object Empty : LoadState<Nothing>()
    data class Success<T>(val data: List<T>) : LoadState<T>()
    data class Error(val error: Throwable) : LoadState<Nothing>()
}
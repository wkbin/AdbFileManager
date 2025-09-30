package model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class FileTransferState {
    object Idle : FileTransferState()
    data class Transferring(
        val fileName: String,
        val progress: Float,
        val bytesTransferred: Long,
        val totalBytes: Long
    ) : FileTransferState()
    object Completed : FileTransferState()
    data class Failed(val error: String) : FileTransferState()
}

class FileTransferProgress {
    private val _state = MutableStateFlow<FileTransferState>(FileTransferState.Idle)
    val state: StateFlow<FileTransferState> = _state.asStateFlow()

    fun startTransfer(fileName: String, totalBytes: Long) {
        _state.value = FileTransferState.Transferring(
            fileName = fileName,
            progress = 0f,
            bytesTransferred = 0L,
            totalBytes = totalBytes
        )
    }

    fun updateProgress(bytesTransferred: Long) {
        val currentState = _state.value
        if (currentState is FileTransferState.Transferring) {
            val progress = if (currentState.totalBytes > 0) {
                bytesTransferred.toFloat() / currentState.totalBytes.toFloat()
            } else {
                0f
            }
            _state.value = currentState.copy(
                progress = progress.coerceIn(0f, 1f),
                bytesTransferred = bytesTransferred
            )
        }
    }

    fun complete() {
        _state.value = FileTransferState.Completed
    }

    fun fail(error: String) {
        _state.value = FileTransferState.Failed(error)
    }

    fun reset() {
        _state.value = FileTransferState.Idle
    }
}

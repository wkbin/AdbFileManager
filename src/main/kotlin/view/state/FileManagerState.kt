package view.state

import model.AppInfo
import model.Bookmark
import model.FileItem
import model.SortType
import view.LoadState
import java.io.File

data class FileManagerState(
    val sortType: SortType = SortType.TYPE_ASC,
    val files: LoadState<FileItem> = LoadState.Idle,
    val fileEncoding: String = "UTF-8",
    val fileContent: String? = null,
    val fileName: String? = null,
    val pendingDeleteFile: String? = null,
    val operationInProgress: Boolean = false,
    val pendingRenameFile: FileItem? = null,
    val showCreateDirectoryDialog: Boolean = false,
    val showCreateFileDialog: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList(),
    val currentPathBookmarked: Boolean = false,
    val showBookmarkMenu: Boolean = false,
    val showAddBookmarkDialog: Boolean = false,
    val pendingChangePermissionsFile: FileItem? = null,
    val showChangePermissionsDialog: Boolean = false,
    val isTransferring: Boolean = false,
    val transferringFileName: String? = null,
    val showTransferDialog: Boolean = false,
    val pendingCopyMoveFile: FileItem? = null,
    val isCopyOperation: Boolean = true, // true = copy, false = move
    val showCopyMoveDialog: Boolean = false,
    val selectionMode: Boolean = false,
    val selectedFiles: Set<String> = emptySet(),
    val pendingBatchDeleteFiles: Set<String> = emptySet(),
    val showAppManager: Boolean = false,
    val appList: List<AppInfo> = emptyList(),
    val appListLoading: Boolean = false,
    val showClipboardDialog: Boolean = false,
    val highlightedFileName: String? = null,

    // Image preview
    val showImagePreviewDialog: Boolean = false,
    val previewImageFile: File? = null,
    val previewImageName: String? = null,
    val previewImageSize: String = "",
    val isPreviewImageLoading: Boolean = false,

    // Search dialog & recursive search
    val showSearchDialog: Boolean = false,
    val recursiveSearchResults: List<FileItem> = emptyList(),
    val isRecursiveSearching: Boolean = false,

    // Device info dashboard
    val showDeviceInfoDialog: Boolean = false,
    val detailedDeviceInfo: model.DeviceDetailedInfo? = null,
    val isDeviceInfoLoading: Boolean = false,

    // Scrcpy screen mirroring
    val showScrcpyDialog: Boolean = false,
    val scrcpyPhase: ScrcpyPhase = ScrcpyPhase.IDLE,
    val scrcpyDownloadProgress: Float = 0f,
    val scrcpyDownloadedBytes: Long = 0L,
    val scrcpyTotalBytes: Long = 0L,
    val scrcpyStatusText: String = "",
    val scrcpyError: String? = null,
    val scrcpyVersion: String = ""
)

/** Scrcpy 下载 / 启动流程阶段 */
enum class ScrcpyPhase {
    IDLE,
    CHECKING,    // 正在检查最新版本
    DOWNLOADING, // 正在下载
    EXTRACTING,  // 正在解压
    ERROR
}

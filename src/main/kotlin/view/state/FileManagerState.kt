package view.state

import model.Bookmark
import model.FileItem
import model.SortType
import view.LoadState

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
    val selectedFiles: Set<String> = emptySet()
)

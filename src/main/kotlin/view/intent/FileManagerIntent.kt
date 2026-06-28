package view.intent

import model.Bookmark
import model.FileItem
import model.SortType
import java.io.File


sealed class FileManagerIntent {
    data class ResetDirectory(val path: String) : FileManagerIntent()
    object NavigateUp : FileManagerIntent()
    data class NavigateToPathIndex(val index: Int) : FileManagerIntent()
    object LoadFiles : FileManagerIntent()
    data class ImportFiles(val files: List<File>) : FileManagerIntent()
    data class DeleteFile(val fileName: String) : FileManagerIntent()
    data class RequestDeleteConfirmation(val fileName: String) : FileManagerIntent()
    object CancelDeleteConfirmation : FileManagerIntent()
    data class NavigateTo(val directoryName: String) : FileManagerIntent()
    data class ResetSortType(val type: SortType) : FileManagerIntent()
    data class ExportFile(val fileName: String, val destinationPath: String) : FileManagerIntent()
    data class LoadFileContent(val fileName: String, val useDetectedEncoding: Boolean = true) :
        FileManagerIntent()

    data class SaveFile(val newContent: String) : FileManagerIntent()
    object CloseEditor : FileManagerIntent()
    data class ChangeEncoding(val encoding: String) : FileManagerIntent()
    
    data class RequestRename(val file: FileItem) : FileManagerIntent()
    object DismissRenameDialog : FileManagerIntent()
    data class RenameFile(val oldName: String, val newName: String) : FileManagerIntent()
    object ShowCreateDirectoryDialog : FileManagerIntent()
    object ShowCreateFileDialog : FileManagerIntent()
    object DismissCreateDialogs : FileManagerIntent()
    data class CreateDirectory(val dirName: String) : FileManagerIntent()
    data class CreateFile(val fileName: String, val content: String) : FileManagerIntent()

    object RequestBookmarkMenu : FileManagerIntent()
    object DismissBookmarkMenu : FileManagerIntent()
    object RequestAddBookmarkDialog : FileManagerIntent()
    object DismissAddBookmarkDialog : FileManagerIntent()
    data class AddBookmark(val name: String) : FileManagerIntent()
    data class RemoveBookmark(val bookmark: Bookmark) : FileManagerIntent()
    data class NavigateToBookmark(val bookmark: Bookmark) : FileManagerIntent()
    
    data class RequestChangePermissions(val file: FileItem) : FileManagerIntent()
    object DismissChangePermissionsDialog : FileManagerIntent()
    data class ChangePermissions(val fileName: String, val permissions: String) : FileManagerIntent()
    
    data class StartTransfer(val fileName: String) : FileManagerIntent()
    object EndTransfer : FileManagerIntent()

    data class InstallApk(val fileName: String) : FileManagerIntent()

    data class RequestCopy(val file: FileItem) : FileManagerIntent()
    data class RequestMove(val file: FileItem) : FileManagerIntent()
    object DismissCopyMoveDialog : FileManagerIntent()
    data class CopyFile(val fileName: String, val destPath: String) : FileManagerIntent()
    data class MoveFile(val fileName: String, val destPath: String) : FileManagerIntent()

    object ToggleSelectionMode : FileManagerIntent()
    data class ToggleFileSelection(val fileName: String) : FileManagerIntent()
    object SelectAllFiles : FileManagerIntent()
    object ClearFileSelection : FileManagerIntent()
    object BatchDeleteFiles : FileManagerIntent()
    data class BatchExportFiles(val destinationPath: String) : FileManagerIntent()
}

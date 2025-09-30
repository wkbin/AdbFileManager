package model

interface Strings {
    val appTitle: String
    fun appVersion(version: String): String
    val appDescription: String
    val appChecking: String
    val appCheckUpdate: String
    val appViewOnGithub: String
    val appClose: String
    val appLater: String
    val appUpdateNow: String
    val appNewVersion: String
    fun appNewVersionDetail(version: String): String
    val appUpdateContent: String
    val appDontPromptAgain: String

    val navRoot: String
    val navBack: String
    val navGoBack: String

    val fileDeleteConfirm: String
    fun fileDeleteMessage(fileName: String): String
    val fileDelete: String
    val fileCancel: String
    val fileSave: String
    val fileRename: String
    val fileImportSuccess: String
    fun fileImportFailed(error: String): String
    fun fileExportFailed(error: String): String
    val fileDeleteSuccess: String
    fun fileDeleteFailed(error: String): String
    val fileSaveSuccess: String
    fun fileSaveFailed(error: String): String
    val fileDownload: String
    val fileEdit: String
    val fileNewFolder: String
    val fileNewFile: String
    val fileFolderName: String
    val fileFolderNameInput: String
    val fileFolderNameErrorInvalid: String
    val fileName: String
    val fileNameInput: String
    val fileContent: String
    val fileContentInput: String
    val fileCreate: String
    val fileNameEmptyError: String
    val fileNameInvalidError: String
    val fileLink: String
    val fileLinkDirectory: String
    val fileDirectory: String
    val fileModified: String
    fun fileCharactersLines(chars: Int, lines: Int): String

    val changePermissions: String
    val permissionChangeSuccess: String
    fun permissionChangeFailed(error: String): String
    fun permissionCurrent(permissions: String): String
    val permissionUseOctal: String
    val permissionOctalLabel: String
    val permissionOwner: String
    val permissionGroup: String
    val permissionOther: String
    val permissionRead: String
    val permissionWrite: String
    val permissionExecute: String

    val addBookmark: String
    val removeBookmark: String
    val bookmark: String
    fun bookmarkPath(currentPath: String): String
    val bookmarkNameLabel: String
    val bookmarkConfirm: String
    val bookmarkCancel: String
    val bookmarkEmpty: String
    val bookmarkAddCurrentPath: String
    val bookmarkAddedSuccess: String
    fun bookmarkAddedFailed(error: String): String
    val bookmarkRemovedSuccess: String
    fun bookmarkRemovedFailed(error: String): String
    val folderCreatedSuccess: String
    fun folderCreatedFailed(error: String): String
    val fileCreatedSuccess: String
    fun fileCreatedFailed(error: String): String
    val renameSuccess: String
    fun renameFailed(error: String): String

    val editorTitle: String
    fun editorTitleWithName(name: String): String
    val editorFileEditor: String
    fun editorEncoding(encoding: String): String
    val editorEncodingLabel: String
    val editorClose: String
    val editorFileContent: String
    val editorModified: String
    val editorCharactersLines: String

    val sortTitle: String
    val sortTypeFolderFirst: String
    val sortTypeFileFirst: String
    val sortNameAZ: String
    val sortDateOldest: String
    val sortDateNewest: String
    val sortSizeSmallest: String
    val sortSizeLargest: String

    val pathEdit: String
    val pathInput: String
    val pathInputPlaceholder: String
    fun pathInvalid(path: String): String
    val pathAlreadyRoot: String
    val pathInvalidIndex: String
    val pathCollapse: String

    val searchTitle: String
    val searchPlaceholder: String

    val errorTitle: String
    val errorEmptyDirectory: String
    val errorNoFiles: String
    val errorLoading: String

    val permissionDenied: String
    val permissionInvalid: String

    fun terminalCommandFailed(exitCode: Int, msg: String): String

    fun adbPermissionSet(path: String): String
    fun adbPermissionError(msg: String): String

    val aboutTitle: String
    val aboutDescription: String
    val aboutViewOnGithub: String
    val aboutCheckUpdate: String
    val aboutChecking: String
    val aboutClose: String

    val themeFollowSystem: String
    val themeLight: String
    val themeDark: String
    val themeToggle: String
    val languageToggle: String
    val deviceToggle: String

    val languageChinese: String
    val languageEnglish: String

    val deviceNotConnected: String
    val deviceSelectMethod: String

    val windowMinimize: String
    val windowClose: String

    val deviceConnectUSB: String
    val deviceConnectWireless: String
    val deviceUSBGuide: String
    val deviceWirelessGuide: String
    val deviceRefresh: String
    val deviceConnect: String
    
    fun deviceUSBGuideStep1(): String
    fun deviceUSBGuideStep2(): String
    fun deviceUSBGuideStep3(): String
    fun deviceUSBGuideStep4(): String
    fun deviceWirelessGuideStep1(): String
    fun deviceWirelessGuideStep2(): String
    fun deviceWirelessGuideStep3(): String
    fun deviceWirelessGuideStep4(): String
    val deviceWirelessGuideIP: String
    val deviceWirelessGuidePort: String
    val deviceWirelessGuidePairPort: String
    val deviceWirelessGuidePairCode: String
    
    val transferFile: String
    val transferPreparing: String
    fun transferInProgress(percent: Int): String
}

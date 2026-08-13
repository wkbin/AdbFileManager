package model

enum class SortType(val displayName: String) {
    TYPE_ASC("类型 (文件夹优先)"),
    TYPE_DESC("类型 (文件优先)"),
    NAME_ASC("名称 (A-Z)"),
    NAME_DESC("名称 (Z-A)"),
    DATE_ASC("日期 (最早)"),
    DATE_DESC("日期 (最新)"),
    SIZE_ASC("大小 (最小)"),
    SIZE_DESC("大小 (最大)");

    companion object {
        fun updateDisplayNames() {
            // This can be called after StringsManager is initialized
        }
    }
}

package data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.Bookmark
import java.io.File

class BookmarkRepository {
    private val bookmarksFile: File by lazy {
        val appDataDir = File(System.getProperty("user.home"), ".adbfilemanager")
        if (!appDataDir.exists()) {
            appDataDir.mkdirs()
        }
        File(appDataDir, "bookmarks.json")
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    init {
        loadBookmarks()
    }

    private fun loadBookmarks() {
        if (bookmarksFile.exists()) {
            try {
                val content = bookmarksFile.readText()
                if (content.isNotBlank()) {
                    val list = json.decodeFromString<List<Bookmark>>(content)
                    _bookmarks.value = list.sortedByDescending { it.createdAt }
                }
            } catch (e: Exception) {
                println("Failed to load bookmarks: ${e.message}")
            }
        }
    }

    private fun saveBookmarks() {
        try {
            val content = json.encodeToString(_bookmarks.value)
            bookmarksFile.writeText(content)
        } catch (e: Exception) {
            println("Failed to save bookmarks: ${e.message}")
        }
    }

    suspend fun addBookmark(name: String, path: String): Bookmark = withContext(Dispatchers.IO) {
        val bookmark = Bookmark(name = name, path = path)
        val newList = _bookmarks.value.toMutableList()
        newList.add(0, bookmark)
        _bookmarks.value = newList
        saveBookmarks()
        bookmark
    }

    suspend fun removeBookmark(bookmark: Bookmark) = withContext(Dispatchers.IO) {
        val newList = _bookmarks.value.toMutableList()
        newList.remove(bookmark)
        _bookmarks.value = newList
        saveBookmarks()
    }

    suspend fun removeBookmarkByPath(path: String) = withContext(Dispatchers.IO) {
        val newList = _bookmarks.value.filterNot { it.path == path }
        _bookmarks.value = newList
        saveBookmarks()
    }

    suspend fun isBookmarked(path: String): Boolean {
        return _bookmarks.value.any { it.path == path }
    }

    fun getBookmarkByPath(path: String): Bookmark? {
        return _bookmarks.value.find { it.path == path }
    }
}

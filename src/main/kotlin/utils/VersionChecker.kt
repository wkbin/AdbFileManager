package utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import viewmodel.FileManagerViewModel
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 版本检查工具类
 */
object VersionChecker {
    private const val GITHUB_API_URL = "https://api.github.com/repos/wkbin/AdbFileManager/releases/latest"
    private val json = Json { ignoreUnknownKeys = true }
    
    // 配置文件
    private val configFile by lazy {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".adbfilemanager")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        File(appDir, "update_config.json")
    }
    
    // 更新配置
    @Serializable
    private data class UpdateConfig(
        val ignoredVersion: String = "",
        val neverShowUpdates: Boolean = false
    )
    
    // 获取更新配置
    private fun getUpdateConfig(): UpdateConfig {
        return try {
            if (configFile.exists()) {
                json.decodeFromString<UpdateConfig>(configFile.readText())
            } else {
                UpdateConfig()
            }
        } catch (e: Exception) {
            println("读取更新配置失败: ${e.message}")
            UpdateConfig()
        }
    }
    
    // 保存更新配置
    fun saveUpdateConfig(ignoredVersion: String = "", neverShowUpdates: Boolean = false) {
        try {
            val config = UpdateConfig(ignoredVersion, neverShowUpdates)
            configFile.writeText(json.encodeToString(config))
        } catch (e: Exception) {
            println("保存更新配置失败: ${e.message}")
        }
    }

    @Serializable
    private data class GitHubRelease(
        val tag_name: String,
        val html_url: String,
        val body: String
    )

    /**
     * 检查是否有新版本
     * @param forceCheck 是否强制检查，忽略用户设置
     * @return 如果有新版本，返回新版本信息，否则返回 null
     */
    suspend fun checkForUpdates(forceCheck: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            // 检查用户配置
            val config = getUpdateConfig()
            if (!forceCheck && config.neverShowUpdates) {
                return@withContext null
            }
            
            // 使用 OkHttp 替代 HttpClient
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()
                
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return@withContext null
                val release = json.decodeFromString<GitHubRelease>(responseBody)
                val currentVersion = FileManagerViewModel.VERSION
                val latestVersion = release.tag_name.removePrefix("v")
                
                // 如果用户已忽略此版本且不是强制检查，不再提示
                if (!forceCheck && config.ignoredVersion == latestVersion) {
                    return@withContext null
                }
                
                // 比较版本号
                if (isNewerVersion(latestVersion, currentVersion)) {
                    UpdateInfo(
                        version = latestVersion,
                        downloadUrl = release.html_url,
                        releaseNotes = release.body
                    )
                } else {
                    null
                }
            } else {
                println("检查更新失败，HTTP状态码: ${response.code}")
                null
            }
        } catch (e: Exception) {
            println("检查更新失败: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * 比较版本号
     * @return 如果 newVersion 比 currentVersion 新，返回 true
     */
    private fun isNewerVersion(newVersion: String, currentVersion: String): Boolean {
        val newParts = newVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
        
        for (i in 0 until maxOf(newParts.size, currentParts.size)) {
            val new = newParts.getOrNull(i) ?: 0
            val current = currentParts.getOrNull(i) ?: 0
            
            when {
                new > current -> return true
                new < current -> return false
            }
        }
        return false
    }
}
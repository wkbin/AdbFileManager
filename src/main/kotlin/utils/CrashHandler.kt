package utils

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

/**
 * 全局崩溃捕获与日志记录工具
 * 防止应用程序由于未捕获异常而静默退出（闪退/点击无反应），并将崩溃日志写入磁盘并弹窗提示。
 */
object CrashHandler {

    private val logDir: File by lazy {
        val appDataDir = File(System.getProperty("user.home"), ".adbfilemanager/logs")
        if (!appDataDir.exists()) {
            appDataDir.mkdirs()
        }
        appDataDir
    }

    private val crashLogFile: File by lazy {
        File(logDir, "crash.log")
    }

    fun init() {
        // 设置默认的全局未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleCrash(thread, throwable)
        }
    }

    fun logError(message: String, throwable: Throwable? = null) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        pw.println("[$timeStamp] ERROR: $message")
        throwable?.printStackTrace(pw)
        pw.flush()

        val logContent = sw.toString()
        System.err.println(logContent)

        try {
            crashLogFile.appendText(logContent + "\n")
        } catch (e: Exception) {
            System.err.println("Failed to write to crash log: ${e.message}")
        }
    }

    private fun handleCrash(thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())

        pw.println("================================================================================")
        pw.println("CRASH REPORT - $timeStamp")
        pw.println("Thread: ${thread.name} (id: ${thread.threadId()})")
        pw.println("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")})")
        pw.println("Java: ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})")
        pw.println("User Home: ${System.getProperty("user.home")}")
        pw.println("Working Dir: ${File(".").absolutePath}")
        pw.println("--------------------------------------------------------------------------------")
        throwable.printStackTrace(pw)
        pw.println("================================================================================")
        pw.println()
        pw.flush()

        val crashReport = sw.toString()
        System.err.println(crashReport)

        // 写入崩溃日志文件
        try {
            crashLogFile.appendText(crashReport)
        } catch (e: Exception) {
            System.err.println("Failed to write crash log to ${crashLogFile.absolutePath}: ${e.message}")
        }

        // 弹出系统级 Swing 错误对话框提示用户
        try {
            val shortMessage = """
                应用程序遇到严重错误被迫停止运行。
                
                错误原因: ${throwable::class.java.simpleName}: ${throwable.message ?: "无详细信息"}
                
                详细崩溃日志已保存至:
                ${crashLogFile.absolutePath}
            """.trimIndent()

            if (!GraphicsEnvironmentUtils.isHeadless()) {
                SwingUtilities.invokeAndWait {
                    JOptionPane.showMessageDialog(
                        null,
                        shortMessage,
                        "AdbFileManager 启动/运行时错误",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        } catch (_: Throwable) {
            // 忽略弹窗本身的异常，确保进程能退出
        }
    }
}

private object GraphicsEnvironmentUtils {
    fun isHeadless(): Boolean {
        return try {
            java.awt.GraphicsEnvironment.isHeadless()
        } catch (_: Throwable) {
            true
        }
    }
}

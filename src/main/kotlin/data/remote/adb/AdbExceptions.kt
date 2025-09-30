package data.remote.adb

import java.io.IOException

open class AdbException(message: String, cause: Throwable? = null) : IOException(message, cause)

class AdbCommandException(
    val exitCode: Int,
    val output: List<String>,
    message: String,
    cause: Throwable? = null
) : AdbException(message, cause) {
    constructor(exitCode: Int, output: List<String>) : this(
        exitCode,
        output,
        "ADB command failed with exit code $exitCode"
    )
}

class AdbConnectionException(
    message: String,
    cause: Throwable? = null
) : AdbException(message, cause)

class AdbDeviceNotFoundException(
    message: String = "No ADB device connected",
    cause: Throwable? = null
) : AdbException(message, cause)

class AdbPermissionException(
    message: String = "Permission denied",
    cause: Throwable? = null
) : AdbException(message, cause)

package runtime.adb.env

object Environment {
    private val resolver = EnvironmentResolver()

    val ANDROID_HOME get() = resolver.resolve("ANDROID_HOME")
    val ANDROID_NDK get() = resolver.resolve("ANDROID_NDK")
}

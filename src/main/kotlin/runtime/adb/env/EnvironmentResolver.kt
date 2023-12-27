package runtime.adb.env

import java.io.File
import java.util.regex.Pattern

internal class EnvironmentResolver {
    private val envVarsPattern by lazy { Pattern.compile("\\$(\\w+)") }

    private val variables: MutableMap<String, String> by lazy {
        val envMap = mutableMapOf<String, String>()
        val envFile = File(".env")
        if (envFile.exists()) {
            envFile.readLines().filter(this::isNotComment).forEach { line ->
                val (key, value) = line.split("=")
                envMap[key] = replaceEnvVars(value)
            }
        }
        envMap.forEach { (key, value) ->
            println("$key: $value")
        }
        envMap
    }

    fun resolve(name: String) = variables[name]
        ?: System.getenv(name)
        ?: throw IllegalStateException("$name environment variable is not set. Use the .env file or " +
                "set/export the variable before running.")

    private fun isNotComment(value: String): Boolean {
        return !value.trim().startsWith("#")
    }

    private fun replaceEnvVars(value: String): String {
        val standardizedPath = value
            .replace("\\", File.separator)
            .replace("/", File.separator)
        val matcher = envVarsPattern.matcher(standardizedPath)
        var result = standardizedPath
        while (matcher.find()) {
            val variable = matcher.group(1)
            val systemValue = System.getenv(variable)
            if (systemValue != null) {
                result = result.replace("\$$variable", systemValue)
            }
        }
        return result
    }
}

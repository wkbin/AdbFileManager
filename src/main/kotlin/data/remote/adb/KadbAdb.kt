package data.remote.adb

import com.flyfishxu.kadb.Kadb
import com.flyfishxu.kadb.cert.KadbCert
import com.flyfishxu.kadb.cert.OkioFilePrivateKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import model.AppSettings
import okio.Path.Companion.toPath
import okio.source
import okio.sink
import okio.ForwardingSource
import okio.ForwardingSink
import okio.Buffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.prefs.Preferences
import java.util.UUID

/** Opt-in direct wireless transport. CLI devices keep their existing backend. */
class KadbAdb(
    private val cli: Adb,
    identityDirectory: File,
    private val preferences: Preferences = Preferences.userNodeForPackage(KadbAdb::class.java),
    private val discovery: WirelessDiscovery = JmDnsWirelessDiscovery()
) : Adb by cli, AutoCloseable {
    private val endpoints = ConcurrentHashMap<String, Pair<String, Int>>()

    init {
        preferences.get("wirelessEndpoints", "").lineSequence().forEach { value ->
            val host = value.substringBeforeLast(':', "")
            val port = value.substringAfterLast(':').toIntOrNull()
            if (host.isNotBlank() && port != null && port in 1..65535) endpoints[value] = host to port
        }
        identityDirectory.mkdirs()
        KadbCert.configure(OkioFilePrivateKeyStore(File(identityDirectory, "kadb-key.pem").path.toPath()))
    }

    override suspend fun connect(ipAddress: String, port: String): AdbWifiState {
        if (!AppSettings.useKadbWireless) return cli.connect(ipAddress, port)
        val host = ipAddress.trim()
        require(host.isNotEmpty() && host.none(Char::isWhitespace)) { "请输入有效的设备 IP 地址" }
        val resolvedPort = if (port.isBlank()) {
            startDiscovery()
            try {
                withTimeout(10000) {
                    var discovered = discovery.connectDevices.firstOrNull { it.host == host }
                    while (discovered == null) {
                        delay(200)
                        discovered = discovery.connectDevices.firstOrNull { it.host == host }
                    }
                    discovered.port
                }
            } catch (timeout: kotlinx.coroutines.TimeoutCancellationException) {
                throw AdbConnectionException("未发现无线调试连接端口，请填写手机无线调试主页面显示的连接端口", timeout)
            }
        } else port.toInt().also { require(it in 1..65535) }
        val endpoint = host to resolvedPort
        val id = "${endpoint.first}:${endpoint.second}"
        operate(endpoint) { client ->
            val response = client.shell("echo kadb-connected")
            check(response.exitCode == 0) { response.errorOutput }
        }
        synchronized(endpoints) {
            endpoints[id] = endpoint
            preferences.put("wirelessEndpoints", endpoints.keys.sorted().joinToString("\n"))
        }
        return AdbWifiState(true, endpoint.first, endpoint.second.toString())
    }

    override suspend fun pair(ipAddress: String, port: String, code: String): List<String> {
        if (!AppSettings.useKadbWireless) return cli.pair(ipAddress, port, code)
        require(port.toIntOrNull() in 1..65535) { "配对端口应为 1–65535" }
        require(code.trim().matches(Regex("[0-9]{6}"))) { "请输入手机显示的六位配对码" }
        startDiscovery()
        Kadb.pair(ipAddress.trim(), port.toInt(), code.trim())
        return listOf("Successfully paired to $ipAddress:$port")
    }

    override suspend fun devices(): List<String> {
        val cliDevices = try {
            cli.devices()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (!AppSettings.useKadbWireless) throw error
            emptyList()
        }
        if (!AppSettings.useKadbWireless) {
            withContext(Dispatchers.IO) { discovery.stop() }
            return cliDevices
        }
        startDiscovery()
        val discovered = discovery.connectDevices
        val candidates = endpoints.toMutableMap()
        discovered.forEach { service ->
            val id = "${service.name}.${service.serviceType}"
            val address = service.host to service.port
            endpoints[id] = address
            candidates.entries.removeIf { it.key != id && it.value.first == service.host }
            candidates[id] = address
        }
        val permits = Semaphore(4)
        val reachable = coroutineScope {
            candidates.map { (id, address) ->
                async {
                    permits.withPermit {
                        try {
                            if (operate(address, probe = true) { it.shell("true").exitCode == 0 }) id else null
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }
        val aliases = discovered.flatMap { listOf(it.name, "${it.host}:${it.port}") }.toSet()
        return (cliDevices.filterNot { it in aliases } + reachable).distinct()
    }

    private suspend fun startDiscovery() = withContext(Dispatchers.IO) { discovery.start() }

    override fun close() = discovery.close()

    override suspend fun wifiState(deviceId: String): AdbWifiState {
        val endpoint = endpoint(deviceId) ?: return cli.wifiState(deviceId)
        return AdbWifiState(true, endpoint.first, endpoint.second.toString())
    }

    override suspend fun shell(adbDevice: AdbDevice, vararg args: String, throwOnError: Boolean): List<String> {
        val endpoint = endpoint(adbDevice.deviceId)
            ?: return cli.shell(adbDevice, *args, throwOnError = throwOnError)
        return operate(endpoint) {
            // Callers already quote shell tokens and deliberately supply redirects/operators.
            val response = it.shell(args.joinToString(" "))
            val lines = (response.output + response.errorOutput).reader().buffered().readLines()
            if (throwOnError && response.exitCode != 0) throw AdbCommandException(response.exitCode, lines)
            lines
        }
    }

    override suspend fun push(adbDevice: AdbDevice, localPath: String, remotePath: String, throwOnError: Boolean): List<String> {
        val endpoint = endpoint(adbDevice.deviceId)
            ?: return cli.push(adbDevice, localPath, remotePath, throwOnError)
        operate(endpoint) { client ->
            upload(client, File(localPath), remotePath, cleanup = { cleanupRemote(endpoint, it) })
        }
        return listOf("Uploaded $localPath")
    }

    override suspend fun pull(adbDevice: AdbDevice, remotePath: String, localPath: String, throwOnError: Boolean): List<String> {
        val endpoint = endpoint(adbDevice.deviceId)
            ?: return cli.pull(adbDevice, remotePath, localPath, throwOnError)
        operate(endpoint) { client ->
            download(client, remotePath, File(localPath))
        }
        return listOf("Downloaded $remotePath")
    }

    override fun pushWithProgress(adbDevice: AdbDevice, localPath: String, remotePath: String, progressCallback: AdbTransferProgress): Flow<String> {
        if (endpoint(adbDevice.deviceId) == null) return cli.pushWithProgress(adbDevice, localPath, remotePath, progressCallback)
        return flow {
            progressCallback.onProgress(0)
            val address = endpoint(adbDevice.deviceId) ?: error("Kadb session unavailable")
            operate(address) {
                upload(it, File(localPath), remotePath, progressCallback, cleanup = { path -> cleanupRemote(address, path) })
            }
            emit("Uploaded $localPath")
            progressCallback.onProgress(100)
        }.flowOn(Dispatchers.IO)
    }

    override fun pullWithProgress(adbDevice: AdbDevice, remotePath: String, localPath: String, progressCallback: AdbTransferProgress): Flow<String> {
        if (endpoint(adbDevice.deviceId) == null) return cli.pullWithProgress(adbDevice, remotePath, localPath, progressCallback)
        return flow {
            progressCallback.onProgress(0)
            operate(endpoint(adbDevice.deviceId) ?: error("Kadb session unavailable")) {
                download(it, remotePath, File(localPath), progressCallback)
            }
            emit("Downloaded $remotePath")
            progressCallback.onProgress(100)
        }.flowOn(Dispatchers.IO)
    }

    private fun upload(
        client: Kadb,
        source: File,
        requestedPath: String,
        progress: AdbTransferProgress? = null,
        resolveDirectory: Boolean = true,
        cleanup: (String) -> Unit
    ) {
        require(source.exists()) { "Upload source does not exist: $source" }
        val remoteIsDirectory = resolveDirectory && client.shell("test -d ${shellQuote(requestedPath)}").exitCode == 0
        val target = if (remoteIsDirectory) requestedPath.trimEnd('/') + "/" + source.name else requestedPath
        if (source.isDirectory) {
            check(client.shell("mkdir -p ${shellQuote(target)}").exitCode == 0) { "Cannot create $target" }
            val children = source.listFiles() ?: error("Cannot read $source")
            children.forEach { upload(client, it, target.trimEnd('/') + "/" + it.name, resolveDirectory = false, cleanup = cleanup) }
            return
        }
        val parent = target.substringBeforeLast('/', ".").ifEmpty { "/" }
        val stagedPath = parent.trimEnd('/') + "/.afm-kadb-${UUID.randomUUID()}"
        var committed = false
        try {
            var transferred = 0L
            source.source().use { input ->
                val counting = object : ForwardingSource(input) {
                    override fun read(sink: Buffer, byteCount: Long): Long {
                        val count = super.read(sink, byteCount)
                        if (count > 0) {
                            transferred += count
                            progress?.onProgress((transferred * 100 / source.length().coerceAtLeast(1)).toInt().coerceIn(0, 99))
                        }
                        return count
                    }
                }
                client.push(counting, stagedPath, 33188, source.lastModified())
                val response = client.shell("mv -f -- ${shellQuote(stagedPath)} ${shellQuote(target)}")
                if (response.exitCode != 0) throw AdbCommandException(response.exitCode, response.errorOutput.lines())
                committed = true
            }
        } finally {
            if (!committed) runCatching { cleanup(stagedPath) }
        }
    }

    private fun cleanupRemote(address: Pair<String, Int>, path: String) {
        Kadb.create(address.first, address.second, connectTimeout = 1000, socketTimeout = 1000).use {
            it.shell("rm -f -- ${shellQuote(path)}")
        }
    }

    private fun download(
        client: Kadb,
        remotePath: String,
        requestedFile: File,
        progress: AdbTransferProgress? = null,
        resolveDirectory: Boolean = true
    ) {
        val stat = client.openSync().use { it.lstat(remotePath) }
        val target = if (resolveDirectory && requestedFile.isDirectory) File(requestedFile, remotePath.trimEnd('/').substringAfterLast('/')) else requestedFile
        if (stat.mode and 61440 == 16384) {
            check(target.isDirectory || target.mkdirs()) { "Cannot create $target" }
            val entries = client.openSync().use { it.list(remotePath) }
            entries.filterNot { it.name == "." || it.name == ".." }.forEach {
                require(it.name.isNotEmpty() && '/' !in it.name && '\\' !in it.name) { "Invalid remote entry name" }
                download(client, remotePath.trimEnd('/') + "/" + it.name, File(target, it.name), resolveDirectory = false)
            }
            return
        }
        target.absoluteFile.parentFile?.mkdirs()
        val temporary = File.createTempFile(".kadb-", ".part", target.absoluteFile.parentFile)
        try {
            var transferred = 0L
            temporary.sink().use { output ->
                val counting = object : ForwardingSink(output) {
                    override fun write(source: Buffer, byteCount: Long) {
                        super.write(source, byteCount)
                        transferred += byteCount
                        progress?.onProgress((transferred * 100 / stat.size.coerceAtLeast(1)).toInt().coerceIn(0, 99))
                    }
                }
                client.pull(counting, remotePath)
            }
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
    }

    private fun shellQuote(value: String): String = "'" + value.replace("'", "'\"'\"'") + "'"

    internal fun openClient(device: AdbDevice): Kadb? = endpoint(device.deviceId)?.let {
        Kadb.create(it.first, it.second, connectTimeout = 5000, socketTimeout = 0)
    }

    internal suspend fun screenshot(device: AdbDevice, destination: File): Boolean {
        val endpoint = endpoint(device.deviceId) ?: return false
        operate(endpoint) { client ->
            val parent = destination.absoluteFile.parentFile
            parent?.mkdirs()
            val temporary = File.createTempFile(".kadb-screen-", ".png", parent)
            try {
                client.open("exec:screencap -p").use { stream ->
                    temporary.sink().use { stream.source.readAll(it) }
                }
                val signature = temporary.inputStream().use { it.readNBytes(8) }
                check(signature.contentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10))) {
                    "Device did not return a PNG screenshot"
                }
                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } finally {
                temporary.delete()
            }
        }
        return true
    }

    private fun endpoint(id: String) = if (AppSettings.useKadbWireless) endpoints[id] else null

    private suspend fun <T> operate(endpoint: Pair<String, Int>, probe: Boolean = false, block: (Kadb) -> T): T = coroutineScope {
        val client = Kadb.create(endpoint.first, endpoint.second, connectTimeout = if (probe) 2000 else 5000, socketTimeout = if (probe) 3000 else 15000)
        val result = async(Dispatchers.IO) {
            coroutineContext.ensureActive()
            try {
                block(client)
            } catch (error: Exception) {
                coroutineContext.ensureActive()
                throw error
            }
        }
        try {
            result.await()
        } finally {
            // Close the transport before waiting for a cancelled blocking worker to finish.
            client.close()
        }
    }
}

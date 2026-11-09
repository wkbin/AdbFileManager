package data.remote.adb

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

data class WirelessEndpoint(val name: String, val host: String, val port: Int, val serviceType: String)

interface WirelessDiscovery : AutoCloseable {
    val connectDevices: List<WirelessEndpoint>
    fun start()
    fun stop()
    override fun close() = stop()
}

/** One multicast listener per active IPv4 interface, shared by device polling. */
class JmDnsWirelessDiscovery : WirelessDiscovery {
    private val services = ConcurrentHashMap<String, WirelessEndpoint>()
    private val instances = mutableListOf<JmDNS>()
    private var generation = 0

    override val connectDevices: List<WirelessEndpoint>
        get() = services.values.toList().sortedBy { it.name }

    @Synchronized
    override fun start() {
        if (instances.isNotEmpty()) return
        val currentGeneration = ++generation
        val addresses = NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && it.supportsMulticast() && !it.isVirtual }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .filterNot { it.isLoopbackAddress || it.isLinkLocalAddress }
            .distinct()
        addresses.forEach { address ->
            val dns = try {
                JmDNS.create(address, "AdbFileManager")
            } catch (_: java.io.IOException) {
                return@forEach
            }
            instances += dns
            listOf("_adb._tcp", "_adb-tls-connect._tcp").forEach { serviceType ->
                val type = "$serviceType.local."
                dns.addServiceListener(type, object : ServiceListener {
                    override fun serviceAdded(event: ServiceEvent) {
                        dns.requestServiceInfo(event.type, event.name)
                    }

                    override fun serviceResolved(event: ServiceEvent) {
                        val host = event.info.inet4Addresses.firstOrNull()?.hostAddress ?: return
                        if (event.info.port !in 1..65535) return
                        synchronized(this@JmDnsWirelessDiscovery) {
                            if (generation != currentGeneration) return
                            services["${address.hostAddress}/${event.name}/$serviceType"] =
                                WirelessEndpoint(event.name, host, event.info.port, serviceType)
                        }
                    }

                    override fun serviceRemoved(event: ServiceEvent) {
                        synchronized(this@JmDnsWirelessDiscovery) {
                            if (generation == currentGeneration) {
                                services.remove("${address.hostAddress}/${event.name}/$serviceType")
                            }
                        }
                    }
                })
            }
        }
    }

    override fun stop() {
        val closing = synchronized(this) {
            generation++
            services.clear()
            instances.toList().also { instances.clear() }
        }
        closing.forEach { runCatching { it.close() } }
    }
}

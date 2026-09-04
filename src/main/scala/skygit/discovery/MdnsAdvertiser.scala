package skygit.discovery

import java.net.InetAddress
import javax.jmdns.{JmDNS, ServiceInfo}

class MdnsAdvertiser(gitPort: Int, webPort: Int) extends AutoCloseable {

    private var jmdns: Option[JmDNS] = None

    def start(): Unit = {
        val address = InetAddress.getLocalHost
        val dns = JmDNS.create(address, "skygit")

        val service = ServiceInfo.create(
            "_git._tcp.local.",
            "SkyGit",
            gitPort,
            "SkyGit Git server"
        )

        dns.registerService(
            ServiceInfo.create(
                "_http._tcp.local.",
                "SkyGit Web",
                webPort,
                "SkyGit web interface"
            )
        )

        dns.registerService(service)
        jmdns = Some(dns)

        println(s"mDNS: SkyGit advertised on git port $gitPort and web port $webPort")
    }

    def stop(): Unit = {
        jmdns.foreach(_.close())
        jmdns = None
    }

    override def close(): Unit =
        stop()
}

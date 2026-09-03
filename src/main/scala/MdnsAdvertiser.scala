import java.net.InetAddress
import javax.jmdns.{JmDNS, ServiceInfo}

class MdnsAdvertiser(port: Int) {

    private var jmdns: Option[JmDNS] = None

    def start(): Unit = {
        val address = InetAddress.getLocalHost
        val dns = JmDNS.create(address, "skygit")

        val service = ServiceInfo.create(
            "_git._tcp.local.",
            "SkyGit",
            port,
            "SkyGit Git server"
        )

        dns.registerService(service)
        jmdns = Some(dns)

        println(s"mDNS: SkyGit advertised on port $port")
    }

    def stop(): Unit = {
        jmdns.foreach(_.close())
        jmdns = None
    }
}

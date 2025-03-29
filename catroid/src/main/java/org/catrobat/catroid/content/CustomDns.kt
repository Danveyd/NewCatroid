import android.util.Log
import java.net.InetAddress
import java.net.UnknownHostException
import okhttp3.Dns
import org.catrobat.catroid.content.GeminiManager

class CustomDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return try {
            // Проверка, установлен ли dns_server
            if (GeminiManager.dns_server.isNullOrEmpty()) {
                // Используем стандартный DNS
                Dns.SYSTEM.lookup(hostname)
            } else {
                // Используем указанный DNS сервер с доменным именем
                Log.d("CustomDns", "Resolving hostname using DNS server: ${GeminiManager.dns_server}")
                val addresses = InetAddress.getAllByName(GeminiManager.dns_server)
                addresses.toList()
            }
        } catch (e: UnknownHostException) {
            Log.e("CustomDns", "Error resolving DNS: ${e.message}")
            throw e
        }
    }
}
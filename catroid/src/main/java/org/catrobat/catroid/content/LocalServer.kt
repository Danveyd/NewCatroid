package org.catrobat.catroid.content

import android.util.Log
import kotlinx.coroutines.*
import org.catrobat.catroid.utils.ErrorLog
import java.io.*
import java.net.*

class LocalServer private constructor() {
    companion object {
        private var serverSocket: ServerSocket? = null
        private var clientSocket: Socket? = null
        private var outputStream: OutputStream? = null
        private var connectedPort: String? = null
        private var connectedIP: String? = null
        private var receivedValue: String = ""
        private var isRunning = false

        private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        fun startOrJoin(ip: String?, port: String) {
            coroutineScope.launch {
                try {
                    isRunning = true
                    if (ip.isNullOrEmpty()) {
                        startServer(port)
                    } else {
                        connectToServer(ip, port)
                    }
                } catch (e: Exception) {
                    if (isRunning) {
                        ErrorLog.log(e.message ?: "**message not provided :(**")
                        Log.e("LocalServer", "Ошибка в startOrJoin: ${e.message}", e)
                    }
                } finally {
                    if (isRunning) {
                        stop()
                    }
                }
            }
        }

        private suspend fun startServer(port: String) {
            serverSocket = ServerSocket(port.toInt())
            connectedPort = port
            connectedIP = getLocalIPAddress()
            Log.d("LocalServer", "Сервер запущен на $connectedIP:$port и ожидает подключения...")

            val socket = serverSocket!!.accept()
            Log.d("LocalServer", "Клиент подключился: ${socket.remoteSocketAddress}")

            clientSocket = socket
            outputStream = socket.getOutputStream()

            listenForMessages(socket)
        }

        private suspend fun connectToServer(ip: String, port: String) {
            val socket = Socket(ip, port.toInt())
            clientSocket = socket
            outputStream = socket.getOutputStream()
            connectedIP = ip
            connectedPort = port
            Log.d("LocalServer", "Подключен к серверу $ip:$port")

            listenForMessages(socket)
        }

        private fun handleClient(socket: Socket) {
            coroutineScope.launch {
                listenForMessages(socket)
            }
        }

        private suspend fun listenForMessages(socket: Socket) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (isRunning && socket.isConnected) {

                    val message = reader.readLine()
                    if (message == null) {
                        Log.d("LocalServer", "Соединение разорвано удаленной стороной.")
                        break
                    }
                    receivedValue = message
                    Log.d("LocalServer", "Получено: $message")
                }
            } catch (e: Exception) {
                if (isRunning) {
                    ErrorLog.log(e.message ?: "**message not provided :(**")
                    Log.e("LocalServer", "Ошибка чтения: ${e.message}")
                }
            }
        }

        fun send(value: String) {
            coroutineScope.launch {
                if (outputStream == null) {
                    Log.e("LocalServer", "Ошибка: соединение не установлено (outputStream is null).")
                    return@launch
                }
                try {
                    outputStream?.write((value + "\n").toByteArray())
                    outputStream?.flush()
                    Log.d("LocalServer", "Отправлено: $value")
                } catch (e: Exception) {
                    ErrorLog.log(e.message ?: "**message not provided :(**")
                    Log.e("LocalServer", "Ошибка отправки: ${e.message}")
                }
            }
        }

        fun stop() {
            if (!isRunning) return
            isRunning = false
            try {
                serverSocket?.close()
                clientSocket?.close()
            } catch (e: IOException) {
                Log.w("LocalServer", "Ошибка при закрытии сокетов: ${e.message}")
            } finally {
                serverSocket = null
                clientSocket = null
                outputStream = null
                Log.d("LocalServer", "Сервер/клиент остановлен.")
            }
        }

        fun getValue(): String = receivedValue
        fun getIP(): String = connectedIP ?: "NaN"
        fun getPort(): String = connectedPort ?: "NaN"

        private fun getLocalIPAddress(): String? {
            try {
                return NetworkInterface.getNetworkInterfaces().toList()
                    .flatMap { it.inetAddresses.toList() }
                    .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                    ?.hostAddress
            } catch (e: Exception) {
                Log.e("LocalServer", "Не удалось получить локальный IP-адрес", e)
                return null
            }
        }
    }
}
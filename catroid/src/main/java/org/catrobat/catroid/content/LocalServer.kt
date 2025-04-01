package org.catrobat.catroid.content

import android.util.Log
import kotlinx.coroutines.*
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

        fun startOrJoin(ip: String?, port: String) {
            GlobalScope.launch {
                try {
                    if (ip.isNullOrEmpty()) {
                        startServer(port)
                    } else {
                        connectToServer(ip, port)
                    }
                } catch (e: Exception) {
                    Log.e("LocalServer", "Ошибка: ${e.message}")
                }
            }
        }

        private fun startServer(port: String) {
            try {
                serverSocket = ServerSocket(port.toInt())
                connectedPort = port
                connectedIP = getLocalIPAddress()
                isRunning = true
                Log.d("LocalServer", "Сервер запущен на $connectedIP:$port")

                while (isRunning) {
                    val socket = serverSocket!!.accept()
                    handleClient(socket)
                }
            } catch (e: Exception) {
                Log.e("LocalServer", "Ошибка запуска сервера: ${e.message}")
            }
        }

        private fun connectToServer(ip: String, port: String) {
            try {
                clientSocket = Socket(ip, port.toInt())
                outputStream = clientSocket!!.getOutputStream()
                connectedIP = ip
                connectedPort = port
                isRunning = true
                Log.d("LocalServer", "Подключен к серверу $ip:$port")
                listenForMessages(clientSocket!!)
            } catch (e: Exception) {
                Log.e("LocalServer", "Ошибка подключения: ${e.message}")
            }
        }

        private fun handleClient(socket: Socket) {
            GlobalScope.launch {
                listenForMessages(socket)
            }
        }

        private fun listenForMessages(socket: Socket) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (isRunning) {
                    val message = reader.readLine() ?: break
                    receivedValue = message
                    Log.d("LocalServer", "Получено: $message")
                }
            } catch (e: Exception) {
                Log.e("LocalServer", "Ошибка чтения: ${e.message}")
            }
        }

        fun send(value: String) {
            GlobalScope.launch {
                try {
                    outputStream?.write((value + "\n").toByteArray())
                    outputStream?.flush()
                    Log.d("LocalServer", "Отправлено: $value")
                } catch (e: Exception) {
                    Log.e("LocalServer", "Ошибка отправки: ${e.message}")
                }
            }
        }

        fun stop() {
            isRunning = false
            serverSocket?.close()
            clientSocket?.close()
            outputStream = null
            Log.d("LocalServer", "Сервер остановлен")
        }

        fun getValue(): String = receivedValue
        fun getIP(): String = connectedIP ?: "NaN"
        fun getPort(): String = connectedPort ?: "NaN"

        private fun getLocalIPAddress(): String? {
            return NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is InetAddress && !it.isLoopbackAddress && it.address.size == 4 }
                ?.hostAddress
        }
    }
}

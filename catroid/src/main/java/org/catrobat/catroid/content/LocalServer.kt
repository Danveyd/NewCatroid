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

        // ИЗМЕНЕНО: Используем кастомный CoroutineScope для лучшего управления жизненным циклом
        private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        fun startOrJoin(ip: String?, port: String) {
            // ИЗМЕНЕНО: Запускаем корутину в нашем scope
            coroutineScope.launch {
                try {
                    isRunning = true
                    if (ip.isNullOrEmpty()) {
                        startServer(port)
                    } else {
                        connectToServer(ip, port)
                    }
                } catch (e: Exception) {
                    if (isRunning) { // Логируем ошибку, только если сервер не был остановлен намеренно
                        ErrorLog.log(e.message ?: "**message not provided :(**")
                        Log.e("LocalServer", "Ошибка в startOrJoin: ${e.message}", e)
                    }
                } finally {
                    if (isRunning) {
                        stop() // Убедимся, что все ресурсы освобождены при ошибке
                    }
                }
            }
        }

        private suspend fun startServer(port: String) {
            serverSocket = ServerSocket(port.toInt())
            connectedPort = port
            connectedIP = getLocalIPAddress()
            Log.d("LocalServer", "Сервер запущен на $connectedIP:$port и ожидает подключения...")

            // Принимаем ОДНО подключение. Это блокирующая операция.
            val socket = serverSocket!!.accept()
            Log.d("LocalServer", "Клиент подключился: ${socket.remoteSocketAddress}")

            // ИЗМЕНЕНО: Вот ключевое исправление.
            // Теперь сервер тоже сохраняет clientSocket и outputStream.
            // После этого он ничем не отличается от клиента.
            clientSocket = socket
            outputStream = socket.getOutputStream()

            // Начинаем слушать сообщения от подключенного клиента
            listenForMessages(socket)
        }

        private suspend fun connectToServer(ip: String, port: String) {
            val socket = Socket(ip, port.toInt())
            clientSocket = socket
            outputStream = socket.getOutputStream()
            connectedIP = ip
            connectedPort = port
            Log.d("LocalServer", "Подключен к серверу $ip:$port")

            // Начинаем слушать сообщения от сервера
            listenForMessages(socket)
        }

        // Эта функция не требует изменений, но теперь ее вызывает и сервер, и клиент
        private fun handleClient(socket: Socket) {
            coroutineScope.launch {
                listenForMessages(socket)
            }
        }

        private suspend fun listenForMessages(socket: Socket) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (isRunning && socket.isConnected) {
                    // readLine() будет ждать, пока не придет строка с символом новой строки '\n'
                    val message = reader.readLine()
                    if (message == null) {
                        Log.d("LocalServer", "Соединение разорвано удаленной стороной.")
                        break // Выходим из цикла, если стрим закрыт
                    }
                    receivedValue = message
                    Log.d("LocalServer", "Получено: $message")
                }
            } catch (e: Exception) {
                if (isRunning) { // Не показываем ошибку, если мы сами закрыли сокет
                    ErrorLog.log(e.message ?: "**message not provided :(**")
                    Log.e("LocalServer", "Ошибка чтения: ${e.message}")
                }
            }
        }

        fun send(value: String) {
            // ИЗМЕНЕНО: Запускаем в том же scope
            coroutineScope.launch {
                if (outputStream == null) {
                    Log.e("LocalServer", "Ошибка: соединение не установлено (outputStream is null).")
                    return@launch
                }
                try {
                    // Добавляем '\n' в конец, чтобы readLine() на другой стороне сработал
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
                clientSocket?.close() // Закрытие сокета также закроет его input/output stream
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
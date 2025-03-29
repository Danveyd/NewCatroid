package org.catrobat.catroid.content

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.catrobat.catroid.stage.StageActivity
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.ArrayList

class LocalServer private constructor() {

    companion object {
        private var serverSocket: ServerSocket? = null
        private var clientSocket: Socket? = null
        private var outputStream: OutputStream? = null
        private var connectedPort: String? = null
        private var connectedIP: String? = null
        private var receivedValue: String = ""

        fun toast(gval: String) {
            val params = ArrayList<Any>(listOf(gval))
            StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
        }
        // Запускает сервер на указанном порту
        fun start(port: String) {
            GlobalScope.launch {
                try {
                    serverSocket = ServerSocket(port.toInt())
                    connectedPort = port
                    connectedIP = NetworkInterface.getNetworkInterfaces().toList()
                        .flatMap { it.inetAddresses.toList() }
                        .firstOrNull { it is InetAddress && !it.isLoopbackAddress && it.address.size == 4 }?.hostAddress
                    //toast("Сервер запущен на порту: $port")
                    Log.d("Server", "Сервер запущен на порту: $port")

                    while (true) {
                        clientSocket = serverSocket!!.accept()
                        // Новая корутина для обработки каждого клиента
                        //toast("Клиент подключился: ${clientSocket?.inetAddress}")
                        Log.d("Server", "Клиент подключился: ${clientSocket?.inetAddress}")
                        try {
                            // Слушаем поток ввода от клиента
                            val bufferedReader = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
                            while (true) {
                                receivedValue = bufferedReader.readLine() ?: break
                                //toast("Получено значение: $receivedValue")
                                Log.d("Server", "Получено значение: $receivedValue")
                                // Обработай полученные данные здесь
                            }
                        } catch (e: Exception) {
                            toast("Ошибка при обработке клиента: ${e.message}")
                            Log.d("Server", "Ошибка при обработке клиента: ${e.message}")
                        } finally {
                            clientSocket?.close()
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Останавливает сервер
        fun stop() {
            try {
                clientSocket?.close()
                serverSocket?.close()
                //toast("Сервер остановлен.")
                Log.d("Server","Сервер остановлен.")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Отправляет значение подключенным устройствам
        fun send(value: String) {
            try {
                if (clientSocket != null && !clientSocket!!.isClosed) {
                    val outputStream = clientSocket!!.getOutputStream() // Получаем OutputStream каждый раз
                    outputStream.write((value + "\n").toByteArray()) // Добавляем перевод строки для удобства
                    outputStream.flush()
                } else {
                    toast("Клиент не подключен")
                    Log.d("Server", "Клиент не подключен.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        // Возвращает последнее полученное значение
        fun getValue(): String {
            return receivedValue
        }

        // Подключается к другому локальному серверу
        fun connect(ip: String, port: String) {
            try {
                connectedIP = ip
                connectedPort = port
                clientSocket = Socket(ip, port.toInt())
                //toast("Подключено к серверу: $ip:$port")
                Log.d("Server", "Подключено к серверу: $ip:$port")

                // Также инициализируй outputStream
                outputStream = clientSocket?.getOutputStream()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        // Получает IP запущенного сервера
        fun getIP(): String {
            return connectedIP ?: "NaN"
        }

        // Получает порт запущенного сервера
        fun getPort(): String {
            return connectedPort ?: "NaN"
        }
    }
}

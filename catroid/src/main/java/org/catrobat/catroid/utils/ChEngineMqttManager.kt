package org.catrobat.catroid.utils

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import org.catrobat.catroid.content.EventWrapper
import org.catrobat.catroid.content.eventids.MqttMessageEventId
import org.catrobat.catroid.stage.StageActivity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ChEngineMqttManager {

    private val clients = ConcurrentHashMap<String, Mqtt3AsyncClient>()
    private val latestMessages = ConcurrentHashMap<String, String>()

    fun connect(id: String, host: String, port: Int, onComplete: (Boolean) -> Unit) {
        val existingClient = clients[id]
        if (existingClient != null && existingClient.state.isConnected) {
            onComplete(true)
            return
        }

        try {
            val client = MqttClient.builder()
                .useMqttVersion3()
                .identifier("chengine_${UUID.randomUUID().toString().take(8)}")
                .serverHost(host)
                .serverPort(port)
                .buildAsync()

            client.connectWith()
                .send()
                .whenComplete { _, throwable ->
                    if (throwable != null) {
                        throwable.printStackTrace()
                        onComplete(false)
                    } else {
                        clients[id] = client
                        onComplete(true)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(false)
        }
    }

    fun joinRoom(clientId: String, roomId: String, salt: String, onComplete: (Boolean) -> Unit) {
        val client = clients[clientId]
        if (client == null) {
            onComplete(false)
            return
        }

        if (!client.state.isConnected) {
            onComplete(false)
            return
        }

        val safeTopic = getSaltedTopic(roomId, salt)

        client.subscribeWith()
            .topicFilter(safeTopic)
            .callback { publish ->
                val payload = String(publish.payloadAsBytes)
                latestMessages[roomId] = payload

                triggerMqttEvent(roomId)
            }
            .send()
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    throwable.printStackTrace()
                    onComplete(false)
                } else {
                    onComplete(true)
                }
            }
    }

    fun publishMessage(clientId: String, roomId: String, salt: String, message: String) {
        val client = clients[clientId] ?: return
        if (!client.state.isConnected) return

        val safeTopic = getSaltedTopic(roomId, salt)

        client.publishWith()
            .topic(safeTopic)
            .payload(message.toByteArray())
            .send()
    }

    fun getLatestMessage(roomId: String): String {
        return latestMessages[roomId] ?: ""
    }

    private fun getSaltedTopic(roomId: String, salt: String): String {
        val cleanSalt = salt.trim().replace(" ", "_")
        val prefix = cleanSalt.ifEmpty { "public" }
        return "chengine/$prefix/rooms/$roomId"
    }

    private fun triggerMqttEvent(roomId: String) {
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val project = org.catrobat.catroid.ProjectManager.getInstance().currentProject ?: return

        StageActivity.activeStageActivity.get()?.runOnUiThread {
            try {
                for (sprite in stageListener.spritesFromStage) {
                    for (script in sprite.scriptList) {
                        if (script is org.catrobat.catroid.content.WhenMqttMessageScript) {

                            val roomFormula = script.formulaMap[org.catrobat.catroid.content.bricks.Brick.BrickField.VALUE_1]
                            if (roomFormula != null) {
                                val scope = org.catrobat.catroid.content.Scope(project, sprite, null)
                                val expectedRoom = roomFormula.interpretString(scope)

                                if (expectedRoom == roomId) {
                                    val sequence = sprite.createSequenceAction(script)
                                    sprite.look.addAction(sequence)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun disconnect(id: String) {
        val client = clients[id]
        if (client != null && client.state.isConnected) {
            client.disconnect()
        }
        clients.remove(id)
    }

    fun disconnectAll() {
        clients.forEach { (_, client) ->
            if (client.state.isConnected) {
                client.disconnect()
            }
        }
        clients.clear()
        latestMessages.clear()
    }
}

package ru.smi_alexey.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed class WebSocketMessage {
    abstract val type: String
}

@Serializable
data class TextMessage(
    override val type: String = "text",
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String? = null
) : WebSocketMessage()

@Serializable
data class CommandMessage(
    override val type: String = "command",
    val command: String,
    val params: Map<String, String> = emptyMap(),
    val target: String? = null
) : WebSocketMessage()

@Serializable
data class StatusUpdate(
    override val type: String = "status",
    val status: String,
    val lastActivity: Long = System.currentTimeMillis()
) : WebSocketMessage()

@Serializable
data class ServerResponse(
    val success: Boolean,
    val message: String,
    val processedAt: Long = System.currentTimeMillis()
)

// Обёртка для динамической десериализации
@Serializable
data class MessageWrapper(
    val type: String,
    val version: String = "1.0",
    val data: JsonObject
)

// Модуль сериализаторов
val webSocketSerializersModule = SerializersModule {
    polymorphic(WebSocketMessage::class) {
        subclass(TextMessage::class, TextMessage.serializer())
        subclass(CommandMessage::class, CommandMessage.serializer())
        subclass(StatusUpdate::class, StatusUpdate.serializer())
    }
}

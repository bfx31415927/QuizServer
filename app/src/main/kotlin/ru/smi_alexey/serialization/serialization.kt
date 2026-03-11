package ru.smi_alexey.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

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
    val lastActivity: Long = System.currentTimeMillis(),
    val userId: String? = null
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
    val wr_type: String,
    val version: String = "1.0",
    val data: JsonObject
)

// Модуль сериализаторов
val webSocketSerializersModule = SerializersModule {
    polymorphic(WebSocketMessage::class) {
//        subclass(TextMessage::class, TextMessage.serializer())
//        subclass(CommandMessage::class, CommandMessage.serializer())
//        subclass(StatusUpdate::class, StatusUpdate.serializer())
        subclass(TextMessage::class)
        subclass(CommandMessage::class)
        subclass(StatusUpdate::class)
    }
}

// Определяем тип сообщения по структуре JSON
fun analyzeMessageType(jsonString: String): MessageType {
    return try {
        val jsonElement = Json.parseToJsonElement(jsonString).jsonObject

        // Проверяем наличие поля "wr_type" — признак обёртки
        if ("wr_type" in jsonElement) {
            MessageType.WRAPPED
        } else if ("type" in jsonElement) {
            // Если есть "type", но нет "wr_type" — прямой формат
            MessageType.DIRECT
        } else {
            MessageType.UNKNOWN
        }
    } catch (e: Exception) {
        MessageType.UNKNOWN
    }
}

enum class MessageType {
    DIRECT,    // Прямой экземпляр WebSocketMessage
    WRAPPED,  // Сообщение в обёртке MessageWrapper
    UNKNOWN     // Неизвестный формат
}
// Создаём экземпляр Json с нужными настройками

val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    serializersModule = webSocketSerializersModule
    classDiscriminator = "_type" // или любое другое имя, не совпадающее с полем в классе
}

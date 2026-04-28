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
    abstract val _type: String
}

@Serializable
data class AuthMessage(
    override val _type: String = "auth",
    val action: String, // "login" или "register"
    val login: String,
    val password: String,
    val email: String? = null  // только для регистрации
) : WebSocketMessage()

@Serializable
data class EmailMessage(
    override val _type: String = "email",
    val login: String,
    val email: String
) : WebSocketMessage()

@Serializable
data class ChangePasswordMessage(
    override val _type: String = "change_password",
    val login: String,
    val password: String,
    val code: String
) : WebSocketMessage()

@Serializable
data class TextMessage(
    override val _type: String = "text",
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String? = null
) : WebSocketMessage()

@Serializable
data class CommandMessage(
    override val _type: String = "command",
    val command: String,
    val params: Map<String, String> = emptyMap(),
    val target: String? = null
) : WebSocketMessage()

@Serializable
data class StatusUpdate(
    override val _type: String = "status",
    val status: String,
    val lastActivity: Long = System.currentTimeMillis(),
    val userId: String? = null
) : WebSocketMessage()

@Serializable
data class ServerResponse(
    override val _type: String = "server_response",
    val success: Boolean,
    val message: String,
    val processedAt: Long = System.currentTimeMillis()
) : WebSocketMessage()

@Serializable
data class ServerResponseWithDetails(
    override val _type: String = "server_response_with_details",
    val success: Boolean,
    val details: String,
    val message: String,
    val processedAt: Long = System.currentTimeMillis()
) : WebSocketMessage()

@Serializable
data class ClientResponse(
    override val _type: String = "client_response",
    val success: Boolean,
    val message: String,
    val processedAt: Long = System.currentTimeMillis()
) : WebSocketMessage()

// Модуль сериализаторов
val webSocketSerializersModule = SerializersModule {
    polymorphic(WebSocketMessage::class) {
        subclass(AuthMessage::class)
        subclass(TextMessage::class)
        subclass(CommandMessage::class)
        subclass(StatusUpdate::class)
        subclass(ServerResponse::class)
        subclass(ClientResponse::class)
    }
}

// Определяем тип сообщения по структуре JSON
fun analyzeMessageType(jsonString: String): MessageType {
    return try {
        val jsonElement = Json.parseToJsonElement(jsonString).jsonObject

    if ("type" in jsonElement) {
            // прямой формат
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
    UNKNOWN     // Неизвестный формат
}
// Создаём экземпляр Json с нужными настройками

val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    serializersModule = webSocketSerializersModule
//    classDiscriminator = "_type" // или любое другое имя, не совпадающее с полем в классе
}

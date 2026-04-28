package ru.smi_alexey.email

import ru.smi_alexey.log.log
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/**
 * Отправляет HTML-письмо через SMTP (например, Яндекс).
 * @param toEmail - email получателя
 * @param subject - тема письма
 * @param htmlContent - HTML-текст письма
 * @return true, если отправка успешна, иначе false
 */
fun sendEmail(
    toEmail: String,
    subject: String,
    htmlContent: String
): Boolean {
    // Настройки SMTP (лучше вынести в конфиг или переменные окружения)
    val host = "smtp.yandex.ru"
    val port = "465"
    val username = System.getenv("SMTP_USERNAME") ?: "quiz-smi@yandex.ru"
    val password = System.getenv("SMTP_PASSWORD") ?: "hbxsthbjfbtsmbfl"

    val properties = Properties().apply {
        put("mail.smtp.host", host)
        put("mail.smtp.port", port)
        put("mail.smtp.ssl.enable", "true")
        put("mail.smtp.auth", "true")
        put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
    }

    val session = Session.getInstance(properties, object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(username, password)
        }
    })

    return try {
        val message = MimeMessage(session)
        message.setFrom(InternetAddress(username, "Игра 'Quiz'"))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
        message.subject = subject

        val multipart = MimeMultipart()
        val htmlPart = MimeBodyPart().apply {
            setContent(htmlContent, "text/html; charset=utf-8")
        }
        multipart.addBodyPart(htmlPart)
        // При необходимости добавить вложение – можно расширить
        message.setContent(multipart)

        Transport.send(message)
        log.info("Письмо успешно отправлено на $toEmail")
        true
    } catch (e: MessagingException) {
        log.error("Ошибка при отправке письма на $toEmail", e)
        false
    }
}
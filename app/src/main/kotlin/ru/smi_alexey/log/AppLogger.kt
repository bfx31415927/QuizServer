package ru.smi_alexey.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object log {
    private val logger: Logger = LoggerFactory.getLogger("GlobalAppLogger")

    fun debug(msg: String) = logger.debug(msg)
    fun info(msg: String) = logger.info(msg)
    fun warn(msg: String) = logger.warn(msg)
    fun error(msg: String, throwable: Throwable? = null) =
        if (throwable != null) logger.error(msg, throwable) else logger.error(msg)

    // Для параметризованных сообщений (экономит ресурсы при неактивном уровне логирования)
    fun debug(format: String, vararg args: Any) = logger.debug(format, *args)
    fun info(format: String, vararg args: Any) = logger.info(format, *args)
}
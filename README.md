# QuizServer


This project uses [Gradle](https://gradle.org/).
To build and run the application, use the *Gradle* tool window by clicking the Gradle icon in the right-hand toolbar,
or run it directly from the terminal:

* Run `./gradlew run` to build and run the application.
* Run `./gradlew build` to only build the application.
* Run `./gradlew check` to run all checks, including tests.
* Run `./gradlew clean` to clean all build outputs.

Note the usage of the Gradle Wrapper (`./gradlew`).
This is the suggested way to use Gradle in production projects.

[Learn more about the Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html).

[Learn more about Gradle tasks](https://docs.gradle.org/current/userguide/command_line_interface.html#common_tasks).


This project follows the suggested multi-module setup and consists of the `app` and `utils` subprojects.
The shared build logic was extracted to a convention plugin located in `buildSrc`.

This project uses a version catalog (see `gradle/libs.versions.toml`) to declare and version dependencies
and both a build cache and a configuration cache (see `gradle.properties`).

ПЕРВАЯ ПРОБНАЯ ВЕРСИЯ: (коммит 28.02.26)
----------------------------------------
1) Запускаем программу
   2) На смартфоне запускаем приложение [Mini Postman]
   3.1) Проверка GET:
        Выбираем GET и вводим в поле http://188.243.65:16999 
        или http://188.243.65:16999/ping соответственно
      
        Результат: В IDEA должны увидеть: 
              [eventLoopGroupProxy-4-8] INFO ktor.application - 200 OK: GET - / in <??>ms 
            или
              [eventLoopGroupProxy-4-8] INFO ktor.application - 200 OK: GET - /ping in <??>ms
                   а в Postman должны увидеть: {"text":"Hello from Ktor"} или
                   {"status":"ok", text":"Pong!"} соответственно   
                     
   3.1) Проверка POST:
        Выбираем POST и вводим в поле http://188.243.65:16999/post,
        а в поле [Editable Body (JSON)] вводим {"userInput":"My Text"}
        
       Результат: В IDEA должны увидеть:
       [eventLoopGroupProxy-4-12] INFO ktor.application - 200 OK: POST - /post in <??>ms
       а в Postman должны увидеть: {"processedText":"Received: MY TEXT", "status":"success"}
       

01.03.26
---------
1) Добавил webSocket
см. изменения в libs.versions.toml
ВАЖНО: в коде:
format { call ->
val remoteAddress = call.request.origin.remoteAddress
val method = call.request.httpMethod.value
val uri = call.request.uri
val userAgent = call.request.headers["User-Agent"] ?: "Unknown"
"REMOTE: $remoteAddress | $method $uri | User-Agent: $userAgent"
}
remoteAddress вместо первоначального remoteHost (что вызывала непонятные задержки)
2) На скорую руку был написан файл d:\t\index.html - для соединения и 
посылки команды в WebSocket (файл запускается через браузер)

02.03.26 - 03.03.26
--------------------
1) Удалил GET, POST из Main.kt
2) Изменил libs.versions.toml и build.gradle.kts:
   подключил logback-classic вместо slf4j-simple, чтобы с помощью
   добавленного файла QuizServer\app\src\main\resources\logback.xml
   можно было настроить новое логирование,
   конкретно в моем случае - добавился вывод даты/времени в начале логируемых строк

01.03.26
---------
Если раскомментировать 3 куска":
//    val StartTime = AttributeKey<Long>("StartTime")

//        intercept(ApplicationCallPipeline.Monitoring) {
//            call.attributes.put(StartTime, System.currentTimeMillis())
//        }
и
//            format { call ->
//                val method = call.request.httpMethod.value
//                val uri = call.request.uri
//                val status = call.response.status()?.value ?: "Unknown"
//                val start = call.attributes.getOrNull(StartTime) ?: System.currentTimeMillis()
//                val elapsed = System.currentTimeMillis() - start
//
//                "[HANDSHAKE_LOG] $method $uri → $status in ${elapsed}ms"
//            }
то выполнив usecase:
- Запустите сервер
- Подключитесь клиентом к /ws
- Дождитесь соединения
- Отключите клиента
    vожно убедиться, что строка лога с "[HANDSHAKE_LOG] GET /ws" появится
    с запозданием после строки "WebSocket disconnected"

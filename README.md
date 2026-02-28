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
       


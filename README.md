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

06.03.26 - 07.03.26
-------------------
1) Установил в винду PostgreSQL 17.0 в папку "c:\Program Files\PostgreSQL\17"
   Для просиотра БД используется pgAdmin 4 (ver. 8.12) - иконка со слоником на панели задач
2) Помимо дефолтного пользователя postgres создал нового пользователя quiz_user с паролем 31415926.
   Чтобы увидеть всех пользователей postgreSQL можно выполнить:
      SELECT rolname AS username,
         rolsuper AS is_superuser,
         rolcreatedb AS can_create_db,
         rolcreaterole AS can_create_role,
         rolcanlogin AS can_login
      FROM pg_roles
      WHERE rolcanlogin;
3) Создал с помощью pgAdmin свою БД "quiz_db" и добавил возможность миграций схемы БД:
      3.1) Создал в файловой системе проекта файл
           "QuizServer\app\src\main\resources\db\migration\V1__First_migration.sql",
           в котором прописал запросы для создания начальной таблицы и индексов для нее.
           - Практически обязательно добавлять к создаваемым объектам внутри файла "IF NOT EXISTS"
           - Важно правильно именовать файлы миграций. Если предполагается, что их будет много, 
             то лучше именовать примерно так: V001__First_migration.sql, V002__Second_migration.sql и т.д.,
             потому что миграции будут применяться в алфавитном порядке.
      3.2) После создания первой миграции в БД создается (в рубрике [Tables]) объект "flyway_schema_history",
           в котором хранится информация о примененных миграциях.
      3.3) Подкорректировал файлы libs.versions.toml и build.gradle.kts(:app), чтобы добавить библиотеки flyway.
           Потратил массу времени, пока разобрался, что ПОМИМО ПРОЧЕГО обязательно надо прописать в libs.versions.toml
           строки:
             flyway-database-postgresql = { module = "org.flywaydb:flyway-database-postgresql", version.ref = "flyway" }
                     и
             "flyway-database-postgresql", потому что
            (Начиная с Flyway 10.x, команда Flyway объявила о том, что:
              Поддержка отдельных баз данных (включая PostgreSQL) вынесена из flyway-core и
              теперь нужно добавлять отдельную зависимость)
             
      3.4) Добавил в Main.kt запуск миграций БД.  
4) Изменил в файле build.gradle.kts(:app) строку:
   mainClass.set("ru.smi_alexey.quizserver.app.MainKt")
   позволяющую корректно запускать приложение из терминала

08.03.26
--------
1) Подкорректировал libs.versions.toml и build.gradle.kts
   в плане библиотек slf4j и logback
   1.1) Реализовал глобальный логгер log для любых логирований,
   1.2) Отредактировал файл logback.xml
   1.3) Поменял все вызовы логирования на log
2) Добавил файл V2__Second_migration.sql для проверки миграций
3) Убрал лишнюю строку в файле V1__First_migration.sql
4) Добавил несколько файлов, разгрузив Main.kt:
   - migrations.kt, transactions.kt(package:ru.smi_alexey.db)
   - Applogger.kt(package:ru.smi_alexey.log)
   - embedded.kt(package:ru.smi_alexey.server)


11.03.26
--------
1) Практически подготовил embedded.kt для приема/отправки сообщений клиенту
   через json со свец. форматами
2) 	 Добавил в установки для Json строку:
     classDiscriminator = "_type" // или любое другое имя, не совпадающее с полем в классе
		 Без этой строки при запуске программы выдавалась ошибка.
		 Выяснилось, что Kotlinx.Serialization по умолчанию тоже использует
		 поле type как дискриминатор классов (class discriminator) для полиморфной сериализации.

12.03.26 (16:40)
------------------
Подправил логирование сообщений, включил парсинг приходящих от клиентов сообщений,
проверил приход от клиента трех сериализованных объектов классов в обертке

13.03.2026 (13:42) (*)
----------------------
1) Поменял в иерархии sealed-классов type на _type
   и закомментировал classDiscriminator = "_type" в настройках Json
   (Пришлось так сделать, т.к. по дефолту при сериализации передается
   спец. поле type)
2) Проверил прием сообщений от клиента на сервер и отправку от него ответа для всех подготовленных
   клиентом вариантов (для трех классов в обертке и для тех же классов без обертки)
3) Проставил после (13:42) - (*), чтобы по этому же маркеру на коммите клиента
   судить о стыковке двух версий программ.

13.03.2026 (23:00) (**)
----------------------
1) Подрафинировал код
2) Проставил после (23:00) - (**), чтобы по этому же маркеру на коммите клиента
   судить о стыковке двух версий программ.

14.03.2026 (13:36) (***)
----------------------
Подготовил (вместе с сервером) проверку:
1) Проследите, что изначально у клиента закомментированы все команды sendWrapperMessage и
   sendDirectMessage, расположенные сразу после override fun onOpen.
2) Проследите, что изначально у сервера закомментированы все команды sendWrapperMessage и
   sendDirectMessage, расположенные сразу после log.info("WebSocket connected: $clientAddress").
3) Раскомментируем первую из упомянутых команд на клиенте.
4) Запускаем сервер, затем запускаем клиента.
5) Проверяем, что раскомментированная команда выполнилась правильно:
   смотрим логи на клиенте и на сервере. Завершаем клиента и сервер.
6) Раскомментируем следующую из упомянутых команд на клиенте, комментируем предыдущую.
7) Выполняем пункты 4 - 5.
8) Выполняем пункты  6 - 7 до исчерпания всех команд клиента.
9) Убеждаемся, что все упомянутые команды клиента закомментированы.
10) Раскомментируем первую из упомянутых команд на сервере.
11) Выполняем пункты 4 - 5
12) Раскомментируем следующую из упомянутых команд на сервере, комментируем предыдущую.
13) Выполняем пункт 7, пункт 12 и так до перебора всех команд на сервере
14) Убеждаемся, что все упомянутые команды сервера закомментированы.
15) Проставил после (13:36) - (***), чтобы по этому же маркеру на коммите сервера
    судить о стыковке двух версий программ.
16) Сделал ветку этой версии клиента - QuizAdmin_MessagesTest,
    Сделал ветку этой версии сервера - QuizServer_MessagesTest
		

14.03.2026 (19:38) (****) [BRANCH: QuizSever_MessagesTest]
-------------------------
Подправил код так, чтобы после запуска сервера и потом клиента 
последовательность действий была такая:
CLIENT(отправил):  	sendWrapperMessage(this@MyWebSocket,TextMessage(content = "Привет от Android-клиента!",userId = "1"))
SERVER(получил):  	TextMessage(content = "Привет от Android-клиента!",userId = "1")
SERVER(отправил):  	sendWrapperMessage(session,TextMessage(content = "userId = 2", userId = "2"))
CLIENT(получил):  	TextMessage(content = "userId = 2", userId = "2")
CLIENT(отправил):  	sendWrapperMessage(myWebSocket,CommandMessage(command = "start_game",params = mapOf("round" to "1"),target = "all"))
SERVER(получил):		CommandMessage(command = "start_game",params = mapOf("round" to "1"),target = "all")
SERVER(отправил):		sendWrapperMessage(session, StatusUpdate( status = "status", userId = "2"))
CLIENT(получил):		StatusUpdate( status = "status", userId = "2")
CLIENT(отправил):   sendDirectMessage(myWebSocket,TextMessage(content = "userId = 3", userId = "3"))
SERVER(получил):		TextMessage(content = "userId = 3", userId = "3")
SERVER(отправил):		sendDirectMessage(session,CommandMessage(command = "stop_game",params = mapOf("round" to "2"),target = "not_all"))
CLIENT(получил):		CommandMessage(command = "stop_game",params = mapOf("round" to "2"),target = "not_all")
CLIENT(отправил):		sendDirectMessage(myWebSocket, StatusUpdate(status = "status3", userId = "3"))
SERVER(получил):		StatusUpdate(status = "status3", userId = "3")
SERVER(отправил):		sendDirectMessage(session,ServerResponse(success = true, message = mess))
CLIENT(получил):		Log.d("MyWebSocket", "Получено сообщение ServerResponse: $message")

15.03.2026 (13:04) (****) [BRANCH: QuizServer_MessagesTest]
-------------------------	
1) Вывел уникальные номер сессии для каждого подключенного клиента и его deviceID

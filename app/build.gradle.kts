plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":utils"))

    implementation(libs.bundles.ktorServer)
    implementation(libs.kotlinxSerialization)

    // Зависимости для PostgreSQL (Exposed + драйвер + пул соединений)
    implementation(libs.bundles.database)

    // Логирование через Logback
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.flyway.core)

    // Добавленные зависимости для отправки email
    implementation(libs.javax.mail)
    implementation(libs.javax.activation)

    // Тестовые зависимости
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("ru.smi_alexey.quizserver.app.MainKt")
}
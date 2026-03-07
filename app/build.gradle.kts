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
    implementation(libs.logback.classic)
    implementation(libs.flyway.core)
}

application {
    mainClass.set("ru.smi_alexey.quizserver.app.MainKt")
}

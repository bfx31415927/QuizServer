plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":utils"))

    implementation(libs.bundles.ktorServer)
    implementation(libs.kotlinxSerialization)

    // Удаляем slf4j-simple — он блокирует logback
    // implementation(libs.slf4j.simple)

    // Подключаем logback-classic через libs.versions.toml
    implementation(libs.logback.classic)
}

application {
    mainClass.set("org.example.app.MainKt")
}
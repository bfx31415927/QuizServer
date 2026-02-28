plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":utils"))

    implementation(libs.bundles.ktorServer)
    implementation(libs.kotlinxSerialization)
    implementation(libs.slf4j.simple)
}

application {
    mainClass.set("org.example.app.AppKt")
}
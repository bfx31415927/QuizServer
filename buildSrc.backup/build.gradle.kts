plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.20")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.2.20")
}

kotlin {
    jvmToolchain(21) // Укажите версию Java, если нужно
}

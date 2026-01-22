plugins {
    val kotlinVersion = "2.0.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"
}

group = "top.limbang"
version = "0.0.4"


repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    compileOnly(files("debug-sandbox/plugins/mirai-console-mcsm-plugin-1.2.0.mirai2.jar"))

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.10.0")
}
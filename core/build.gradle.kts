plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    id("io.ktor.plugin") version "3.6.1"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

group = "ink.knowledge"
version = "0.1.0"

repositories { mavenCentral() }

kotlin { jvmToolchain(21) }

application { mainClass.set("ink.knowledge.ApplicationKt") }

dependencies {
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-di")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("ch.qos.logback:logback-classic:1.6.8")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation(kotlin("test"))
}

val clientModels = layout.projectDirectory.dir("../client/tablet/api")

tasks.register<JavaExec>("generateClientModels") {
    group = "build"
    description = "Regenerates the tablet's C++ API models from the response serializers."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("ink.knowledge.codegen.CppModelGeneratorKt")
    args(clientModels.asFile.path)
}

tasks.test { systemProperty("clientModels", clientModels.asFile.path) }

package ink.knowledge.configuration.core

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

val ApiJson =
    Json {
        classDiscriminator = "kind"
        encodeDefaults = true
    }

fun Application.configureSerialization() {
    install(ContentNegotiation) { json(ApiJson) }
}

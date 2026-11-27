package ink.knowledge

import ink.knowledge.configuration.configureDependencies
import ink.knowledge.configuration.configureRouting
import ink.knowledge.configuration.configureSerialization
import ink.knowledge.exceptions.configureExceptionHandling
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) = EngineMain.main(args)

suspend fun Application.module() {
    configureDependencies()
    configureSerialization()
    configureExceptionHandling()
    configureRouting()
}

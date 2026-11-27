package ink.knowledge

import ink.knowledge.configuration.configureSerialization
import ink.knowledge.configuration.properties.VaultProperties
import ink.knowledge.controller.workspace.workspaceController
import ink.knowledge.exceptions.configureExceptionHandling
import ink.knowledge.service.vault.FileService
import ink.knowledge.service.workspace.WorkspaceService
import io.ktor.server.application.Application
import io.ktor.server.config.property
import io.ktor.server.netty.EngineMain
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val fileService = FileService(property<VaultProperties>("vault"))
    val workspaceService = WorkspaceService(fileService)

    configureSerialization()
    configureExceptionHandling()

    routing {
        get("/health") { call.respondText("ok") }
        workspaceController(workspaceService, fileService)
    }
}

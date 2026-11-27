package ink.knowledge

import ink.knowledge.configuration.configureSerialization
import ink.knowledge.configuration.properties.VaultProperties
import ink.knowledge.controller.workspace.workspaceController
import ink.knowledge.exceptions.configureExceptionHandling
import ink.knowledge.service.vault.FileService
import ink.knowledge.service.workspace.WorkspaceService
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.config.property
import io.ktor.server.netty.EngineMain
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlin.time.TimeSource

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val fileService = FileService(property<VaultProperties>("vault"))
    val workspaceService = WorkspaceService(fileService)
    val started = TimeSource.Monotonic.markNow()
    val index = fileService.index()
    val workspace = workspaceService.load(index)
    log.info("Indexed vault: ${index.files.size} files, ${index.folders.size} folders in ${started.elapsedNow().inWholeMilliseconds} ms")

    configureSerialization()
    configureExceptionHandling()

    routing {
        get("/health") { call.respondText("ok") }
        workspaceController(index, workspace, fileService)
    }
}

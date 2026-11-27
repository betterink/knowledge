package ink.knowledge.configuration

import ink.knowledge.controller.workspace.workspaceController
import ink.knowledge.models.vault.VaultIndex
import ink.knowledge.models.workspace.Workspace
import ink.knowledge.service.vault.FileService
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

suspend fun Application.configureRouting() {
    val index = dependencies.resolve<VaultIndex>()
    val workspace = dependencies.resolve<Workspace>()
    val fileService = dependencies.resolve<FileService>()

    routing {
        get("/health") { call.respondText("ok") }
        workspaceController(index, workspace, fileService)
    }
}

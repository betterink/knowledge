package ink.knowledge.controller.workspace

import ink.knowledge.enums.vault.FileFormat
import ink.knowledge.service.vault.FileService
import ink.knowledge.service.workspace.WorkspaceService
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.workspaceController(
    workspaceService: WorkspaceService,
    fileService: FileService,
) {
    route("/v1") {
        get("/workspace") {
            call.respond(workspaceService.load())
        }

        get("/files/{path...}") {
            val path =
                call.parameters
                    .getAll("path")
                    .orEmpty()
                    .joinToString("/")
            val version = fileService.version(path)
            val format = FileFormat.of(path.substringAfterLast('/'))
            call.response.header(HttpHeaders.ETag, "\"${version.hash}\"")
            call.respondBytes(fileService.read(path), ContentType.parse(format.mimeType))
        }
    }
}

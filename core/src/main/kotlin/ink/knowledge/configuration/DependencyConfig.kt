package ink.knowledge.configuration

import ink.knowledge.configuration.properties.VaultProperties
import ink.knowledge.models.vault.VaultIndex
import ink.knowledge.models.workspace.Workspace
import ink.knowledge.service.vault.FileService
import ink.knowledge.service.workspace.WorkspaceService
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.config.property
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.provide
import io.ktor.server.plugins.di.resolve
import kotlin.time.TimeSource

fun Application.configureDependencies() {
    val log = log
    val vault = property<VaultProperties>("vault")
    dependencies {
        provide<VaultProperties> { vault }
        provide(::FileService)
        provide(::WorkspaceService)
        provide<VaultIndex> {
            val started = TimeSource.Monotonic.markNow()
            val index = resolve<FileService>().index()
            log.info(
                "Indexed vault: ${index.files.size} files, ${index.folders.size} folders in ${started.elapsedNow().inWholeMilliseconds} ms",
            )
            index
        }
        provide<Workspace> { resolve<WorkspaceService>().load(resolve()) }
    }
}

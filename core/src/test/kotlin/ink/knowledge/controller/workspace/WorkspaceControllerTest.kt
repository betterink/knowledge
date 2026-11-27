package ink.knowledge.controller.workspace

import ink.knowledge.util.factory.VaultFactory
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkspaceControllerTest {
    private fun withVault(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            environment {
                val vault = MapApplicationConfig("vault.root" to VaultFactory.create().toString())
                config = ApplicationConfig("application.conf").mergeWith(vault)
            }
            block()
        }

    @Test
    fun workspaceReturnsTree() =
        withVault {
            val response = client.get("/v1/workspace")
            assertEquals(HttpStatusCode.OK, response.status)
            val root = Json.parseToJsonElement(response.bodyAsText()).jsonObject["root"]!!.jsonObject
            val first = root["children"]!!.jsonArray.first().jsonObject
            assertEquals("folder", first["kind"]!!.jsonPrimitive.content)
            assertEquals("FOLDER", first["format"]!!.jsonPrimitive.content)
        }

    @Test
    fun manifestReturnsIndex() =
        withVault {
            val response = client.get("/v1/manifest")
            assertEquals(HttpStatusCode.OK, response.status)
            val index = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val same = index["files"]!!.jsonObject["A/Same.md"]!!.jsonObject
            assertEquals("ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb", same["hash"]!!.jsonPrimitive.content)
            assertTrue("Empty" in index["folders"]!!.jsonArray.map { it.jsonPrimitive.content })
        }

    @Test
    fun fileReturnsContentWithHashEtag() =
        withVault {
            val response = client.get("/v1/files/A/Same.md")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("a", response.bodyAsText())
            assertEquals("\"ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb\"", response.headers[HttpHeaders.ETag])
            assertTrue(response.headers[HttpHeaders.ContentType]!!.startsWith("text/markdown"))
        }

    @Test
    fun missingFileIs404() =
        withVault {
            assertEquals(HttpStatusCode.NotFound, client.get("/v1/files/Nope.md").status)
        }

    @Test
    fun excludedPathIs400() =
        withVault {
            assertEquals(HttpStatusCode.BadRequest, client.get("/v1/files/.obsidian/app.json").status)
        }
}

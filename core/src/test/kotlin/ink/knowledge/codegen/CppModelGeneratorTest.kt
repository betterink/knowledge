package ink.knowledge.codegen

import ink.knowledge.configuration.core.ApiJson
import ink.knowledge.models.vault.VaultIndex
import ink.knowledge.models.workspace.Workspace
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class CppModelGeneratorTest {
    private val directory = Path.of(System.getProperty("clientModels"))

    @Test
    fun checkedInModelsMatchSerializers() {
        CppModelGenerator(clientRoots).files().forEach { (name, expected) ->
            val actual = directory.resolve(name).takeIf(Files::exists)?.let(Files::readString)
            assertEquals(expected, actual, "$name is stale. Run: cd core && ./gradlew generateClientModels")
        }
    }

    @Test
    fun clientFixturesDecodeWithServerSerializers() {
        ApiJson.decodeFromString<Workspace>(Files.readString(directory.resolve("tests/workspace.json")))
        ApiJson.decodeFromString<VaultIndex>(Files.readString(directory.resolve("tests/manifest.json")))
    }
}

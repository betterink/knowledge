package ink.knowledge.codegen

import ink.knowledge.configuration.core.ApiJson
import ink.knowledge.models.vault.VaultIndex
import ink.knowledge.models.workspace.Workspace
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.nonNullOriginal
import java.nio.file.Files
import java.nio.file.Path

/** Response bodies the tablet parses. Add a root when a route returns a new type. */
val clientRoots: List<KSerializer<*>> = listOf(Workspace.serializer(), VaultIndex.serializer())

const val HEADER = "ApiModels.h"
const val SOURCE = "ApiModels.cpp"

/** Writes the generated C++ models into the directory given as the only argument. */
fun main(args: Array<String>) {
    val directory = Path.of(args.single())
    CppModelGenerator(clientRoots).files().forEach { (name, text) -> Files.writeString(directory.resolve(name), text) }
}

@OptIn(ExperimentalSerializationApi::class)
class CppModelGenerator(
    roots: List<KSerializer<*>>,
) {
    private val discriminator = ApiJson.configuration.classDiscriminator
    private val types = LinkedHashMap<String, SerialDescriptor>()
    private val rootNames = roots.map { typeName(it.descriptor) }

    init {
        roots.forEach { collect(it.descriptor) }
    }

    fun files(): Map<String, String> = mapOf(HEADER to header(), SOURCE to source())

    private fun collect(descriptor: SerialDescriptor) {
        val type = descriptor.nonNullOriginal
        when (type.kind) {
            is PrimitiveKind -> return
            StructureKind.LIST, StructureKind.MAP -> type.elementDescriptors.forEach(::collect)
            StructureKind.CLASS, SerialKind.ENUM, PolymorphicKind.SEALED -> {
                val name = typeName(type)
                val known = types[name]
                if (known != null) {
                    check(known.serialName == type.serialName) { "Two types map to C++ name $name" }
                    return
                }
                types[name] = type
                when (type.kind) {
                    StructureKind.CLASS -> type.elementDescriptors.forEach(::collect)
                    PolymorphicKind.SEALED -> subtypes(type).forEach(::collect)
                    else -> Unit
                }
            }
            else -> error("Unsupported serial kind ${type.kind} in ${type.serialName}")
        }
    }

    private fun typeName(descriptor: SerialDescriptor) =
        descriptor.nonNullOriginal.serialName
            .substringAfterLast('.')
            .replaceFirstChar { it.uppercase() }

    private fun subtypes(sealed: SerialDescriptor) = sealed.getElementDescriptor(1).elementDescriptors.toList()

    private fun cppType(descriptor: SerialDescriptor): String {
        val type = descriptor.nonNullOriginal
        val base =
            when (type.kind) {
                PrimitiveKind.STRING -> "QString"
                PrimitiveKind.INT -> "int"
                PrimitiveKind.LONG -> "qint64"
                PrimitiveKind.BOOLEAN -> "bool"
                PrimitiveKind.DOUBLE, PrimitiveKind.FLOAT -> "double"
                StructureKind.LIST -> "std::vector<${cppType(type.getElementDescriptor(0))}>"
                StructureKind.MAP -> {
                    check(type.getElementDescriptor(0).kind == PrimitiveKind.STRING) { "Map keys must be strings in ${type.serialName}" }
                    "std::map<QString, ${cppType(type.getElementDescriptor(1))}>"
                }
                StructureKind.CLASS, SerialKind.ENUM, PolymorphicKind.SEALED -> typeName(type)
                else -> error("Unsupported serial kind ${type.kind} in ${type.serialName}")
            }
        return if (descriptor.isNullable) "std::optional<$base>" else base
    }

    /** Types a declaration needs complete: direct and optional members, not containers. */
    private fun completeDependencies(descriptor: SerialDescriptor): List<String> {
        val type = descriptor.nonNullOriginal
        return when (type.kind) {
            StructureKind.CLASS -> if (typeName(type) in types) listOf(typeName(type)) else emptyList()
            PolymorphicKind.SEALED -> subtypes(type).map(::typeName)
            else -> emptyList()
        }
    }

    private fun structOrder(): List<SerialDescriptor> {
        val ordered = LinkedHashMap<String, SerialDescriptor>()
        val visiting = mutableSetOf<String>()

        fun visit(type: SerialDescriptor) {
            val name = typeName(type)
            if (name in ordered) return
            check(visiting.add(name)) { "$name contains itself without a list or map" }
            type.elementDescriptors
                .flatMap(::completeDependencies)
                .forEach { visit(types.getValue(it)) }
            ordered[name] = type
        }
        types.values.filter { it.kind == StructureKind.CLASS }.forEach(::visit)
        return ordered.values.toList()
    }

    private fun header() =
        buildString {
            appendLine("// Generated from core's serializers by `./gradlew generateClientModels`. Do not edit.")
            appendLine("#pragma once")
            appendLine("#include <QtCore/QByteArray>")
            appendLine("#include <QtCore/QString>")
            appendLine("#include <map>")
            appendLine("#include <optional>")
            appendLine("#include <variant>")
            appendLine("#include <vector>")
            appendLine()
            appendLine("namespace api {")
            types.values.filter { it.kind == SerialKind.ENUM }.forEach { enum ->
                appendLine("enum class ${typeName(enum)} { ${(0 until enum.elementsCount).joinToString { enum.getElementName(it) }} };")
            }
            types.values.filter { it.kind == StructureKind.CLASS }.forEach { appendLine("struct ${typeName(it)};") }
            types.values.filter { it.kind == PolymorphicKind.SEALED }.forEach { sealed ->
                appendLine("// Selected by the \"$discriminator\" field.")
                appendLine("using ${typeName(sealed)} = std::variant<${subtypes(sealed).joinToString { typeName(it) }}>;")
            }
            structOrder().forEach { struct ->
                appendLine()
                appendLine("struct ${typeName(struct)} {")
                for (i in 0 until struct.elementsCount) {
                    appendLine("    ${cppType(struct.getElementDescriptor(i))} ${struct.getElementName(i)}{};")
                }
                appendLine("};")
            }
            appendLine()
            appendLine("// Returns nullopt on invalid JSON and sets error to \"<path>: <reason>\".")
            appendLine("template <typename T> std::optional<T> parse(const QByteArray &json, QString *error = nullptr);")
            rootNames.forEach { appendLine("extern template std::optional<$it> parse<$it>(const QByteArray &, QString *);") }
            appendLine("}")
        }

    private fun source() =
        buildString {
            appendLine("// Generated from core's serializers by `./gradlew generateClientModels`. Do not edit.")
            appendLine("#include \"$HEADER\"")
            append(PRELUDE)
            appendLine()
            types.keys.forEach { appendLine("bool read(Reader &, const QJsonValue &, $it &);") }
            append(CONTAINERS)
            types.values.forEach { type ->
                appendLine()
                when (type.kind) {
                    SerialKind.ENUM -> enumReader(type)
                    PolymorphicKind.SEALED -> sealedReader(type)
                    else -> structReader(type)
                }
            }
            appendLine("} // namespace")
            append(ENTRY)
            rootNames.forEach { appendLine("template std::optional<$it> parse<$it>(const QByteArray &, QString *);") }
            appendLine("}")
        }

    private fun StringBuilder.enumReader(enum: SerialDescriptor) {
        val name = typeName(enum)
        appendLine("bool read(Reader &r, const QJsonValue &json, $name &out) {")
        appendLine("    static const std::map<QString, $name> values{")
        for (i in 0 until enum.elementsCount) {
            val value = enum.getElementName(i)
            appendLine("        {\"$value\", $name::$value},")
        }
        appendLine("    };")
        appendLine("    return lookup(r, json, values, out);")
        appendLine("}")
    }

    private fun StringBuilder.sealedReader(sealed: SerialDescriptor) {
        val name = typeName(sealed)
        appendLine("bool read(Reader &r, const QJsonValue &json, $name &out) {")
        appendLine("    using Read = bool (*)(Reader &, const QJsonValue &, $name &);")
        appendLine("    static const std::map<QString, Read> kinds{")
        subtypes(sealed).forEach {
            appendLine(
                "        {\"${it.serialName}\", [](Reader &r, const QJsonValue &json, $name &out) { return readAs<${typeName(
                    it,
                )}>(r, json, out); }},",
            )
        }
        appendLine("    };")
        appendLine("    if (!json.isObject()) return r.fail(\"expected object\");")
        appendLine("    Read reader = nullptr;")
        appendLine("    r.path.append(\".$discriminator\");")
        appendLine("    if (!lookup(r, json.toObject().value(\"$discriminator\"), kinds, reader)) return false;")
        appendLine("    r.path.removeLast();")
        appendLine("    return reader(r, json, out);")
        appendLine("}")
    }

    private fun StringBuilder.structReader(struct: SerialDescriptor) {
        appendLine("bool read(Reader &r, const QJsonValue &json, ${typeName(struct)} &out) {")
        appendLine("    if (!json.isObject()) return r.fail(\"expected object\");")
        appendLine("    const QJsonObject object = json.toObject();")
        val fields =
            (0 until struct.elementsCount).map {
                "field(r, object, \"${struct.getElementName(
                    it,
                )}\", out.${struct.getElementName(it)})"
            }
        appendLine("    return ${fields.joinToString("\n        && ").ifEmpty { "true" }};")
        appendLine("}")
    }
}

private val PRELUDE =
    """
    #include <QtCore/QJsonArray>
    #include <QtCore/QJsonDocument>
    #include <QtCore/QJsonObject>
    #include <QtCore/QJsonValue>
    #include <QtCore/QStringList>

    namespace api {
    namespace {
    struct Reader {
        QStringList path;
        QString error;
        bool fail(const QString &reason) {
            const QString at = path.join("");
            error = (at.startsWith('.') ? at.mid(1) : at.isEmpty() ? "$" : at) + ": " + reason;
            return false;
        }
    };

    bool read(Reader &r, const QJsonValue &json, QString &out) {
        if (!json.isString()) return r.fail("expected string");
        out = json.toString();
        return true;
    }
    bool read(Reader &r, const QJsonValue &json, bool &out) {
        if (!json.isBool()) return r.fail("expected boolean");
        out = json.toBool();
        return true;
    }
    bool read(Reader &r, const QJsonValue &json, double &out) {
        if (!json.isDouble()) return r.fail("expected number");
        out = json.toDouble();
        return true;
    }
    bool read(Reader &r, const QJsonValue &json, qint64 &out) {
        if (!json.isDouble() || json.toDouble() != double(json.toInteger())) return r.fail("expected integer");
        out = json.toInteger();
        return true;
    }
    bool read(Reader &r, const QJsonValue &json, int &out) {
        qint64 value = 0;
        if (!read(r, json, value)) return false;
        if (value != int(value)) return r.fail("integer out of range");
        out = int(value);
        return true;
    }
    template <typename T>
    bool lookup(Reader &r, const QJsonValue &json, const std::map<QString, T> &values, T &out) {
        const auto found = values.find(json.toString());
        if (!json.isString()) return r.fail("expected string");
        if (found == values.end()) return r.fail("unknown value \"" + json.toString() + "\"");
        out = found->second;
        return true;
    }
    """.trimIndent() + "\n"

private val CONTAINERS =
    """
    template <typename T> struct IsOptional : std::false_type {};
    template <typename T> struct IsOptional<std::optional<T>> : std::true_type {};

    template <typename T> bool read(Reader &r, const QJsonValue &json, std::optional<T> &out) {
        if (json.isNull() || json.isUndefined()) { out.reset(); return true; }
        out.emplace();
        return read(r, json, *out);
    }
    template <typename T> bool read(Reader &r, const QJsonValue &json, std::vector<T> &out) {
        if (!json.isArray()) return r.fail("expected array");
        const QJsonArray array = json.toArray();
        out.resize(array.size());
        for (qsizetype i = 0; i < array.size(); ++i) {
            r.path.append(QString("[%1]").arg(i));
            if (!read(r, array[i], out[i])) return false;
            r.path.removeLast();
        }
        return true;
    }
    template <typename T> bool read(Reader &r, const QJsonValue &json, std::map<QString, T> &out) {
        if (!json.isObject()) return r.fail("expected object");
        const QJsonObject object = json.toObject();
        for (auto it = object.begin(); it != object.end(); ++it) {
            r.path.append("[\"" + it.key() + "\"]");
            if (!read(r, it.value(), out[it.key()])) return false;
            r.path.removeLast();
        }
        return true;
    }
    template <typename T> bool field(Reader &r, const QJsonObject &object, const char *name, T &out) {
        r.path.append(QString(".") + name);
        if (!object.contains(QLatin1String(name)) && !IsOptional<T>::value) return r.fail("missing");
        if (!read(r, object.value(QLatin1String(name)), out)) return false;
        r.path.removeLast();
        return true;
    }
    template <typename Subtype, typename Sealed> bool readAs(Reader &r, const QJsonValue &json, Sealed &out) {
        Subtype value;
        if (!read(r, json, value)) return false;
        out = std::move(value);
        return true;
    }
    """.trimIndent() + "\n"

private val ENTRY =
    """

    template <typename T> std::optional<T> parse(const QByteArray &json, QString *error) {
        QJsonParseError syntax;
        const QJsonDocument document = QJsonDocument::fromJson(json, &syntax);
        Reader reader;
        T value;
        if (syntax.error != QJsonParseError::NoError) reader.fail(syntax.errorString());
        else if (read(reader, document.isArray() ? QJsonValue(document.array()) : QJsonValue(document.object()), value))
            return value;
        if (error) *error = reader.error;
        return std::nullopt;
    }
    """.trimIndent() + "\n"

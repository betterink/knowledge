// Generated from core's serializers by `./gradlew generateClientModels`. Do not edit.
#include "ApiModels.h"
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

bool read(Reader &, const QJsonValue &, Workspace &);
bool read(Reader &, const QJsonValue &, Folder &);
bool read(Reader &, const QJsonValue &, Node &);
bool read(Reader &, const QJsonValue &, Attachment &);
bool read(Reader &, const QJsonValue &, FileFormat &);
bool read(Reader &, const QJsonValue &, Version &);
bool read(Reader &, const QJsonValue &, Base &);
bool read(Reader &, const QJsonValue &, Diagram &);
bool read(Reader &, const QJsonValue &, Link &);
bool read(Reader &, const QJsonValue &, LinkKind &);
bool read(Reader &, const QJsonValue &, Page &);
bool read(Reader &, const QJsonValue &, PageReference &);
bool read(Reader &, const QJsonValue &, VaultIndex &);
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

bool read(Reader &r, const QJsonValue &json, Workspace &out) {
    if (!json.isObject()) return r.fail("expected object");
    const QJsonObject object = json.toObject();
    return field(r, object, "name", out.name)
        && field(r, object, "root", out.root);
}

bool read(Reader &r, const QJsonValue &json, Folder &out) {
    if (!json.isObject()) return r.fail("expected object");
    const QJsonObject object = json.toObject();
    return field(r, object, "path", out.path)
        && field(r, object, "name", out.name)
        && field(r, object, "parent", out.parent)
        && field(r, object, "children", out.children)
        && field(r, object, "folderNote", out.folderNote)
        && field(r, object, "format", out.format);
}

bool read(Reader &r, const QJsonValue &json, Node &out) {
    using Read = bool (*)(Reader &, const QJsonValue &, Node &);
    static const std::map<QString, Read> kinds{
        {"attachment", [](Reader &r, const QJsonValue &json, Node &out) { return readAs<Attachment>(r, json, out); }},
        {"base", [](Reader &r, const QJsonValue &json, Node &out) { return readAs<Base>(r, json, out); }},
        {"diagram", [](Reader &r, const QJsonValue &json, Node &out) { return readAs<Diagram>(r, json, out); }},
        {"folder", [](Reader &r, const QJsonValue &json, Node &out) { return readAs<Folder>(r, json, out); }},
        {"page", [](Reader &r, const QJsonValue &json, Node &out) { return readAs<Page>(r, json, out); }},
    };
    if (!json.isObject()) return r.fail("expected object");
    Read reader = nullptr;
    r.path.append(".kind");
    if (!lookup(r, json.toObject().value("kind"), kinds, reader)) return false;
    r.path.removeLast();
    return reader(r, json, out);
}

bool read(Reader &r, const QJsonValue &json, Attachment &out) {
    if (!json.isObject()) return r.fail("expected object");
    const QJsonObject object = json.toObject();
    return field(r, object, "path", out.path)
        && field(r, object, "name", out.name)
        && field(r, object, "parent", out.parent)
        && field(r, object, "format", out.format)
        && field(r, object, "version", out.version);
}

bool read(Reader &r, const QJsonValue &json, FileFormat &out) {
    static const std::map<QString, FileFormat> values{
        {"FOLDER", FileFormat::FOLDER},
        {"MARKDOWN", FileFormat::MARKDOWN},
        {"EXCALIDRAW", FileFormat::EXCALIDRAW},
        {"CANVAS", FileFormat::CANVAS},
        {"BASE", FileFormat::BASE},
        {"PDF", FileFormat::PDF},
        {"IMAGE_JPG", FileFormat::IMAGE_JPG},
        {"IMAGE_PNG", FileFormat::IMAGE_PNG},
        {"VIDEO_MP4", FileFormat::VIDEO_MP4},
        {"VIDEO_MOV", FileFormat::VIDEO_MOV},
        {"UNKNOWN", FileFormat::UNKNOWN},
    };
    return lookup(r, json, values, out);
}

bool read(Reader &r, const QJsonValue &json, Version &out) {
    if (!json.isObject()) return r.fail("expected object");
    const QJsonObject object = json.toObject();
    return field(r, object, "hash", out.hash)
        && field(r, object, "size", out.size)
        && field(r, object, "modified", out.modified);
}

bool read(Reader &r, const QJsonValue &json, Base &out) {
    if (!json.isObject()) return r.fail("expected object");
    const QJsonObject object = json.toObject();
    return field(r, object, "path", out.path)
        && field(r, object, "name", out.name)
        && field(r, object, "parent", out.parent)
        && field(r, object, "version", out.version)
        && field(r, object, "format", out.format);
}

bool read(Reader &r, const QJsonValue &json, Diagram &out) {
    if (!json.isObject()) return r.fail("expected object");
    const QJsonObject object = json.toObject();
    return field(r, object, "path", out.path)
        && field(r, object, "name", out.name)
        && field(r, object, "parent", out.parent)
        && field(r, object, "version", out.version)
        && field(r, object, "links", out.links)
        && field(r, object, "format", out.format);
}

bool read(Reader &r, const QJsonValue &json, Link &out) {
    if (!json.isObject()) return r.fail("expected object");
    const QJsonObject object = json.toObject();
    return field(r, object, "source", out.source)
        && field(r, object, "target", out.target)
        && field(r, object, "raw", out.raw)
        && field(r, object, "subpath", out.subpath)
        && field(r, object, "alias", out.alias)
        && field(r, object, "kind", out.kind)
        && field(r, object, "start", out.start)
        && field(r, object, "end", out.end);
}

bool read(Reader &r, const QJsonValue &json, LinkKind &out) {
    static const std::map<QString, LinkKind> values{
        {"LINK", LinkKind::LINK},
        {"EMBED", LinkKind::EMBED},
    };
    return lookup(r, json, values, out);
}

bool read(Reader &r, const QJsonValue &json, Page &out) {
    if (!json.isObject()) return r.fail("expected object");
    const QJsonObject object = json.toObject();
    return field(r, object, "path", out.path)
        && field(r, object, "name", out.name)
        && field(r, object, "parent", out.parent)
        && field(r, object, "version", out.version)
        && field(r, object, "title", out.title)
        && field(r, object, "links", out.links)
        && field(r, object, "format", out.format);
}

bool read(Reader &r, const QJsonValue &json, PageReference &out) {
    if (!json.isObject()) return r.fail("expected object");
    const QJsonObject object = json.toObject();
    return field(r, object, "path", out.path)
        && field(r, object, "name", out.name)
        && field(r, object, "version", out.version);
}

bool read(Reader &r, const QJsonValue &json, VaultIndex &out) {
    if (!json.isObject()) return r.fail("expected object");
    const QJsonObject object = json.toObject();
    return field(r, object, "folders", out.folders)
        && field(r, object, "files", out.files);
}
} // namespace

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
template std::optional<Workspace> parse<Workspace>(const QByteArray &, QString *);
template std::optional<VaultIndex> parse<VaultIndex>(const QByteArray &, QString *);
}

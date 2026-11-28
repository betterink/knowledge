// Generated from core's serializers by `./gradlew generateClientModels`. Do not edit.
#pragma once
#include <QtCore/QByteArray>
#include <QtCore/QString>
#include <map>
#include <optional>
#include <variant>
#include <vector>

namespace api {
enum class FileFormat { FOLDER, MARKDOWN, EXCALIDRAW, CANVAS, BASE, PDF, IMAGE_JPG, IMAGE_PNG, VIDEO_MP4, VIDEO_MOV, UNKNOWN };
enum class LinkKind { LINK, EMBED };
struct Workspace;
struct Folder;
struct Attachment;
struct Version;
struct Base;
struct Diagram;
struct Link;
struct Page;
struct PageReference;
struct VaultIndex;
// Selected by the "kind" field.
using Node = std::variant<Attachment, Base, Diagram, Folder, Page>;

struct Version {
    QString hash{};
    qint64 size{};
    qint64 modified{};
};

struct PageReference {
    QString path{};
    QString name{};
    Version version{};
};

struct Folder {
    QString path{};
    QString name{};
    std::optional<QString> parent{};
    std::vector<Node> children{};
    std::optional<PageReference> folderNote{};
    FileFormat format{};
};

struct Workspace {
    QString name{};
    Folder root{};
};

struct Attachment {
    QString path{};
    QString name{};
    std::optional<QString> parent{};
    FileFormat format{};
    Version version{};
};

struct Base {
    QString path{};
    QString name{};
    std::optional<QString> parent{};
    Version version{};
    FileFormat format{};
};

struct Diagram {
    QString path{};
    QString name{};
    std::optional<QString> parent{};
    Version version{};
    std::vector<Link> links{};
    FileFormat format{};
};

struct Link {
    QString source{};
    std::optional<QString> target{};
    QString raw{};
    std::optional<QString> subpath{};
    std::optional<QString> alias{};
    LinkKind kind{};
    int start{};
    int end{};
};

struct Page {
    QString path{};
    QString name{};
    std::optional<QString> parent{};
    Version version{};
    QString title{};
    std::vector<Link> links{};
    FileFormat format{};
};

struct VaultIndex {
    std::vector<QString> folders{};
    std::map<QString, Version> files{};
};

// Returns nullopt on invalid JSON and sets error to "<path>: <reason>".
template <typename T> std::optional<T> parse(const QByteArray &json, QString *error = nullptr);
extern template std::optional<Workspace> parse<Workspace>(const QByteArray &, QString *);
extern template std::optional<VaultIndex> parse<VaultIndex>(const QByteArray &, QString *);
}

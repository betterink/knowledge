#include "ApiModels.h"
#include <QtCore/QDebug>
#include <QtCore/QFile>
#include <QtCore/QMap>

using namespace api;

static QByteArray fixture(const char *name) {
    QFile file(QString(FIXTURES) + "/" + name);
    return file.open(QIODevice::ReadOnly) ? file.readAll() : QByteArray();
}

template <class... Visitors> struct Overloaded : Visitors... {
    using Visitors::operator()...;
};
template <class... Visitors> Overloaded(Visitors...) -> Overloaded<Visitors...>;

// Counts each subtype the parser selected, by its "kind" name.
static void count(const Folder &folder, QMap<QString, int> &kinds) {
    for (const Node &node : folder.children)
        std::visit(Overloaded{
                       [&](const Folder &child) {
                           ++kinds["folder"];
                           count(child, kinds);
                       },
                       [&](const Page &) { ++kinds["page"]; },
                       [&](const Diagram &) { ++kinds["diagram"]; },
                       [&](const Base &) { ++kinds["base"]; },
                       [&](const Attachment &) { ++kinds["attachment"]; },
                   },
                   node);
}

static const Page *page(const Folder &folder, const QString &path) {
    for (const Node &node : folder.children) {
        if (const auto *found = std::get_if<Page>(&node); found && found->path == path) return found;
        if (const auto *child = std::get_if<Folder>(&node))
            if (const Page *found = page(*child, path)) return found;
    }
    return nullptr;
}

static QString failure(const QByteArray &json) {
    QString error;
    return parse<Workspace>(json, &error) ? QString("parsed") : error;
}

int main() {
    QString error;
    const auto workspace = parse<Workspace>(fixture("workspace.json"), &error);
    if (!workspace) {
        qWarning() << error;
        return 1;
    }
    QMap<QString, int> kinds;
    count(workspace->root, kinds);
    const QMap<QString, int> expected{{"attachment", 1}, {"base", 1}, {"diagram", 1}, {"folder", 4}, {"page", 5}};
    if (kinds != expected) {
        qWarning() << kinds;
        return 2;
    }

    const Page *home = page(workspace->root, "Home.md");
    if (!home || home->title != "Home Page" || home->format != FileFormat::MARKDOWN || home->parent != QString())
        return 3;
    const Page *projects = page(workspace->root, "Projects/Projects.md");
    if (!projects || projects->links.size() != 4) return 4;
    const Link &image = projects->links[2];
    if (image.kind != LinkKind::EMBED || image.alias != QString("300") || image.subpath || image.start >= image.end)
        return 5;

    const auto index = parse<VaultIndex>(fixture("manifest.json"), &error);
    if (!index || index->folders.size() != 4 || index->files.size() != 8
        || index->files.at("A/Same.md").hash != "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb"
        || index->files.at("A/Same.md").size != 1) {
        qWarning() << error;
        return 6;
    }

    // Invalid input names the exact location and never yields a value.
    const QByteArray root =
        R"({"name":"v","root":{"path":"","name":"v","parent":null,"folderNote":null,"format":"FOLDER","children":[%1]}})";
    const auto with = [&](const char *child) { return QString::fromUtf8(root).arg(child).toUtf8(); };
    const QList<QPair<QByteArray, QString>> cases{
        {with(R"({"kind":"shelf"})"), R"(root.children[0].kind: unknown value "shelf")"},
        {with(R"({"path":"x"})"), "root.children[0].kind: expected string"},
        {with(R"({"kind":"base","path":"x","name":"x","parent":null,"format":"BASE"})"),
         "root.children[0].version: missing"},
        {with(R"({"kind":"base","path":1})"), "root.children[0].path: expected string"},
        {with(
             R"({"kind":"base","path":"x","name":"x","parent":null,"format":"WAV","version":{"hash":"h","size":1,"modified":2}})"),
         R"(root.children[0].format: unknown value "WAV")"},
        {with(
             R"({"kind":"base","path":"x","name":"x","parent":null,"format":"BASE","version":{"hash":"h","size":1.5,"modified":2}})"),
         "root.children[0].version.size: expected integer"},
        {"{", "$: unterminated object"},
    };
    for (const auto &[json, message] : cases)
        if (failure(json) != message) {
            qWarning() << "expected" << message << "got" << failure(json);
            return 7;
        }
    if (failure(with(
            R"({"kind":"base","path":"x","name":"x","parent":null,"format":"BASE","version":{"hash":"h","size":1,"modified":2}})"))
        != "parsed")
        return 8;
    qInfo() << "PASS: every node kind selected by kind, links, enums, manifest map, located errors";
    return 0;
}

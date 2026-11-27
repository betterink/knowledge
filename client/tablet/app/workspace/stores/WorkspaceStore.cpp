#include "WorkspaceStore.h"
#include <QtCore/QDebug>

static const QString path = "v1/workspace";

const QStringList WorkspaceStore::migrations{
    "CREATE TABLE response(path TEXT PRIMARY KEY, body BLOB NOT NULL,"
    " fetched_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)",
};

WorkspaceStore::WorkspaceStore(Database &db, HttpClient *http, QObject *parent)
    : QObject(parent), m_db(db), m_http(http) {
    const auto rows = m_db.query("SELECT body, fetched_at FROM response WHERE path = ?", {path});
    QString error;
    if (!rows || rows->isEmpty()) show(std::nullopt, "No cached workspace");
    else if (auto cached = api::parse<api::Workspace>(rows->first()[0].toByteArray(), &error))
        show(std::move(cached), "Cached " + rows->first()[1].toString() + " UTC");
    else show(std::nullopt, "Cache unreadable (" + error + ")");
    refresh();
}

void WorkspaceStore::refresh() {
    if (!m_http) return;
    m_http->get(path, this, [this](const HttpResponse &response) {
        QString error = response.error.isEmpty() ? QString("HTTP %1").arg(response.status) : response.error;
        auto workspace = response.ok() ? api::parse<api::Workspace>(response.body, &error) : std::nullopt;
        if (!workspace) {
            show(std::nullopt, "Refresh failed (" + error + "); showing cache");
            return;
        }
        if (!m_db.exec("INSERT INTO response(path, body) VALUES(?, ?) ON CONFLICT(path)"
                       " DO UPDATE SET body = excluded.body, fetched_at = CURRENT_TIMESTAMP",
                       {path, response.body}))
            qWarning().noquote() << "Workspace cache write failed:" << m_db.lastError();
        show(std::move(workspace), "Fetched from server");
    });
}

void WorkspaceStore::show(std::optional<api::Workspace> workspace, const QString &status) {
    if (workspace) m_workspace = std::move(workspace);
    m_status = status;
    qInfo().noquote() << "Workspace:" << m_status;
    emit changed();
}

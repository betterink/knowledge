#include "WorkspaceStore.h"
#include <QtCore/QJsonDocument>
#include <QtCore/QJsonObject>
#include <QtCore/QDebug>

static const QString path = "v1/workspace";

const QStringList WorkspaceStore::migrations{
    "CREATE TABLE response(path TEXT PRIMARY KEY, body BLOB NOT NULL,"
    " fetched_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)",
};

WorkspaceStore::WorkspaceStore(Database &db, HttpClient *http, QObject *parent)
    : QObject(parent), m_db(db), m_http(http) {
    const auto rows = m_db.query("SELECT body, fetched_at FROM response WHERE path = ?", {path});
    if (rows && !rows->isEmpty())
        show(rows->first()[0].toByteArray(), "Cached " + rows->first()[1].toString() + " UTC");
    else show({}, "No cached workspace");
    refresh();
}

void WorkspaceStore::refresh() {
    if (!m_http) return;
    m_http->get(path, this, [this](const HttpResponse &response) {
        const QJsonDocument document = QJsonDocument::fromJson(response.body);
        if (!response.ok() || !document.isObject()) {
            const QString reason = response.error.isEmpty() ? QString("HTTP %1").arg(response.status) : response.error;
            show({}, "Refresh failed (" + reason + "); showing cache");
            return;
        }
        if (!m_db.exec("INSERT INTO response(path, body) VALUES(?, ?) ON CONFLICT(path)"
                       " DO UPDATE SET body = excluded.body, fetched_at = CURRENT_TIMESTAMP",
                       {path, response.body}))
            qWarning().noquote() << "Workspace cache write failed:" << m_db.lastError();
        show(response.body, "Fetched from server");
    });
}

void WorkspaceStore::show(const QByteArray &body, const QString &status) {
    if (!body.isEmpty()) m_workspace = QJsonDocument::fromJson(body).object().toVariantMap();
    m_status = status;
    qInfo().noquote() << "Workspace:" << m_status;
    emit changed();
}

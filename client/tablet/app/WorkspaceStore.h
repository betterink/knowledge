#pragma once
#include "Database.h"
#include "HttpClient.h"
#include <QtCore/QObject>
#include <QtCore/QVariantMap>

// Shows the cached workspace immediately, then replaces it with the backend copy.
class WorkspaceStore : public QObject {
    Q_OBJECT
    Q_PROPERTY(QVariantMap workspace READ workspace NOTIFY changed)
    Q_PROPERTY(QString status READ status NOTIFY changed)
public:
    static const QStringList migrations;
    WorkspaceStore(Database &db, HttpClient *http, QObject *parent = nullptr);
    QVariantMap workspace() const { return m_workspace; }
    QString status() const { return m_status; }
    Q_INVOKABLE void refresh();
signals:
    void changed();
private:
    void show(const QByteArray &body, const QString &status);
    Database &m_db;
    HttpClient *m_http;
    QVariantMap m_workspace;
    QString m_status;
};

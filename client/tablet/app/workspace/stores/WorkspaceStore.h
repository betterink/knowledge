#pragma once
#include "ApiModels.h"
#include "Database.h"
#include "HttpClient.h"
#include <QtCore/QObject>
#include <QtQml/qqmlregistration.h>

// Shows the cached workspace immediately, then replaces it with the backend copy.
class WorkspaceStore : public QObject {
    Q_OBJECT
    QML_ELEMENT
    QML_UNCREATABLE("Created in main.cpp")
    Q_PROPERTY(QString name READ name NOTIFY changed)
    Q_PROPERTY(QString status READ status NOTIFY changed)
public:
    static const QStringList migrations;
    WorkspaceStore(Database &db, HttpClient *http, QObject *parent = nullptr);
    QString name() const { return m_workspace ? m_workspace->name : QString(); }
    QString status() const { return m_status; }
    Q_INVOKABLE void refresh();
signals:
    void changed();
private:
    void show(std::optional<api::Workspace> workspace, const QString &status);
    Database &m_db;
    HttpClient *m_http;
    std::optional<api::Workspace> m_workspace;
    QString m_status;
};

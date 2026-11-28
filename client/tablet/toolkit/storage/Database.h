#pragma once
#include <QtCore/QString>
#include <QtCore/QStringList>
#include <QtCore/QVariant>
#include <memory>
#include <optional>

struct sqlite3;

// SQLite connection. Migration i runs once; PRAGMA user_version counts applied migrations.
class Database {
public:
    static std::unique_ptr<Database> open(const QString &path, const QStringList &migrations, QString *error);
    ~Database();
    Database(const Database &) = delete;
    Database &operator=(const Database &) = delete;
    bool exec(const QString &sql, const QVariantList &params = {});
    std::optional<QList<QVariantList>> query(const QString &sql, const QVariantList &params = {});
    QString lastError() const { return m_error; }
private:
    explicit Database(sqlite3 *db) : m_db(db) {}
    bool migrate(const QStringList &migrations);
    bool fail();
    sqlite3 *m_db;
    QString m_error;
};

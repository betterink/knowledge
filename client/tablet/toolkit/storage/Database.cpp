#include "Database.h"
#include <QtCore/QDir>
#include <QtCore/QFileInfo>
#include <sqlite3.h>

std::unique_ptr<Database> Database::open(const QString &path, const QStringList &migrations, QString *error) {
    QDir().mkpath(QFileInfo(path).absolutePath());
    sqlite3 *handle = nullptr;
    const int rc =
        sqlite3_open_v2(path.toUtf8().constData(), &handle, SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE, nullptr);
    std::unique_ptr<Database> db(new Database(handle));
    if (rc != SQLITE_OK) db->fail();
    else if (db->query("PRAGMA journal_mode=WAL") && db->migrate(migrations)) return db;
    if (error) *error = db->lastError();
    return nullptr;
}

Database::~Database() { sqlite3_close(m_db); }

bool Database::fail() {
    m_error = QString::fromUtf8(sqlite3_errmsg(m_db));
    return false;
}

bool Database::migrate(const QStringList &migrations) {
    const auto version = query("PRAGMA user_version");
    if (!version) return false;
    const int applied = version->first().first().toInt();
    if (applied > migrations.size()) {
        m_error = QString("Database schema %1 is newer than this app (%2)").arg(applied).arg(migrations.size());
        return false;
    }
    for (int i = applied; i < migrations.size(); ++i) {
        const QString script = "BEGIN;" + migrations[i] + QString(";PRAGMA user_version=%1;COMMIT;").arg(i + 1);
        if (sqlite3_exec(m_db, script.toUtf8().constData(), nullptr, nullptr, nullptr) != SQLITE_OK) {
            fail();
            sqlite3_exec(m_db, "ROLLBACK", nullptr, nullptr, nullptr);
            return false;
        }
    }
    return true;
}

static int bind(sqlite3_stmt *statement, int index, const QVariant &value) {
    if (value.isNull()) return sqlite3_bind_null(statement, index);
    switch (value.typeId()) {
    case QMetaType::Bool:
    case QMetaType::Int:
    case QMetaType::LongLong:
    case QMetaType::UInt:
    case QMetaType::ULongLong: return sqlite3_bind_int64(statement, index, value.toLongLong());
    case QMetaType::Double: return sqlite3_bind_double(statement, index, value.toDouble());
    case QMetaType::QByteArray: {
        const QByteArray bytes = value.toByteArray();
        return sqlite3_bind_blob64(statement, index, bytes.constData(), bytes.size(), SQLITE_TRANSIENT);
    }
    default: {
        const QByteArray text = value.toString().toUtf8();
        return sqlite3_bind_text64(statement, index, text.constData(), text.size(), SQLITE_TRANSIENT, SQLITE_UTF8);
    }
    }
}

static QVariant column(sqlite3_stmt *statement, int index) {
    switch (sqlite3_column_type(statement, index)) {
    case SQLITE_INTEGER: return qint64(sqlite3_column_int64(statement, index));
    case SQLITE_FLOAT: return sqlite3_column_double(statement, index);
    case SQLITE_TEXT:
        return QString::fromUtf8(reinterpret_cast<const char *>(sqlite3_column_text(statement, index)),
                                 sqlite3_column_bytes(statement, index));
    case SQLITE_BLOB:
        return QByteArray(static_cast<const char *>(sqlite3_column_blob(statement, index)),
                          sqlite3_column_bytes(statement, index));
    default: return QVariant();
    }
}

std::optional<QList<QVariantList>> Database::query(const QString &sql, const QVariantList &params) {
    sqlite3_stmt *statement = nullptr;
    if (sqlite3_prepare_v2(m_db, sql.toUtf8().constData(), -1, &statement, nullptr) != SQLITE_OK) {
        fail();
        return std::nullopt;
    }
    std::unique_ptr<sqlite3_stmt, int (*)(sqlite3_stmt *)> owner(statement, sqlite3_finalize);
    for (int i = 0; i < params.size(); ++i)
        if (bind(statement, i + 1, params[i]) != SQLITE_OK) {
            fail();
            return std::nullopt;
        }
    QList<QVariantList> rows;
    int rc;
    while ((rc = sqlite3_step(statement)) == SQLITE_ROW) {
        QVariantList row;
        for (int i = 0; i < sqlite3_column_count(statement); ++i)
            row.append(column(statement, i));
        rows.append(row);
    }
    if (rc != SQLITE_DONE) {
        fail();
        return std::nullopt;
    }
    return rows;
}

bool Database::exec(const QString &sql, const QVariantList &params) { return query(sql, params).has_value(); }

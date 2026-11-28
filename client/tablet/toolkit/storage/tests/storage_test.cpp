#include "Database.h"
#include <QtCore/QCoreApplication>
#include <QtCore/QTemporaryDir>
#include <QtCore/QDebug>

int main(int argc, char **argv) {
    QCoreApplication app(argc, argv);
    QTemporaryDir directory;
    const QString path = directory.filePath("nested/app.db");
    const QStringList migrations{
        "CREATE TABLE note(id INTEGER PRIMARY KEY, title TEXT NOT NULL, body BLOB)",
        "ALTER TABLE note ADD COLUMN score REAL",
    };
    QString error;
    {
        auto db = Database::open(path, migrations.mid(0, 1), &error);
        if (!db) {
            qWarning() << error;
            return 1;
        }
        if (!db->exec("INSERT INTO note(title, body) VALUES(?, ?)", {"first", QByteArray("\0\1", 2)})) return 2;
    }
    {
        // Reopening applies only the new migration and keeps existing rows.
        auto db = Database::open(path, migrations, &error);
        if (!db) {
            qWarning() << error;
            return 3;
        }
        if (!db->exec("UPDATE note SET score = ? WHERE title = ?", {1.5, "first"})) return 4;
        const auto rows = db->query("SELECT id, title, body, score, NULL FROM note");
        if (!rows || rows->size() != 1) return 5;
        const QVariantList row = rows->first();
        if (row != QVariantList{qint64(1), "first", QByteArray("\0\1", 2), 1.5, QVariant()}) {
            qWarning() << row;
            return 6;
        }
        if (db->query("SELECT * FROM missing") || !db->lastError().contains("missing")) return 7;
        if (db->query("PRAGMA journal_mode")->first().first() != "wal") return 8;
    }
    {
        // A failed migration rolls back and leaves the schema version unchanged.
        if (Database::open(path, migrations + QStringList{"CREATE TABLE note(id)"}, &error)) return 9;
        auto db = Database::open(path, migrations, &error);
        if (!db || db->query("PRAGMA user_version")->first().first() != 2) return 10;
    }
    if (Database::open(path, migrations.mid(0, 1), &error) || !error.contains("newer")) return 11;
    {
        // A trailing line comment must not swallow the version bump.
        const QString commented = directory.filePath("commented.db");
        const QStringList withComment{"CREATE TABLE item(id) -- create items"};
        auto db = Database::open(commented, withComment, &error);
        if (!db || db->query("PRAGMA user_version")->first().first() != 1) return 12;
        db.reset();
        if (!Database::open(commented, withComment, &error)) {
            qWarning() << error;
            return 13;
        }
    }
    qInfo() << "PASS: persistence across reopen, incremental migrations, rollback, typed values, errors";
    return 0;
}

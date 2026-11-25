#include <QCoreApplication>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QSslSocket>
#include <QTextStream>
#include <sqlite3.h>

static int writeSqlite(const QByteArray &path)
{
    sqlite3 *db = nullptr;
    char *error = nullptr;
    int rc = sqlite3_open(path.constData(), &db);
    if (rc == SQLITE_OK)
        rc = sqlite3_exec(db,
            "PRAGMA journal_mode=WAL;"
            "CREATE TABLE IF NOT EXISTS probe(id INTEGER PRIMARY KEY, at TEXT DEFAULT CURRENT_TIMESTAMP);"
            "INSERT INTO probe DEFAULT VALUES;", nullptr, nullptr, &error);
    sqlite3_stmt *count = nullptr;
    if (rc == SQLITE_OK && sqlite3_prepare_v2(db, "SELECT COUNT(*) FROM probe", -1, &count, nullptr) == SQLITE_OK
        && sqlite3_step(count) == SQLITE_ROW)
        QTextStream(stdout) << "sqlite " << sqlite3_libversion() << " rows=" << sqlite3_column_int(count, 0) << '\n';
    else
        QTextStream(stdout) << "sqlite error: " << (error ? error : sqlite3_errmsg(db)) << '\n';
    sqlite3_finalize(count);
    sqlite3_free(error);
    sqlite3_close(db);
    return rc == SQLITE_OK ? 0 : 1;
}

int main(int argc, char *argv[])
{
    QCoreApplication app(argc, argv);
    const QStringList args = app.arguments();
    const QUrl url(args.value(1, QStringLiteral("https://example.com/")));
    const QByteArray dbPath = args.value(2, QStringLiteral("/home/root/network-storage-probe.db")).toUtf8();

    QTextStream out(stdout);
    out << "qt " << qVersion() << " tls=" << QSslSocket::supportsSsl()
        << " backend=" << QSslSocket::activeBackend() << '\n';
    out.flush();

    const int storage = writeSqlite(dbPath);
    QNetworkAccessManager network;
    QNetworkRequest request(url);
    request.setTransferTimeout(15000);
    QNetworkReply *reply = network.get(request);
    QObject::connect(reply, &QNetworkReply::finished, &app, [&] {
        const int status = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
        out << "https " << url.toString() << " status=" << status << " error=" << reply->errorString() << '\n';
        app.exit((reply->error() == QNetworkReply::NoError ? 0 : 2) | storage);
    });
    return app.exec();
}

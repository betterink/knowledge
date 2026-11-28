#include "HttpClient.h"
#include <QtCore/QCoreApplication>
#include <QtCore/QEventLoop>
#include <QtCore/QTimer>
#include <QtCore/QDebug>
#include <QtNetwork/QTcpServer>
#include <QtNetwork/QTcpSocket>
#include <memory>

// Answers /api/ok with JSON, /api/truncated with a short body, others with 404; never answers /api/slow.
static void serve(QTcpServer &server) {
    QObject::connect(&server, &QTcpServer::newConnection, &server, [&server] {
        QTcpSocket *socket = server.nextPendingConnection();
        QObject::connect(socket, &QTcpSocket::readyRead, socket, [socket] {
            const QByteArray request = socket->readAll();
            if (request.startsWith("GET /api/slow")) return;
            if (request.startsWith("GET /api/truncated "))
                socket->write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\nConnection: close\r\n\r\n{\"a\":");
            else if (request.startsWith("GET /api/ok "))
                socket->write("HTTP/1.1 200 OK\r\nContent-Length: 11\r\nConnection: close\r\n\r\n{\"a\":true}\n");
            else socket->write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
            socket->disconnectFromHost();
        });
    });
}

static HttpResponse fetch(HttpClient &client, const QString &path) {
    QEventLoop loop;
    HttpResponse result;
    client.get(path, &loop, [&](const HttpResponse &response) {
        result = response;
        loop.quit();
    });
    loop.exec();
    return result;
}

int main(int argc, char **argv) {
    QCoreApplication app(argc, argv);
    QTcpServer server;
    if (!server.listen(QHostAddress::LocalHost)) return 1;
    serve(server);
    const QUrl base(QString("http://127.0.0.1:%1/api/").arg(server.serverPort()));
    HttpClient client(base, 300);

    const HttpResponse ok = fetch(client, "ok");
    if (!ok.ok() || ok.status != 200 || ok.body != "{\"a\":true}\n") {
        qWarning() << ok.status << ok.error;
        return 2;
    }
    const HttpResponse missing = fetch(client, "missing");
    if (missing.ok() || missing.status != 404 || !missing.error.isEmpty()) return 3;
    const HttpResponse slow = fetch(client, "slow");
    if (slow.ok() || slow.status != 0 || slow.error.isEmpty()) return 4;
    const HttpResponse truncated = fetch(client, "truncated");
    if (truncated.ok() || truncated.status != 200 || truncated.error.isEmpty()) {
        qWarning() << truncated.status << truncated.error << truncated.body;
        return 7;
    }

    QTcpServer closed;
    closed.listen(QHostAddress::LocalHost);
    const quint16 port = closed.serverPort();
    closed.close();
    HttpClient offline(QUrl(QString("http://127.0.0.1:%1/").arg(port)));
    const HttpResponse refused = fetch(offline, "ok");
    if (refused.ok() || refused.status != 0 || refused.error.isEmpty()) return 5;

    // A destroyed context cancels the request and never receives the callback.
    bool called = false;
    auto context = std::make_unique<QObject>();
    client.get("slow", context.get(), [&](const HttpResponse &) { called = true; });
    context.reset();
    QEventLoop wait;
    QTimer::singleShot(500, &wait, &QEventLoop::quit);
    wait.exec();
    if (called) return 6;
    qInfo() << "PASS: 2xx body, HTTP error status, timeout, truncated body, refused connection, cancelled context";
    return 0;
}

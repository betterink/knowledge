#pragma once
#include <QtCore/QByteArray>
#include <QtCore/QUrl>
#include <QtNetwork/QNetworkAccessManager>
#include <functional>

// error is set when the transfer failed (DNS, TLS, timeout, offline, truncated).
struct HttpResponse {
    int status = 0;
    QByteArray body;
    QString error;
    bool ok() const { return error.isEmpty() && status >= 200 && status < 300; }
};

// HTTP(S) against one base URL. TLS trusts the system CA bundle.
class HttpClient {
public:
    explicit HttpClient(const QUrl &baseUrl, int timeoutMs = 15000);
    // done runs on context's thread, and never after context is destroyed.
    void get(const QString &path, QObject *context, std::function<void(const HttpResponse &)> done);
private:
    QUrl m_baseUrl;
    QNetworkAccessManager m_network;
};

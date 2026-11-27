#include "HttpClient.h"
#include <QtNetwork/QNetworkReply>

HttpClient::HttpClient(const QUrl &baseUrl, int timeoutMs) : m_baseUrl(baseUrl) {
    m_network.setTransferTimeout(timeoutMs);
}

void HttpClient::get(const QString &path, QObject *context, std::function<void(const HttpResponse &)> done) {
    QNetworkReply *reply = m_network.get(QNetworkRequest(m_baseUrl.resolved(QUrl(path))));
    reply->setParent(context); // Destroying context cancels the request.
    QObject::connect(reply, &QNetworkReply::finished, context, [reply, done] {
        reply->deleteLater();
        HttpResponse response;
        response.status = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
        if (response.status) response.body = reply->readAll();
        else response.error = reply->errorString();
        done(response);
    });
}

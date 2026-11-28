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
        // Codes below ContentAccessDenied are transport failures, not HTTP statuses.
        const bool transportFailed =
            reply->error() != QNetworkReply::NoError && reply->error() < QNetworkReply::ContentAccessDenied;
        if (response.status) response.body = reply->readAll();
        if (!response.status || transportFailed) response.error = reply->errorString();
        done(response);
    });
}

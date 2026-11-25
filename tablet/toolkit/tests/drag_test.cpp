#include "EpaperDisplayControl.h"
#include <QtGui/QGuiApplication>
#include <QtGui/QMouseEvent>
#include <QtQuick/QQuickWindow>
#include <QtQuick/QQuickItem>
#include <QtQml/QQmlEngine>
#include <QtQml/QQmlContext>
#include <QtQml/QQmlComponent>
#include <QtCore/QDebug>
#include <memory>

int main(int argc, char **argv) {
    QGuiApplication app(argc, argv);
    EpaperDisplayControl controller;
    QQmlEngine engine;
    engine.rootContext()->setContextProperty("testController", &controller);
    QQmlComponent component(&engine);
    component.setData("import QtQuick\nimport \"qrc:/toolkit\"\n"
        "DragSurface { width: 400; height: 400; controller: testController }", QUrl());
    std::unique_ptr<QObject> object(component.create());
    if (!object) { qWarning() << component.errors(); return 1; }
    auto *surface = qobject_cast<QQuickItem *>(object.get());
    QQuickWindow window; window.resize(400, 400);
    surface->setParentItem(window.contentItem());
    window.show(); app.processEvents();
    auto *square = surface->findChild<QQuickItem *>("dragSquare");
    if (!square) return 2;
    const auto send = [&](QEvent::Type type, QPointF position) {
        const bool release = type == QEvent::MouseButtonRelease;
        QMouseEvent event(type, position, position,
            type == QEvent::MouseMove ? Qt::NoButton : Qt::LeftButton,
            release ? Qt::NoButton : Qt::LeftButton, Qt::NoModifier);
        QCoreApplication::sendEvent(&window, &event);
        app.processEvents();
    };
    // A tap and sub-threshold jitter must leave the square in place.
    send(QEvent::MouseButtonPress, {50, 120});
    send(QEvent::MouseMove, {51, 120});
    send(QEvent::MouseButtonRelease, {51, 120});
    if (square->x() != 30 || square->y() != 100) return 3;
    send(QEvent::MouseButtonPress, {50, 120});
    send(QEvent::MouseMove, {54, 120});
    send(QEvent::MouseMove, {60, 120});
    if (square->x() < 38 || square->x() > 40) {
        qWarning() << "Small drag was delayed or lost initial movement:" << square->x(); return 4;
    }
    send(QEvent::MouseMove, {700, 700});
    send(QEvent::MouseButtonRelease, {700, 700});
    if (square->x() > 320 || square->y() > 320 || square->x() < 0 || square->y() < 60) return 5;
    qInfo() << "PASS: real QML drag, tap/jitter stability, small-movement response and bounds";
    surface->setParentItem(nullptr);
    return 0;
}

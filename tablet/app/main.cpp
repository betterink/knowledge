#include <QtGui/QGuiApplication>
#include <QtQuick/QQuickWindow>
#include <QtQml/QQmlApplicationEngine>
#include <QtQml/QQmlContext>
#include <QtCore/QTimer>
#include <QtCore/QDebug>
#include <QtCore/QSysInfo>
#include "Toolkit.h"
#include "EpaperDisplayControl.h"

int main(int argc, char **argv) {
    for (int i = 1; i < argc; ++i) {
        if (QByteArray(argv[i]) == "--probe") {
            qInfo() << "rm-knowledge; compiled Qt" << QT_VERSION_STR
                    << "runtime Qt" << qVersion() << "architecture" << QSysInfo::currentCpuArchitecture();
            return 0;
        }
    }
    QCoreApplication::setAttribute(Qt::AA_SynthesizeMouseForUnhandledTabletEvents, true);
    QGuiApplication app(argc, argv);
    app.setApplicationName("rm-knowledge");
    const bool fullscreen = app.arguments().contains("--fullscreen");
    Ink::registerTypes();
    EpaperDisplayControl displayControl;
    QQmlApplicationEngine engine;
    engine.rootContext()->setContextProperty("appFullscreen", fullscreen);
    engine.rootContext()->setContextProperty("displayControl", &displayControl);
    QObject::connect(&engine, &QQmlApplicationEngine::objectCreationFailed, &app, [] {
        QCoreApplication::exit(1);
    }, Qt::QueuedConnection);
    engine.load(QUrl("qrc:/app/Main.qml"));
    if (engine.rootObjects().isEmpty()) return 1;
    auto *window = qobject_cast<QQuickWindow *>(engine.rootObjects().first());
    if (window) {
        QObject::connect(window, &QQuickWindow::afterRendering, &displayControl,
                         &EpaperDisplayControl::initialize,
                         static_cast<Qt::ConnectionType>(Qt::QueuedConnection | Qt::SingleShotConnection));
        if (fullscreen && window->screen()) {
            // Minimal embedded QPA backends need explicit geometry.
            window->setGeometry(window->screen()->geometry());
            window->showFullScreen();
        }
    }
    if (app.arguments().contains("--smoke-test")) QTimer::singleShot(500, &app, &QCoreApplication::quit);
    const int result = app.exec();
    displayControl.shutdown();
    return result;
}

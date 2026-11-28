pragma Singleton
import QtQuick

QtObject {
    property real scale: 1
    readonly property color background: "white"
    readonly property color foreground: "black"
    readonly property color muted: "#606060"
    readonly property color pressed: "#d0d0d0"
    readonly property color scrim: "#99ffffff"
    readonly property real spacing: 16 * scale
    readonly property real radius: 8 * scale
    readonly property real borderWidth: Math.max(1, Math.round(2 * scale))
    readonly property real controlHeight: 56 * scale
    readonly property real fontSize: 22 * scale
    readonly property real titleSize: 30 * scale
}

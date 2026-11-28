import QtQuick
import QtQuick.Controls.Basic

Popup {
    id: toast
    property int duration: 3000
    function show(message: string) {
        label.text = message
        open()
        timer.restart()
    }
    parent: Overlay.overlay
    width: Math.min(label.implicitWidth + leftPadding + rightPadding, parent.width - 4 * Theme.spacing)
    x: (parent.width - width) / 2
    y: parent.height - height - 2 * Theme.spacing
    padding: Theme.spacing
    closePolicy: Popup.NoAutoClose
    enter: null
    exit: null
    background: Rectangle {
        color: Theme.foreground
        radius: Theme.radius
    }
    contentItem: Text {
        id: label
        font.pixelSize: Theme.fontSize
        color: Theme.background
        wrapMode: Text.Wrap
    }
    Timer {
        id: timer
        interval: toast.duration
        onTriggered: toast.close()
    }
}

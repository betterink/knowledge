import QtQuick
import QtQuick.Controls.Basic

Popup {
    id: sidebar
    property string title
    default property alias content: column.data
    parent: Overlay.overlay
    width: Math.min(parent.width * 0.8, 480 * Theme.scale)
    height: parent.height
    modal: true
    padding: Theme.spacing
    enter: null
    exit: null
    Overlay.modal: Rectangle { color: Theme.scrim }
    background: Rectangle {
        color: Theme.background
        Rectangle {
            anchors.right: parent.right
            width: Theme.borderWidth
            height: parent.height
            color: Theme.foreground
        }
    }
    contentItem: Column {
        id: column
        spacing: Theme.spacing / 2
        Text {
            width: column.width
            text: sidebar.title
            font.pixelSize: Theme.titleSize
            font.bold: true
            elide: Text.ElideRight
        }
    }
}

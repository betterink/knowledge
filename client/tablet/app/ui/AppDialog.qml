import QtQuick
import QtQuick.Controls.Basic

Popup {
    id: dialog
    property string title
    property string message
    property string acceptText: "OK"
    // Empty hides the cancel button.
    property string rejectText
    default property alias content: body.data
    signal accepted()
    signal rejected()
    parent: Overlay.overlay
    anchors.centerIn: parent
    width: Math.min(parent.width - 2 * Theme.spacing, 640 * Theme.scale)
    modal: true
    padding: Theme.spacing * 1.5
    enter: null
    exit: null
    Overlay.modal: Rectangle { color: Theme.scrim }
    background: Rectangle {
        color: Theme.background
        radius: Theme.radius
        border.width: Theme.borderWidth
        border.color: Theme.foreground
    }
    contentItem: Column {
        id: column
        spacing: Theme.spacing
        Text {
            width: column.width
            text: dialog.title
            font.pixelSize: Theme.titleSize
            font.bold: true
            wrapMode: Text.Wrap
        }
        Text {
            width: column.width
            visible: dialog.message !== ""
            text: dialog.message
            font.pixelSize: Theme.fontSize
            color: Theme.muted
            wrapMode: Text.Wrap
        }
        Column {
            id: body
            width: column.width
            spacing: Theme.spacing
        }
        Row {
            width: column.width
            layoutDirection: Qt.RightToLeft
            spacing: Theme.spacing / 2
            AppButton {
                variant: "primary"
                text: dialog.acceptText
                onClicked: { dialog.accepted(); dialog.close() }
            }
            AppButton {
                visible: dialog.rejectText !== ""
                text: dialog.rejectText
                onClicked: { dialog.rejected(); dialog.close() }
            }
        }
    }
}

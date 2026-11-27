import QtQuick
import QtQuick.Controls.Basic

Button {
    id: control
    // "primary", "outline" or "ghost"
    property string variant: "outline"
    readonly property bool filled: variant === "primary"
    implicitHeight: Theme.controlHeight
    leftPadding: Theme.spacing
    rightPadding: Theme.spacing
    font.pixelSize: Theme.fontSize
    contentItem: Text {
        text: control.text
        font: control.font
        color: !control.enabled ? Theme.muted : control.filled ? Theme.background : Theme.foreground
        horizontalAlignment: control.variant === "ghost" ? Text.AlignLeft : Text.AlignHCenter
        verticalAlignment: Text.AlignVCenter
        elide: Text.ElideRight
    }
    background: Rectangle {
        radius: Theme.radius
        color: control.filled ? Theme.foreground : control.down ? Theme.pressed : Theme.background
        border.width: control.variant === "outline" ? Theme.borderWidth : 0
        border.color: Theme.foreground
    }
}

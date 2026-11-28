pragma ComponentBehavior: Bound
import QtQuick
import QtQuick.Controls.Basic

Popup {
    id: menu
    default property list<Action> actions
    width: 360 * Theme.scale
    padding: Theme.spacing / 2
    enter: null
    exit: null
    background: Rectangle {
        color: Theme.background
        radius: Theme.radius
        border.width: Theme.borderWidth
        border.color: Theme.foreground
    }
    contentItem: Column {
        id: column
        Repeater {
            model: menu.actions
            AppButton {
                required property Action modelData
                width: column.width
                variant: "ghost"
                action: modelData
                onClicked: menu.close()
            }
        }
    }
}

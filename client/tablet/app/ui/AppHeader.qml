import QtQuick

Rectangle {
    id: header
    property string title
    signal menuClicked()
    signal actionsClicked()
    implicitHeight: Theme.controlHeight + Theme.spacing
    color: Theme.background
    AppButton {
        id: menuButton
        anchors.left: parent.left
        anchors.leftMargin: Theme.spacing / 2
        anchors.verticalCenter: parent.verticalCenter
        variant: "ghost"
        text: "Menu"
        onClicked: header.menuClicked()
    }
    Text {
        anchors.left: menuButton.right
        anchors.right: actionsButton.left
        anchors.verticalCenter: parent.verticalCenter
        horizontalAlignment: Text.AlignHCenter
        text: header.title
        font.pixelSize: Theme.titleSize
        font.bold: true
        elide: Text.ElideRight
    }
    AppButton {
        id: actionsButton
        anchors.right: parent.right
        anchors.rightMargin: Theme.spacing / 2
        anchors.verticalCenter: parent.verticalCenter
        variant: "ghost"
        text: "More"
        onClicked: header.actionsClicked()
    }
    Rectangle {
        anchors.bottom: parent.bottom
        width: parent.width
        height: Theme.borderWidth
        color: Theme.foreground
    }
}

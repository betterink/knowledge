import QtQuick
import QtQuick.Window

Window {
    width: appFullscreen ? Screen.width : 900
    height: appFullscreen ? Screen.height : 1200
    visible: true
    color: "white"
    title: "knowledge"
    Text {
        anchors.centerIn: parent
        text: "knowledge"
        font.pixelSize: 48
    }
}

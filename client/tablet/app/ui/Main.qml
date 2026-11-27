import QtQuick
import QtQuick.Window

Window {
    width: appFullscreen ? Screen.width : 900
    height: appFullscreen ? Screen.height : 1200
    visible: true
    color: "white"
    title: "rm-knowledge"
    Column {
        anchors.centerIn: parent
        spacing: 16
        Text { text: workspaceStore.workspace.name ?? "rm-knowledge"; font.pixelSize: 48 }
        Text { text: workspaceStore.status; font.pixelSize: 20 }
    }
}

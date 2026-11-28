import QtQuick

Item {
    id: screen
    required property WorkspaceStore store
    Column {
        anchors.centerIn: parent
        spacing: 16
        Text { text: screen.store.name || "rm-knowledge"; font.pixelSize: 48 }
        Text { text: screen.store.status; font.pixelSize: 20 }
    }
}

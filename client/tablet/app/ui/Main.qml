import QtQuick
import QtQuick.Controls.Basic
import QtQuick.Window

Window {
    id: window
    required property WorkspaceStore workspaceStore
    required property bool fullscreen
    width: fullscreen ? Screen.width : 900
    height: fullscreen ? Screen.height : 1200
    visible: true
    color: Theme.background
    title: "rm-knowledge"
    Binding {
        target: Theme
        property: "scale"
        value: Math.max(0.65, window.width / 900)
    }
    AppHeader {
        id: header
        width: parent.width
        title: window.workspaceStore.name || "rm-knowledge"
        onMenuClicked: sidebar.open()
        onActionsClicked: actions.open()
    }
    WorkspaceScreen {
        anchors.top: header.bottom
        anchors.bottom: parent.bottom
        width: parent.width
        store: window.workspaceStore
    }
    Sidebar {
        id: sidebar
        title: "rm-knowledge"
        AppButton {
            width: parent.width
            variant: "ghost"
            text: "Workspace"
            onClicked: sidebar.close()
        }
        AppButton {
            width: parent.width
            variant: "ghost"
            text: "Exit"
            onClicked: { sidebar.close(); exitDialog.open() }
        }
    }
    ActionMenu {
        id: actions
        parent: header
        x: header.width - width - Theme.spacing / 2
        y: header.height
        Action { text: "Refresh"; onTriggered: window.workspaceStore.refresh() }
        Action { text: "About"; onTriggered: aboutDialog.open() }
    }
    AppDialog {
        id: aboutDialog
        title: "rm-knowledge"
        message: "Obsidian-backed knowledge base for the reMarkable Paper Pro."
    }
    AppDialog {
        id: exitDialog
        title: "Exit rm-knowledge?"
        message: "The tablet returns to its home screen."
        acceptText: "Exit"
        rejectText: "Cancel"
        onAccepted: Qt.quit()
    }
    Toast { id: toast }
    Connections {
        target: window.workspaceStore
        function onChanged() { toast.show(window.workspaceStore.status) }
    }
}

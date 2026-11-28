import QtQuick 2.15
ListView {
    property real scaleFactor: 1
    required property var controller
    id: list
    anchors.fill: parent
    clip: true; model: 200; reuseItems: true
    boundsBehavior: Flickable.StopAtBounds
    boundsMovement: Flickable.StopAtBounds
    pixelAligned: true
    onMovementStarted: controller.setMotionActive(true)
    onMovementEnded: controller.setMotionActive(false)
    onVisibleChanged: if (!visible) { cancelFlick(); controller.setMotionActive(false) }
    delegate: Item {
        required property int index
        width: list.width; height: 90 * scaleFactor
        Text { anchors.verticalCenter: parent.verticalCenter; text: "Document " + (index + 1); font.pixelSize: 24 * scaleFactor }
        Rectangle { anchors.bottom: parent.bottom; width: parent.width; height: 1; color: "black" }
    }
}

import QtQuick 2.15
Rectangle {
    property real scaleFactor: 1
    required property var controller
    // Small explicit threshold avoids platform-dependent drag activation distance.
    property real activationDistance: 2
    property bool prepareDisplay: true
    function syncDisplayPolicy() {
        controller.setDragPageActive(visible && prepareDisplay)
        if (!visible) controller.setMotionActive(false)
    }
    onVisibleChanged: syncDisplayPolicy()
    onPrepareDisplayChanged: syncDisplayPolicy()
    Component.onCompleted: syncDisplayPolicy()
    id: dragArea
    anchors.fill: parent
    color: "white"; border.color: "black"
    Text { x: 16; y: 16; text: "Drag the black square. No animation or IPC."; font.pixelSize: 20 * scaleFactor }
    Rectangle {
        id: square; objectName: "dragSquare"
        x: 30; y: 100; width: 80 * scaleFactor; height: width; color: "black"
        MouseArea {
            anchors.fill: parent; drag.target: square
            drag.threshold: dragArea.activationDistance
            // Preserve the movement accumulated while crossing the threshold.
            drag.smoothed: false
            onPressed: controller.setMotionActive(true)
            onReleased: controller.setMotionActive(false)
            onCanceled: controller.setMotionActive(false)
            drag.minimumX: 0; drag.maximumX: dragArea.width - square.width
            drag.minimumY: 60 * scaleFactor; drag.maximumY: dragArea.height - square.height
        }
    }
}

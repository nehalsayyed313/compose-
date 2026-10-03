import QtQuick
import QtQuick.Controls
import QtQuick.Layouts

ApplicationWindow {
    id: window
    visible: true
    width: 360
    height: 640
    title: qsTr("MyQtApp")

    header: ToolBar {
        Label {
            text: qsTr("MyQtApp")
            font.pixelSize: 20
            anchors.centerIn: parent
        }
    }

    ColumnLayout {
        anchors.centerIn: parent
        spacing: 20

        Label {
            text: qsTr("Hello, Qt Mobile!")
            font.pixelSize: 22
            Layout.alignment: Qt.AlignHCenter
        }

        Button {
            id: countBtn
            text: qsTr("Tap Me")
            highlighted: true
            Layout.alignment: Qt.AlignHCenter
            property int count: 0
            onClicked: {
                count++;
                statusText.text = qsTr("Tapped: %1 times").arg(count);
            }
        }

        Label {
            id: statusText
            text: qsTr("Tapped: 0 times")
            font.pixelSize: 16
            Layout.alignment: Qt.AlignHCenter
        }
    }
}

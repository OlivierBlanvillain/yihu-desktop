This program is a complete replacement for Yihuchess's Android/iOS applications to interface with their Bluetooth Goban.
Concretely, it performs back-and-forth translation of moves between a desktop Go application and a physical Goban.
Each physical stone placement is translated into a click, and the state of a Goban on the screen is is projected onto the Goban's LED lights, in real time.

This program improves over the manufacturer's application in that it is faster (less move delay) and much more consistent (for instance, when a stone is placed, the program *repeatedly* clicks until the newly added stone appears).

## Setup

1. Install JDK 8 or above and [tinyb](https://github.com/intel-iot-devkit/tinyb).
2. Update `GOBAN_MAC_ADDRESS` in `BluetoothDriver.java` with your device's address.
3. Optionally update `INIT_*` in `ScreenDriver.java` to speed up the screen capture setup.
4. `make compile`

## Usage

1. `make run`
2. Turn on the Bluetooth Goban.
3. Position the screen capture window on top the board.
4. Click `Start screen capture!`.

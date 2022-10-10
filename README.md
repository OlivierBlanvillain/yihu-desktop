This program is a complete replacement for Yihuchess's Android/iOS applications to interface with their Bluetooth Goban.
Concretely, it performs back-and-forth translation of moves between a desktop Go application and a physical Goban.
Each physical stone placement is translated into a click, and the state of a Goban on the screen is projected on the Goban, in real-time.

This program improves over the manufacturer's application in that it is faster (less move delay) and much more reliable (for instance, when placing a stone, the program *repeatedly* clicks until the newly added stone appears).

The cryptographic-looking code in `src/main/java/yihuchess/BluetoothDriver.java` is the result of a weekend's worth of reverse engineering on [the manufacturer's APK](http://www.yihuchess.com/software.html), using [CFR](https://www.benf.org/other/cfr/) and [Ghidra](https://ghidra-sre.org/) with the [JNIAnalyzer](https://github.com/Ayrx/JNIAnalyzer) plugin.

## Setup

1. Install JDK 11 and [tinyb](https://github.com/intel-iot-devkit/tinyb) (with Java bindings).
2. Update `GOBAN_MAC_ADDRESS` in `src/main/java/yihuchess/Config.java` with your device's address.
3. Also update `INIT_{X,Y,W,H}` with your screen layout. The `MANUAL_SCREEN_CAPTURE_SETUP` setting helps with that initial setup: it opens a window that you should resize and position over your goban to set those `INIT_{X,Y,W,H}` values.
4. `make run`

## Kernel Tweaks

The following kernel settings increase the stability of the Bluetooth connection to the Yihuchess Goban:

Service (`/etc/systemd/system/fix-goban.service`):

    [Unit]
    Description=run root script at boot/wake to fix bluetooth goban
    Before=bluetooth.service

    [Service]
    Type=oneshot
    RemainAfterExit=yes
    ExecStart=/usr/local/bin/fix-goban.sh

    [Install]
    WantedBy=bluetooth.service

Script (`/usr/local/bin/fix-goban.sh`):

    #!/bin/sh

    /bin/sleep 2
    echo 5 > /sys/kernel/debug/bluetooth/hci0/conn_latency
    echo 2000 > /sys/kernel/debug/bluetooth/hci0/supervision_timeout

Setup:

    sudo chown root:root /etc/systemd/system/fix-goban.service
    sudo chmod a+rx /usr/local/bin/fix-goban.sh
    sudo systemctl enable fix-goban.service
    sudo systemctl enable fix-goban.service --now

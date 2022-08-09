package yihuchess;

import tinyb.BluetoothException;
import java.awt.AWTException;
import java.awt.Point;
import java.awt.Robot;
import java.util.function.Consumer;
import java.util.Arrays;
import static yihuchess.Config.*;
import tinyb.BluetoothManager;

class Main {
  public static void main(String[] args) throws AWTException {
    loop(new Screen(new Robot()), new Bluetooth(BluetoothManager.getBluetoothManager()));
  }

  static void loop(Screen screen, Bluetooth bluetooth) {
    if (MANUAL_SCREEN_CAPTURE_SETUP)
      screen.init();

    Consumer<Point> bluetoothCallback = (p) -> {
      int c = screen.click(p.x, p.y);
      var coord = (char)('A' + p.x + (p.x > 7 ? 1 : 0)) + "" + (19 - p.y);
      var count = c == 0 ? " (failed?)" : c > 1 ? " ("+c+"x)" : "";
      System.out.print("\nClicking on " + coord + count);
    };
    bluetooth.init(bluetoothCallback);
    bluetooth.setAllLights(SPLASH);

    int[][] prev = SPLASH;
    for (;;) {
      try {
        if (!bluetooth.isConnected()) {
          System.out.print("\nReconnecting");
          bluetooth.connect();
          prev = SPLASH;
        }
        var next = screen.screenshot();
        if (!Arrays.deepEquals(prev, next)) {
          var ok = bluetooth.setAllLights(next);
          if (ok) {
            System.out.print(".");
            prev = next;
          }
        }
      } catch (BluetoothException e) {
        var m1 = "GDBus.Error:org.bluez.Error.Failed: Not connected";
        var m2 = "GDBus.Error:org.bluez.Error.Failed: Operation failed with ATT error: 0x0e";
        var m3 = "GDBus.Error:org.bluez.Error.Failed: le-connection-abort-by-local";
        var m = e.getMessage();
        if (!m.equals(m1) && !m.equals(m2) && !m.equals(m3)) {
          System.out.println("");
          e.printStackTrace();
          bluetooth.init(bluetoothCallback);
          prev = SPLASH;
        }
      }
      screen.sleep(MAIN_LOOP_PERIOD_MILLI);
    }
  }
}

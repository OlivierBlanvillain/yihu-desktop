package yihuchess;

import tinyb.BluetoothException;
import java.awt.AWTException;
import java.awt.Point;
import java.awt.Robot;
import java.util.function.Consumer;
import java.util.Arrays;
import static yihuchess.Config.*;
import tinyb.BluetoothManager;
import static java.lang.System.currentTimeMillis;

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

    var lastMessageMillis = currentTimeMillis();
    int[][] prev = new int[19][19];
    for (;;) {
      try {
        if (!bluetooth.isConnected()) {
          System.out.print("\nReconnecting");
          bluetooth.connect();
          prev = new int[19][19];
        }
        var next = screen.screenshot();
        if (!Arrays.deepEquals(prev, next)) {
          var added = countAdded(prev, next);
          var removed = countRemoved(prev, next);
          if (added == 1) {
            var newStone = singleNewStone(prev, next);
            var newColor = next[newStone.x][newStone.y];
            var ok = bluetooth.setLight(newStone.x, newStone.y, UNICOLOR ? 3 : newColor);
            if (!ok) continue;
            lastMessageMillis = currentTimeMillis();
            System.out.print(MANUAL_SCREEN_CAPTURE_SETUP ? showBoard(next) : ",");
          }
          if (added != 1 || removed != 0) {
            var ok = bluetooth.setAllLights(next);
            if (!ok) continue;
            lastMessageMillis = currentTimeMillis();
            System.out.print(MANUAL_SCREEN_CAPTURE_SETUP ? showBoard(next) : ".");
          }
          prev = next;
        } else if (currentTimeMillis() - lastMessageMillis > BLUETOOTH_TIMEOUT_MILLI) {
          var ok = bluetooth.setAllLights(prev);
          if (!ok) continue;
          lastMessageMillis = currentTimeMillis();
          System.out.print("-");
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
          prev = new int[19][19];
        }
      }
      screen.sleep(MAIN_LOOP_PERIOD_MILLI);
    }
  }

  static Point singleNewStone(int[][] prev, int[][] next) {
    for (var i = 0; i < 19; ++i)
      for (var j = 0; j < 19; ++j)
        if (next[i][j] != 0 && next[i][j] != prev[i][j])
          return new Point(i, j);
    throw new IllegalArgumentException();
  }

  static int countAdded(int[][] prev, int[][] next) {
    var count = 0;
    for (var i = 0; i < 19; ++i)
      for (var j = 0; j < 19; ++j)
        if (next[i][j] != 0 && next[i][j] != prev[i][j])
          ++count;
    return count;
  }

  static int countRemoved(int[][] prev, int[][] next) {
    var count = 0;
    for (var i = 0; i < 19; ++i)
      for (var j = 0; j < 19; ++j)
        if (next[i][j] == 0 && next[i][j] != prev[i][j])
          ++count;
    return count;
  }

  private static String showBoard(int[][] board) {
    var sb = new StringBuilder();
    sb.append("\n");
    sb.append("   a b c d e f g h j k l m n o p q r s t\n");
    for (int j = 0; j < 19; ++j) {
      if (19 - j < 10)
        sb.append(' ');
      sb.append(String.valueOf(19 - j));
      for (int i = 0; i < 19; ++i) {
        boolean oshiI = i == 3 || i == 9 || i == 15;
        boolean oshiJ = j == 3 || j == 9 || j == 15;
        if (board[i][j] == 0 && oshiI && oshiJ)
          sb.append(" +");
        else if (board[i][j] == 0)
          sb.append(" .");
        else if (board[i][j] == 1)
          sb.append(" x");
        else
          sb.append(" o");
      }
      sb.append(' ');
      sb.append(String.valueOf(19 - j));
      sb.append('\n');
    }
    sb.append("   a b c d e f g h j k l m n o p q r s t\n");
    return sb.toString();
  }
}

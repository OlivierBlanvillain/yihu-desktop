package yihuchess;

import java.util.*;
import tinyb.BluetoothException;
import java.awt.AWTException;
import java.awt.Point;
import java.util.function.Consumer;
import static yihuchess.Config.*;

public class Main {
  public static int EMPTY = 0;
  public static int BLACK = 1;
  public static int WHITE = 2;

  public static void main(String[] args) throws InterruptedException, AWTException {
    var screenDriver = new ScreenDriver();
    var bluetoothDriver = new BluetoothDriver();

    Consumer<Point> bluetoothCallback = (p) -> {
      var sb = new StringBuilder();
      sb.append("\nClicking on ");
      sb.append(showCoord(p));
      int count = screenDriver.click(p.x, p.y);
      if (count == 0)
        sb.append(" (failed?)");
      else if (count != 1)
        sb.append(" (" + count + "x)");
      System.out.print(sb.toString());
    };

    if (MANUAL_SCREEN_CAPTURE_SETUP)
      screenDriver.init();

    bluetoothDriver.init(bluetoothCallback);

    int[][] prev = null;
    for (;;) {
      try {
        var next = screenDriver.screenshot();
        if (!equals(prev, next)) {
          var ok = bluetoothDriver.setAllLights(next, UNICOLOR);
          if (ok) {
            System.out.print(".");
            prev = next;
          }
        }
        if (!bluetoothDriver.isConnected()) {
          System.out.print("\nReconnecting");
          bluetoothDriver.connect();
          prev = null;
        }
      } catch (BluetoothException e) {
        var m1 = "GDBus.Error:org.bluez.Error.Failed: Not connected";
        var m2 = "GDBus.Error:org.bluez.Error.Failed: Operation failed with ATT error: 0x0e";
        var m3 = "GDBus.Error:org.bluez.Error.Failed: le-connection-abort-by-local";
        var msg = e.getMessage();
        if (!msg.equals(m1) && !msg.equals(m2) && !msg.equals(m3)) {
          System.out.println("");
          e.printStackTrace();
          bluetoothDriver.init(bluetoothCallback);
          prev = null;
        }
      }
      Thread.sleep(MAIN_LOOP_PERIOD_MILLI);
    }
  }

  private static boolean equals(int[][] prev, int[][] next) {
    if (prev == null || next == null)
      return false;
    for (int i = 0; i < 19; ++i)
      for (int j = 0; j < 19; ++j)
        if (next[i][j] != prev[i][j])
          return false;
    return true;
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
        else if (board[i][j] == EMPTY)
          sb.append(" .");
        else if (board[i][j] == BLACK)
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

  public static String showCoord(Point p) {
    return (char)('A' + p.x + (p.x > 7 ? 1 : 0)) + "" + (19 - p.y);
  }
}

import java.util.*;
import tinyb.BluetoothException;
import java.awt.AWTException;
import java.awt.Point;
import java.util.function.Consumer;

public class Main {
  public static int EMPTY = 0;
  public static int BLACK = 1;
  public static int WHITE = 2;

  public static void main(String[] args) throws InterruptedException, AWTException {
    ScreenDriver screenDriver = new ScreenDriver();
    BluetoothDriver bluetoothDriver = new BluetoothDriver();
    Consumer<Point> bluetoothCallback = (p) -> {
      StringBuilder sb = new StringBuilder();
      sb.append("Clicking on ");
      sb.append(showCoord(p));
      int count = screenDriver.click(p.x, p.y);
      if (count == 0)
        sb.append(" (failed?)");
      else if (count != 1)
        sb.append(" (" + count + "x)");
      System.out.println(sb.toString());
    };

    bluetoothDriver.init(bluetoothCallback);
    screenDriver.init();
    Thread.sleep(100);

    for (;;) {
      try {
        int[][] prev = new int[19][19];
        for (;;) {
          int[][] next = screenDriver.screenshot();
          if (equals(prev, next)) {
            Thread.sleep(50);
          } else {
            boolean ok = bluetoothDriver.setAllLights(next);
            if (!ok) continue;
            prev = next;
            System.out.println(showBoard(next));
          }
        }
      } catch (BluetoothException e) {
        System.out.println(e.getMessage());
        bluetoothDriver.init(bluetoothCallback);
      }
    }
  }

  private static boolean equals(int[][] prev, int[][] next) {
    for (int i = 0; i < 19; ++i)
      for (int j = 0; j < 19; ++j)
        if (next[i][j] != prev[i][j])
          return false;
    return true;
  }

  private static String showBoard(int[][] board) {
    StringBuilder sb = new StringBuilder();
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
        else if (board[i][j] == WHITE)
          sb.append(" o");
        else if (board[i][j] == BLACK)
          sb.append(" x");
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

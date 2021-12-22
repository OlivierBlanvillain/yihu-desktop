import java.util.*;
import java.awt.AWTException;
import java.awt.Point;

public class Main {
  public static int EMPTY = 0;
  public static int BLACK = 1;
  public static int WHITE = 2;

  public static void main(String[] args) throws InterruptedException, AWTException {
    ScreenDriver screenDriver = new ScreenDriver();
    BluetoothDriver bluetoothDriver = new BluetoothDriver();

    bluetoothDriver.init((p) -> {
      System.out.println("Clicking " + showCoord(p));
      boolean ok = screenDriver.click(p.x, p.y);
      if (!ok)
        System.out.println("Clicking failed?");
    });

    bluetoothDriver.resetLights();

    screenDriver.init();
    Thread.sleep(500);

    int[][] prev = new int[19][19];
    for (;;) {
      int[][] next = screenDriver.screenshot();
      int d = diff(prev, next);
      Point newStone = singleNewStone(prev, next);
      if (d == 0) {
        Thread.sleep(200);
      } else if (newStone == null) {
        System.out.println(showBoard(next));
        System.out.println("Popup?");
        bluetoothDriver.setAllLights(next);
      } else {
        System.out.println("Lighting on " + Main.showCoord(newStone));
        int newColor = next[newStone.x][newStone.y];
        boolean ok = bluetoothDriver.setLight(newStone.x, newStone.y, newColor);
        if (!ok)
          continue;
        if (d > 1) {
          System.out.println("Capture");
          bluetoothDriver.setAllLights(next);
        }
        prev = next;
      }
    }
  }

  private static Point singleNewStone(int[][] prev, int[][] next) {
    Point stone = null;
    for (int i = 0; i < 19; ++i)
      for (int j = 0; j < 19; ++j)
        if (next[i][j] != EMPTY && next[i][j] != prev[i][j])
          if (stone == null)
            stone = new Point(i, j);
          else
            return null;
    return stone;
  }

  private static int diff(int[][] prev, int[][] next) {
    int count = 0;
    for (int i = 0; i < 19; ++i)
      for (int j = 0; j < 19; ++j)
        if (next[i][j] != prev[i][j])
          ++count;
    return count;
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

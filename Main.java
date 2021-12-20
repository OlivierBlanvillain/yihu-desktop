import java.util.*;
import java.awt.AWTException;

public class Main {
  public static int EMPTY = 0;
  public static int BLACK = 1;
  public static int WHITE = 2;

  public static void main(String[] args) throws InterruptedException, AWTException {
    ScreenDriver computer = new ScreenDriver();
    BluetoothDriver bluetooth = new BluetoothDriver();

    bluetooth.init((p) -> {
      System.out.println("Clicking   " + showCoord(p.x, p.y));
      boolean ok = computer.click(p.x, p.y);
      if (!ok)
        System.out.println("Clicking failed?");
    });

    computer.init();

    int[][] prev = new int[19][19];
    for (;;) {
      int[][] next = computer.capture();
      if (countNewStones(prev, next) >= 2) {
        System.out.println("Popup?");
        Thread.sleep(200);
        continue;
      }
      boolean updated = false;
      for (int i = 0; i < 19; ++i) {
        for (int j = 0; j < 19; ++j) {
          if (next[i][j] != prev[i][j]) {
            boolean ok = bluetooth.setLight(i, j, next[i][j]);
            if (ok)
              updated = true;
            else
              System.out.println("Bluetooth dead?");
          }
        }
      }
      prev = next;
      if (!updated)
        Thread.sleep(200);
    }
  }

  private static int countNewStones(int[][] prev, int[][] next) {
    int count = 0;
    for (int i = 0; i < 19; ++i) {
      for (int j = 0; j < 19; ++j) {
        if (next[i][j] != EMPTY && next[i][j] != prev[i][j])
          ++count;
      }
    }
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
      if (19 - j < 10)
        sb.append(' ');
      sb.append(String.valueOf(19 - j));
      sb.append('\n');
    }
    sb.append("   a b c d e f g h j k l m n o p q r s t\n");
    return sb.toString();
  }

  public static String showCoord(int x, int y) {
    return (char)('A' + x + (x > 7 ? 1 : 0)) + "" + (19 - y);
  }
}

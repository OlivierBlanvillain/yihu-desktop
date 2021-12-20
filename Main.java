import java.util.*;
import java.awt.AWTException;

public class Main {
  public static void main(String[] args) throws InterruptedException, AWTException {
    ComputerDriver computer = new ComputerDriver();
    BluetoothDriver bluetooth = new BluetoothDriver();

    bluetooth.init((p) -> {
      System.out.println("Clicking " + p.x + " " + p.y);
      computer.click(p.x, p.y);
    });

    int[][] prev = new int[19][19];
    for (;;) {
      int[][] next = computer.capture();
      if (countNewStones(prev, next) > 0)
        System.out.println("Popup?");
      boolean printed = false;
      for (int i = 0; i < 19; ++i) {
        for (int j = 0; j < 19; ++j) {
          if (next[i][j] != prev[i][j])
            bluetooth.setLight(i, j, next[i][j]);
            if (!printed) {
              printed = true;
              System.out.println(showBoard(next));
            }
          Thread.sleep(100);
        }
      }
      prev = next;
      Thread.sleep(200);
    }
  }

  private static int countNewStones(int[][] prev, int[][] next) {
    int count = 0;
    for (int i = 0; i < 19; ++i) {
      for (int j = 0; j < 19; ++j) {
        if (next[i][j] != 0 && next[i][j] != prev[i][j])
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
      for (int i = 0; i < 19; ++i) {
        if (19 - j < 10)
          sb.append(' ');
        sb.append(String.valueOf(19 - j));
        boolean oshiI = i == 3 || i == 9 || i == 15;
        boolean oshiJ = j == 3 || j == 9 || j == 15;
        if (board[i][j] == 0 && oshiI && oshiJ)
          sb.append(" +");
        else if (board[i][j] == 0)
          sb.append(" .");
        else if (board[i][j] == 1)
          sb.append(" o");
        else if (board[i][j] == 2)
          sb.append(" x");
        if (19 - j < 10)
          sb.append(' ');
        sb.append(String.valueOf(19 - j));
        sb.append('\n');
      }
    }
    sb.append("   a b c d e f g h j k l m n o p q r s t\n");
    return sb.toString();
  }
}

package yihuchess;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import javax.swing.*;
import static yihuchess.Config.*;

class ScreenDriver {
  Rectangle captureRectangle;
  private Object lock;
  private Robot robot;

  public ScreenDriver() throws AWTException {
    robot = new Robot();
    captureRectangle = new Rectangle(INIT_X, INIT_Y, INIT_W, INIT_H);
    lock = new Object();
  }

  public void init() throws InterruptedException {
    JFrame frame = new JFrame("Capture Area");
    Button button = new Button("Start screen capture!");
    frame.getContentPane().add(button);
    frame.setBounds(INIT_X, INIT_Y, INIT_W, INIT_H);
    frame.setVisible(true);
    frame.getBounds();
    Thread t = new Thread() {
      public void run() {
        synchronized(lock) {
          while (frame.isVisible())
            try {
              lock.wait();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }

        }
      }
    };
    t.start();
    button.addActionListener((e) -> {
      synchronized (lock) {
        captureRectangle = frame.getBounds();
        System.out.println("Screen capture coordinates: " + captureRectangle);
        frame.setVisible(false);
        frame.dispose();
        lock.notify();
      }
    });
    t.join();
  }

  public int[][] screenshot() {
    int[][] output = new int[19][19];
    BufferedImage img = robot.createScreenCapture(captureRectangle);
    for (int x = 0; x < 19; ++x) {
      for (int y = 0; y < 19; ++y) {
        int xCoord = (int)((captureRectangle.width - 1) / 18.0 * x);
        int yCoord = (int)((captureRectangle.height - 1) / 18.0 * y);
        int pixel = img.getRGB(xCoord, yCoord);
        int[] candidates = new int[] { GOBAN_COLOR, WHITE_COLOR, BLACK_COLOR };
        int closest = closestColor(pixel, candidates);
        if (closest == WHITE_COLOR)
          output[x][y] = Main.WHITE;
        else if (closest == BLACK_COLOR)
          output[x][y] = Main.BLACK;
      }
    }
    return output;
  }

  public int click(int x, int y) {
    int xCoord = captureRectangle.x + (int)(captureRectangle.width / 18.0 * x);
    int yCoord = captureRectangle.y + (int)(captureRectangle.height / 18.0 * y);
    for (int i = 0; i < 10; ++i) {
      robot.mouseMove(xCoord, yCoord);
      robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
      if (screenshot()[x][y] != Main.EMPTY)
        return i;
    }
    return 0;
  }

  private int closestColor(int pixel, int[] candidates) {
    assert(candidates.length != 0);
    int dist = Integer.MAX_VALUE;
    int best = 0;
    for (int r: candidates) {
      int d = colorDistance(pixel, r);
      if (d < dist) {
        dist = d;
        best = r;
      }
    }
    return best;
  }

  private int colorDistance(int c1, int c2) {
    int b1 = (c1 & 0x0000ff);
    int g1 = (c1 & 0x00ff00) >> 8;
    int r1 = (c1 & 0xff0000) >> 16;
    int b2 = (c2 & 0x0000ff);
    int g2 = (c2 & 0x00ff00) >> 8;
    int r2 = (c2 & 0xff0000) >> 16;
    int bb = (b1 - b2) * (b1 - b2);
    int gg = (g1 - g2) * (g1 - g2);
    int rr = (r1 - r2) * (r1 - r2);
    return bb + gg + rr;
  }
}

package yihuchess;

import java.awt.Robot;
import java.awt.Button;
import java.awt.Rectangle;
import java.awt.AWTException;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import static yihuchess.Config.*;

class Screen {
  Rectangle captureRectangle;
  Robot robot;
  Object lock;

  Screen(Robot robot) throws AWTException {
    this.captureRectangle = new Rectangle(INIT_X, INIT_Y, INIT_W, INIT_H);
    this.robot = robot;
    this.lock = new Object();
  }

  void init() {
    var frame = new JFrame("Capture Area");
    var button = new Button("Start screen capture!");
    frame.getContentPane().add(button);
    frame.setBounds(INIT_X, INIT_Y, INIT_W, INIT_H);
    frame.setVisible(true);
    frame.getBounds();
    var t = new Thread() {
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
    try {
      t.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  void sleep(int milli) {
    robot.delay(milli);
  }

  int[][] screenshot() {
    var output = new int[19][19];
    var img = robot.createScreenCapture(captureRectangle);
    for (var x = 0; x < 19; ++x) {
      for (var y = 0; y < 19; ++y) {
        var xCoord = (int)((captureRectangle.width - 1) / 18.0 * x);
        var yCoord = (int)((captureRectangle.height - 1) / 18.0 * y);
        var pixel = img.getRGB(xCoord, yCoord);
        var candidates = new int[] { GOBAN_COLOR, WHITE_COLOR, BLACK_COLOR };
        var closest = closestColor(pixel, candidates);
        if (closest == WHITE_COLOR)
          output[x][y] = 2;
        else if (closest == BLACK_COLOR)
          output[x][y] = 1;
      }
    }
    return output;
  }

  int click(int x, int y) {
    var xCoord = captureRectangle.x + (int)(captureRectangle.width / 18.0 * x);
    var yCoord = captureRectangle.y + (int)(captureRectangle.height / 18.0 * y);
    for (var i = 1; i <= MAX_CLICK_ATTEMPS; ++i) {
      robot.mouseMove(xCoord, yCoord);
      robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
      if (screenshot()[x][y] != 0)
        return i;
    }
    return 0;
  }

  int closestColor(int pixel, int[] candidates) {
    var dist = Integer.MAX_VALUE;
    var best = 0;
    for (var r: candidates) {
      var d = colorDistance(pixel, r);
      if (d < dist) {
        dist = d;
        best = r;
      }
    }
    return best;
  }

  int colorDistance(int c1, int c2) {
    var b1 = (c1 & 0x0000ff);
    var g1 = (c1 & 0x00ff00) >> 8;
    var r1 = (c1 & 0xff0000) >> 16;
    var b2 = (c2 & 0x0000ff);
    var g2 = (c2 & 0x00ff00) >> 8;
    var r2 = (c2 & 0xff0000) >> 16;
    var bb = (b1 - b2) * (b1 - b2);
    var gg = (g1 - g2) * (g1 - g2);
    var rr = (r1 - r2) * (r1 - r2);
    return bb + gg + rr;
  }
}

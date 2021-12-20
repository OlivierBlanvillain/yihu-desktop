import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import javax.swing.*;

class ComputerDriver {
  private static int INITIAL_X = 201;
  private static int INITIAL_Y = 57;
  private static int INITIAL_W = 649;
  private static int INITIAL_H = 649;

  private static int GOBAN_COLOR = -139130;
  private static int WHITE_COLOR = -1;
  private static int BLACK_COLOR = -7763575;

  Rectangle captureCoord = new Rectangle(INITIAL_X, INITIAL_Y, INITIAL_W, INITIAL_H);
  Robot robot;

  public ComputerDriver() throws AWTException {
    robot = new Robot();
  }

  public void init() {
    JFrame frame = new JFrame("");
    Button button = new Button("Start!");
    frame.getContentPane().add(button);
    frame.setUndecorated(true);
    frame.setBounds(INITIAL_X, INITIAL_Y, INITIAL_W, INITIAL_H);
    frame.setVisible(true);
    frame.getBounds();
    button.addActionListener((e) -> {
      frame.setVisible(false);
      captureCoord = frame.getBounds();
      System.out.println("Screen capture coordinates: " + captureCoord);
      frame.dispose();
    });
  }

  public int[][] capture() throws AWTException {
    int[][] output = new int[19][19];
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Rectangle screenRectangle = new Rectangle(screenSize);
    BufferedImage img = robot.createScreenCapture(screenRectangle);
    for (int x = 0; x < 19; ++x) {
      for (int y = 0; y < 19; ++y) {
        int xCoord = captureCoord.x + (int)(captureCoord.width / 18.0 * x);
        int yCoord = captureCoord.y + (int)(captureCoord.height / 18.0 * y);
        int pixel = img.getRGB(xCoord, yCoord);
        int[] candidates = new int[] { GOBAN_COLOR, WHITE_COLOR, BLACK_COLOR };
        int closest = closestColor(pixel, candidates);
        if (closest == WHITE_COLOR)
          output[x][y] = 1;
        else if (closest == BLACK_COLOR)
          output[x][y] = 2;
      }
    }
    return output;
  }

  public void click(int x, int y) {
    int xCoord = captureCoord.x + (int)(captureCoord.width / 18.0 * x);
    int yCoord = captureCoord.y + (int)(captureCoord.height / 18.0 * y);
    robot.mouseMove(xCoord, yCoord);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseMove(0, 0);
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
    int b1 = c1 & 0xff;
    int g1 = (c1 & 0xff00) >> 8;
    int r1 = (c1 & 0xff0000) >> 16;
    int b2 = c2 & 0xff;
    int g2 = (c2 & 0xff00) >> 8;
    int r2 = (c2 & 0xff0000) >> 16;
    int bb = (b1 - b2) * (b1 - b2);
    int gg = (g1 - g2) * (g1 - g2);
    int rr = (r1 - r2) * (r1 - r2);
    return bb + gg + rr;
  }
}

import tinyb.*;
import java.util.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class Main {
  static void println(String s) { System.out.println(s); }
  static void println() { System.out.println(); }
  static void print(String s) { System.out.print(s); }

  static void printDevice(BluetoothDevice device) {
    print("Address = " + device.getAddress());
    print(", Name = " + device.getName());
    print(", Connected = " + device.getConnected());
    println();
  }

  static BluetoothDevice getDevice(String address) throws InterruptedException {
    BluetoothManager manager = BluetoothManager.getBluetoothManager();
    BluetoothDevice device = null;
    for (int i = 0; i < 30; ++i) {
      for (BluetoothDevice d: manager.getDevices()) {
        printDevice(d);
        if (d.getAddress().equals(address))
          device = d;
      }
      if (device != null)
        return device;
      Thread.sleep(2000);
    }
    return null;
  }

  static BluetoothGattService getService(BluetoothDevice device, String UUID) throws InterruptedException {
    BluetoothGattService boardService = null;
    do {
      for (BluetoothGattService service: device.getServices()) {
        if (service.getUUID().equals(UUID))
          boardService = service;
      }
      Thread.sleep(1000);
    } while (device.getServices().isEmpty());
    return boardService;
  }

  static BluetoothGattCharacteristic getCharacteristic(BluetoothGattService service, String UUID) {
    for (BluetoothGattCharacteristic characteristic: service.getCharacteristics()) {
      if (characteristic.getUUID().equals(UUID))
        return characteristic;
    }
    return null;
  }

  static Rectangle capture;
  static Robot robot;

  public static void main(String[] args) throws InterruptedException, AWTException {
    robot = new Robot();
    JFrame frame = new JFrame("TitleLessJFrame");
    Button button = new Button("Start!");
    frame.getContentPane().add(button);
    frame.setUndecorated(true);
    frame.setBounds(201, 57, 649, 649);
    frame.setVisible(true);
    capture = frame.getBounds();
    button.addActionListener((e) -> {
      frame.setVisible(false);
      capture = frame.getBounds();
      println("Screen capture: " + capture);
      frame.dispose();
    });

    String deviceAddress = "B4:10:7B:24:1B:4B";
    BluetoothManager manager = BluetoothManager.getBluetoothManager();
    if (manager.startDiscovery())
      println("Bluetooth discovery started...");
    BluetoothDevice device = getDevice(deviceAddress);
    manager.stopDiscovery();
    if (device == null) {
      System.err.println("No device found with the provided address.");
      System.exit(-1);
    }
    print("Found bluetooth device: ");
    printDevice(device);
    if (!device.connect()) {
      println("Could not connect device.");
      System.exit(-1);
    }

    BluetoothGattService boardService = getService(device, "0000fff0-0000-1000-8000-00805f9b34fb");

    if (boardService == null) {
      System.err.println("This device does not have the service we are looking for.");
      device.disconnect();
      System.exit(-1);
    }
    println("Found service " + boardService.getUUID());

    BluetoothGattCharacteristic rblName = getCharacteristic(boardService,
      "0000fff1-0000-1000-8000-00805f9b34fb");
    BluetoothGattCharacteristic rblRx = getCharacteristic(boardService,
      "0000fff4-0000-1000-8000-00805f9b34fb");
    BluetoothGattCharacteristic rblKey = getCharacteristic(boardService,
      "0000fff2-0000-1000-8000-00805f9b34fb");
    BluetoothGattCharacteristic rblBat = getCharacteristic(boardService,
      "0000fff5-0000-1000-8000-00805f9b34fb");
    BluetoothGattCharacteristic rblRx2 = getCharacteristic(boardService,
      "0000fff3-0000-1000-8000-00805f9b34fb");

    if (rblName == null || rblRx == null || rblKey == null || rblBat == null || rblRx2 == null) {
      System.err.println("Could not find some of the characteristics.");
      device.disconnect();
      System.exit(-1);
    }
    println("Found characteristics");

    rblRx2.writeValue(allLightsOff());

    rblRx.enableValueNotifications((v) -> {
      if (v.length == 12) {
        assert(v[9] == getPosSendMagicJni(v[7], v[8], v[3], v[4], (byte)0));
        assert(v[10] == getPosSendMagicJni(v[7], v[8], v[3], v[4], (byte)1));
        v[2] = getPosSendMagicJni(v[7], v[8], v[3], v[4], (byte)2);
        v[4] = getPosSendMagicJni(v[7], v[8], v[3], v[4], (byte)3);
        v[3] = v[2];
        v[2] = (byte)5;
        int n2 = v[3] - 1;
        int n = 19 - v[4];
        println("before boardX:" + n2 + " boardY:" + n);
        assert(!(n2 > 17 || n2 % 2 != 1 || n > 19 || n % 2 != 0));
      }
    });

    while (true) {
      // Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      // Rectangle screenRectangle = new Rectangle(screenSize);
      // BufferedImage img = robot.createScreenCapture(screenRectangle);
      // Rectangle c = new Rectangle(451,128,397,578);
      // for (Point p: new Point[] { new Point(c.x, c.y),
      //                             new Point(c.x+c.width, c.y),
      //                             new Point(c.x, c.y+c.height),
      //                             new Point(c.x+c.width, c.y+c.height) }) {
      //   println(img.getRGB(p.x, p.y) + "");
      // }

      // Thread.sleep(1000);
      // println();

      int[][] s = screenshot();
      for (int y = 0; y < 19; ++y) {
        for (int x = 0; x < 19; ++x) {
          if (s[x][y] == 1)
            print(" o ");
          else if (s[x][y] == 2)
            print(" x ");
          else if (s[x][y] == 0)
            print(" . ");
          else
            print(" " + s[x][y] + " ");
        }
        println();
      }
      println();
      println();
      println();
      Thread.sleep(1000);
      // println();
      // Thread.sleep(1000);
      // println("Playing " + i + ", " + j);
      // rblRx2.writeValue(lightOn(i, j, (i+j)%2+1));
      // click(i, j);
      // i++;
      // if (i == 19) {
      //   i = 0;
      //   j++;
      // }
      // Thread.sleep(1000);
    }
  }

  static final int[][] screenshot() throws AWTException {
    int[][] output = new int[19][19];
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Rectangle screenRectangle = new Rectangle(screenSize);
    BufferedImage img = robot.createScreenCapture(screenRectangle);
    for (int x = 0; x < 19; ++x) {
      for (int y = 0; y < 19; ++y) {
        int xCoord = capture.x + (int)(capture.width / 18.0 * x);
        int yCoord = capture.y + (int)(capture.height / 18.0 * y);
        int avg = colorAvg(new int[] {
          img.getRGB(xCoord, yCoord),
          img.getRGB(xCoord+1, yCoord),
          img.getRGB(xCoord, yCoord+1),
          img.getRGB(xCoord-1, yCoord),
          img.getRGB(xCoord, yCoord-1),
          img.getRGB(xCoord+1, yCoord+1),
          img.getRGB(xCoord-1, yCoord-1),
          img.getRGB(xCoord+1, yCoord-1),
          img.getRGB(xCoord-1, yCoord+1)
        });
        // int b = avg & 0xff;
        // int g = (avg & 0xff00) >> 8;
        // int r = (avg & 0xff0000) >> 16;
        // int grayScale = (int)(0.299 * r + 0.587 * g + 0.114 * b);
        // output[x][y] = (byte)grayScale;
        int gobanColor = -139130;
        int whiteColor = -1;
        int blackColor = -7763575;

        int closest = closestColor(avg, new int[] { gobanColor, whiteColor, blackColor});
        if (closest == whiteColor)
          output[x][y] = 1;
        else if (closest == blackColor)
          output[x][y] = 2;
      }
    }
    return output;
  }

  static int colorAvg(int[] colors) {
    int b = 0;
    int g = 0;
    int r = 0;
    for (int color: colors) {
      b += color & 0xff;
      g += (color & 0xff00) >> 8;
      r += (color & 0xff0000) >> 16;
    }
    b /= colors.length;
    g /= colors.length;
    r /= colors.length;
    int result = b | (g << 8) | (r << 16);
    return result;
  }

  static int closestColor(int candidate, int[] references) {
    assert(references.length != 0);
    int closestD = Integer.MAX_VALUE;
    int closestR = 0;
    for (int r: references) {
      int d = colorDistance(candidate, r);
      if (d < closestD) {
        closestD = d;
        closestR = r;
      }
    }
    return closestR;
  }

  static int colorDistance(int c1, int c2) {
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

  static final void click(int x, int y) throws InterruptedException, AWTException {
    int xCoord = capture.x + (int)(capture.width / 18.0 * x);
    int yCoord = capture.y + (int)(capture.height / 18.0 * y);
    robot.mouseMove(xCoord, yCoord);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    Thread.sleep(100);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseMove(0, 0);
  }

  static final void lightOff(int x, int y) { lightOn(x, y, 0); }
  static final void lightOnWhite(int x, int y) { lightOn(x, y, 1); }
  static final void lightOnBlack(int x, int y) { lightOn(x, y, 2); }

  static final byte[] lightOn(int x, int y, int c) {
    byte magic = (byte)(((byte)x ^ (byte)y) << (byte)1);
    byte[] arrby = new byte[] { (byte)35, (byte)0x4C, (byte)14, 0, 0, 0, 0, 1, (byte)x, (byte)y, (byte)c, 0, magic, 0 };
    arrby[13] = checksum(arrby);
    return prefixed(arrby);
  }

  static final byte[] allLightsOff() {
    byte by = 0;
    byte by2 = (byte)(by ^ 38);
    byte[] arrby = new byte[]{(byte)35, (byte)67, (byte)11, (byte)0x34, 0, (byte)0x3b, 0, (byte)0x12, (byte)0x13, 0, 0};
    arrby[10] = checksum(arrby);
    return prefixed(arrby);
  }

  static byte checksum(byte[] arrby) {
    int n2 = 0;
    int n = arrby.length - 1;
    for (int i = 0; i < n + 0; ++i) {
      n2 = (byte)(n2 ^ arrby[i]);
    }
    return (byte)(n2 & 255);
  }

  static byte[] prefixed(byte[] arrby) {
    int n;
    assert(arrby.length != 0);
    int n2 = arrby.length <= 255 ? 1 : 2;
    byte[] arrby2 = new byte[arrby.length + n2];
    arrby2[0] = (byte)(arrby.length & 255);
    if (n2 == 2) {
      arrby2[1] = (byte)(255 & arrby.length >> 8);
    }
    for (n = 0; n < arrby.length; ++n) {
      arrby2[n + n2] = arrby[n];
    }
    int n3;
    n = arrby2.length;
    n3 = 19;
    if (n <= 19) {
      n3 = n;
    }
    arrby = new byte[n3 + 1];
    arrby[0] = n == arrby2.length ? (n2 == 2 ? (byte)-90 : (byte)-91) : (byte)-89;
    int n4 = 0;
    while (n4 < n3) {
      int n5 = n4 + 1;
      arrby[n5] = arrby2[arrby2.length - n + n4];
      n4 = n5;
    }
    return arrby;
  }


  public static byte getPosSendMagicJni(byte a0, byte a1, byte a2, byte a3, byte a4) {
    char cVar1;
    switch(a4) {
      case 0:
        return (byte)(int)((a1 | a0) ^ a2);
      case 1:
        return (byte)(int)(a2 & ((int)a0 << 1));
      case 2:
        cVar1 = (char)(-10);
        a1 = a0;
        break;
      case 3:
        cVar1 = (char)(-9);
        break;
      default:
        return '\0';
    }
    return (byte)(int)(cVar1 + (a2 ^ a1));
  }
}

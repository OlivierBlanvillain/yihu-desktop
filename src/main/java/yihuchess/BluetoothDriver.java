package yihuchess;

import tinyb.*;
import java.util.*;
import java.awt.Point;
import java.util.function.Consumer;
import static yihuchess.Config.*;

class BluetoothDriver {
  private BluetoothGattCharacteristic inChannel;
  private BluetoothGattCharacteristic outChannel;
  private BluetoothDevice device;

  public void init(Consumer<Point> callback) throws InterruptedException {
    if (device != null) {
      try { device.disconnect(); }
      catch (BluetoothException e) {}
      device = null;
    }
    if (inChannel != null) {
      try { inChannel.disableValueNotifications(); }
      catch (BluetoothException e) {}
      inChannel = null;
    }
    outChannel = null;

    BluetoothManager.getBluetoothManager().startDiscovery();
    boolean allSet = false;
    while (!allSet) {
      try {
        allSet = initOnce(callback);
      } catch (BluetoothException e) {
        e.printStackTrace();
        Thread.sleep(BLUETOOTH_RETRY_PERIOD_MILLI);
      }
    }
    BluetoothManager.getBluetoothManager().stopDiscovery();
  }

  boolean err(String msg) {
    System.out.println(msg);
    return false;
  }

  public boolean initOnce(Consumer<Point> callback) throws InterruptedException {
    System.out.println("Waiting for Bluetooth device...");
    device = getDevice(GOBAN_MAC_ADDRESS);
    if (device == null)
      return err("Device not found.");
    if (!connect())
      return err("Could not connect to the device.");
    System.out.println("Connected to " + device.getName() + ".");

    if(!resolveServices())
      return err("Service resolution failed.");
    var boardService = getService("0000fff0-0000-1000-8000-00805f9b34fb");
    if (boardService == null)
      return err("Service not found.");
    System.out.println("Service found.");

    inChannel = getCharacteristic(boardService, "0000fff4-0000-1000-8000-00805f9b34fb");
    outChannel = getCharacteristic(boardService, "0000fff3-0000-1000-8000-00805f9b34fb");
    if (this.inChannel == null || this.outChannel == null)
      return err("Characteristics not found.");
    System.out.println("Characteristics found.");

    inChannel.enableValueNotifications((arrby) -> {
      if (arrby.length == 12)
        callback.accept(decodeMovePacket(arrby));
    });

    return true;
  }

  BluetoothDevice getDevice(String address) throws InterruptedException {
    var manager = BluetoothManager.getBluetoothManager();
    for (var i = 0; i < BLUETOOTH_RETRIES; ++i) {
      for (var d: manager.getDevices())
        if (d.getAddress().equals(address))
          return d;
      Thread.sleep(BLUETOOTH_RETRY_PERIOD_MILLI);
    }
    return null;
  }

  boolean connect() {
    return device.connect();
  }

  boolean isConnected() {
    return device.getConnected();
  }

  boolean resolveServices() throws InterruptedException {
    for (var i = 0; i < BLUETOOTH_RETRIES; ++i) {
      if (device.getServicesResolved())
        return true;
      Thread.sleep(BLUETOOTH_RETRY_PERIOD_MILLI);
    }
    return false;
  }

  BluetoothGattService getService(String uuid) throws InterruptedException {
    for (var i = 0; i < BLUETOOTH_RETRIES; ++i) {
      for (var s: device.getServices())
        if (s.getUUID().equals(uuid))
          return s;
      Thread.sleep(BLUETOOTH_RETRY_PERIOD_MILLI);
    }
    return null;
  }

  BluetoothGattCharacteristic getCharacteristic(BluetoothGattService service, String uuid) throws InterruptedException {
    for (var i = 0; i < BLUETOOTH_RETRIES; ++i) {
      for (var c: service.getCharacteristics())
        if (c.getUUID().equals(uuid))
          return c;
      Thread.sleep(BLUETOOTH_RETRY_PERIOD_MILLI);
    }
    return null;
  }

  public boolean setAllLights(int[][] board, boolean unicolor) {
    assert(board.length == 19);
    for (int[] b: board)
      assert(b.length == 19);
    int[] flat = new int[19 * 19];
    for (int i = 0; i < 19; ++i)
      for (int j = 0; j < 19; ++j) {
        var s = board[i][18-j];
        if (unicolor && s != Main.EMPTY)
          s = Main.BLACK | Main.WHITE;
        flat[i+19*j] = s;
      }
    return sendSliced(allLightHeaders(allLightsPayload(flat)));
  }

  private void setChecksum(byte[] arrby) {
    int n2 = 0;
    int n = arrby.length - 1;
    for (int i = 0; i < n + 0; ++i)
      n2 = (byte)(n2 ^ arrby[i]);
    arrby[n] = (byte)(n2 & 255);
  }

  private boolean sendSliced(byte[] arrby) {
    int n;
    assert(arrby.length != 0);
    int n2 = arrby.length <= 255 ? 1 : 2;
    byte[] arrby2 = new byte[arrby.length + n2];
    arrby2[0] = (byte)(arrby.length & 255);
    if (n2 == 2)
      arrby2[1] = (byte)(255 & arrby.length >> 8);
    for (n = 0; n < arrby.length; ++n)
      arrby2[n + n2] = arrby[n];
    int n3;
    n = arrby2.length;

    for (n = arrby2.length; n > 0; n -= n3) {
      n3 = 19;
      if (n <= 19)
        n3 = n;
      arrby = new byte[n3 + 1];
      arrby[0] = n == arrby2.length ? (n2 == 2 ? (byte)-90 : (byte)-91) : (byte)-89;
      int n4 = 0;
      while (n4 < n3) {
        int n5 = n4 + 1;
        arrby[n5] = arrby2[arrby2.length - n + n4];
        n4 = n5;
      }
      if (outChannel == null || !isConnected())
        return false;
      var ok = outChannel.writeValue(arrby);
      if (!ok)
        return false;
    }
    return true;
  }

  private Point decodeMovePacket(byte[] a) {
    byte magic0 = (byte)(int)((a[8] | a[7]) ^ a[3]);
    byte magic1 = (byte)(int)(a[3] & ((int)a[7] << 1));
    byte magic2 = (byte)(int)((char)(-10) + (a[3] ^ a[7]));
    byte magic3 = (byte)(int)((char)(-9) + (a[3] ^ a[8]));
    assert(a[9] == magic0 && a[10] == magic1);
    a[2] = magic2;
    a[4] = magic3;
    a[3] = a[2];
    a[2] = (byte)5;
    int n2 = a[3] - 1;
    int n = 19 - a[4];
    assert(!(n2 > 17 || n2 % 2 != 1 || n > 19 || n % 2 != 0));
    return new Point(n2, n);
  }

  private byte[] allLightHeaders(byte[] object) {
    byte[] arrby = new byte[105];
    int n = 0;
    arrby[0] = (byte)35;
    arrby[1] = (byte)66;
    arrby[2] = (byte)105;
    arrby[3] = (byte)0x16;
    arrby[4] = (byte)0x00;
    arrby[5] = (byte)0x24;
    arrby[6] = (byte)0x00;
    int n3 = 7;
    while (n < object.length) {
      arrby[n3] = object[n];
      ++n;
      ++n3;
    }
    byte magic = (byte)(arrby[20] ^ (arrby[4] | arrby[3]));
    arrby[n3] = magic;
    assert(n3 + 1 == 104);
    setChecksum(arrby);
    return arrby;
  }

  private byte[] allLightsPayload(int[] board) {
    int n;
    int n2 = 0;
    byte[] arrby = new byte[96];
    for (n = 0; n < 96; ++n)
      arrby[n] = 0;
    arrby[0] = 1;
    n = 0;
    while (n < 19) {
      int n3 = n * 5 + 1;
      n2 = n + 342 + 1;
      for (int i = 18; i >= 0; --i) {
        int object;
        int n4;
        if (i <= 18 && i > 10) {
          object = board[n2-1];
          if (object != 1) {
            if (object != 2) {
              if (object == 3) {
                n4 = arrby[n3];
                object = 1 << 18 - i;
                arrby[n3] = (byte)(n4 | object);
                n4 = n3 + 3;
                arrby[n4] = (byte)(object | arrby[n4]);
              }
            } else {
              object = n3 + 3;
              arrby[object] = (byte)(arrby[object] | 1 << 18 - i);
            }
          } else {
            arrby[n3] = (byte)(arrby[n3] | 1 << 18 - i);
          }
        }
        if (i <= 10 && i > 2) {
          object = board[n2-1];
          if (object != 1) {
            if (object != 2) {
              if (object == 3) {
                int n5 = n3 + 1;
                n4 = arrby[n5];
                object = 1 << 10 - i;
                arrby[n5] = (byte)(n4 | object);
                n4 = n3 + 4;
                arrby[n4] = (byte)(object | arrby[n4]);
              }
            } else {
              object = n3 + 4;
              arrby[object] = (byte)(1 << 10 - i | arrby[object]);
            }
          } else {
            object = n3 + 1;
            arrby[object] = (byte)(1 << 10 - i | arrby[object]);
          }
        }
        if (i <= 2) {
          object = board[n2-1];
          if (object != 1) {
            if (object != 2) {
              if (object == 3) {
                object = n3 + 2;
                arrby[object] = (byte)(arrby[object] | 1 << 2 - i);
                arrby[object] = (byte)(arrby[object] | 1 << 7 - i);
              }
            } else {
              object = n3 + 2;
              arrby[object] = (byte)(arrby[object] | 1 << 7 - i);
            }
          } else {
            object = n3 + 2;
            arrby[object] = (byte)(arrby[object] | 1 << 2 - i);
          }
        }
        n2 -= 19;
      }
      ++n;
    }
    return arrby;
  }
}

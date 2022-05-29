import tinyb.*;
import java.util.*;
import java.awt.Point;
import java.util.function.Consumer;

class BluetoothDriver {
  private static final String GOBAN_MAC_ADDRESS = "B4:10:7B:24:1B:4B";

  private BluetoothGattCharacteristic inChannel;
  private BluetoothGattCharacteristic outChannel;

  public void init(Consumer<Point> callback) throws InterruptedException {
    boolean allSet = false;
    while (!allSet) {
      try {
        allSet = initOnce(callback);
      } catch (BluetoothException e) {
        System.out.println(e.getMessage());
        Thread.sleep(1000);
      }
    }
  }

  public boolean initOnce(Consumer<Point> callback) throws InterruptedException {
    BluetoothManager manager = BluetoothManager.getBluetoothManager();
    if (manager.startDiscovery())
      System.out.println("Waiting for Bluetooth device...");
    BluetoothDevice device = getDevice(GOBAN_MAC_ADDRESS);
    manager.stopDiscovery();
    if (device == null) {
      System.err.println("Device not found.");
      return false;
    }
    if (device.connect()) {
      System.out.println("Connected to " + GOBAN_MAC_ADDRESS + ".");
    } else {
      System.out.println("Could not connect device.");
      return false;
    }

    BluetoothGattService boardService = getService(device, "0000fff0-0000-1000-8000-00805f9b34fb");
    if (boardService == null) {
      System.err.println("Service not found.");
      device.disconnect();
      return false;
    } else {
      System.out.println("Found service " + boardService.getUUID() + ".");
    }

    // BluetoothGattCharacteristic rblName = getCharacteristic(boardService,
      // "0000fff1-0000-1000-8000-00805f9b34fb");
    // BluetoothGattCharacteristic rblKey = getCharacteristic(boardService,
      // "0000fff2-0000-1000-8000-00805f9b34fb");
    // BluetoothGattCharacteristic rblBat = getCharacteristic(boardService,
      // "0000fff5-0000-1000-8000-00805f9b34fb");
    inChannel = getCharacteristic(boardService,
      "0000fff4-0000-1000-8000-00805f9b34fb");
    outChannel = getCharacteristic(boardService,
      "0000fff3-0000-1000-8000-00805f9b34fb");
    if (this.inChannel == null || this.outChannel == null) {
      System.err.println("Characteristics not found.");
      device.disconnect();
      return false;
    } else {
      System.out.println("Found characteristics.");
    }

    inChannel.enableValueNotifications((arrby) -> {
      if (arrby.length == 12)
        callback.accept(decodeMovePacket(arrby));
    });

    resetLights();

    return true;
  }

  BluetoothDevice getDevice(String address) throws InterruptedException {
    BluetoothManager manager = BluetoothManager.getBluetoothManager();
    BluetoothDevice device = null;
    for (int i = 0; i < 30; ++i) {
      for (BluetoothDevice d: manager.getDevices()) {
        if (d.getAddress().equals(address))
          device = d;
      }
      if (device != null)
        return device;
      Thread.sleep(1000);
    }
    return null;
  }

  BluetoothGattService getService(BluetoothDevice device, String UUID) throws InterruptedException {
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

  BluetoothGattCharacteristic getCharacteristic(BluetoothGattService service, String UUID) {
    for (BluetoothGattCharacteristic characteristic: service.getCharacteristics()) {
      if (characteristic.getUUID().equals(UUID))
        return characteristic;
    }
    return null;
  }

  public boolean resetLights() {
    byte[] arrby = new byte[] {
      (byte)35,
      (byte)67,
      (byte)11,
      (byte)0x34,
      (byte)0,
      (byte)0x3b,
      (byte)0,
      (byte)0x12,
      (byte)0x13,
      (byte)0,
      (byte)0
    };
    setChecksum(arrby);
    return sendSliced(arrby);
  }

  public boolean setAllLights(int[][] board) {
    assert(board.length == 19);
    for (int[] b: board) {
      assert(b.length == 19);
    }
    int[] flat = new int[19 * 19];
    for (int i = 0; i < 19; ++i) {
      for (int j = 0; j < 19; ++j) {
        flat[i+19*j] = board[i][18 - j];
      }
    }
    return sendSliced(allLightHeaders(allLightsPayload(flat)));
  }

  private void setChecksum(byte[] arrby) {
    int n2 = 0;
    int n = arrby.length - 1;
    for (int i = 0; i < n + 0; ++i) {
      n2 = (byte)(n2 ^ arrby[i]);
    }
    arrby[n] = (byte)(n2 & 255);
  }

  private boolean sendSliced(byte[] arrby) {
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

    for (n = arrby2.length; n > 0; n -= n3) {
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
      if (outChannel != null) {
        for (n4 = 4; n4 >= 0 && !outChannel.writeValue(arrby); --n4) {
        }
        if (n4 >= 0) continue;
      }
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
    for (n = 0; n < 96; ++n) {
      arrby[n] = 0;
    }
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

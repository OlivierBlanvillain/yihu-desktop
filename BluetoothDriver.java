import tinyb.*;
import java.util.*;
import java.awt.Point;
import java.util.function.Consumer;

class BluetoothDriver {
  private static String GOBAN_MAC_ADDRESS = "B4:10:7B:24:1B:4B";

  BluetoothGattCharacteristic inChannel;
  BluetoothGattCharacteristic outChannel;

  public void init(Consumer<Point> callback) throws InterruptedException {
    BluetoothManager manager = BluetoothManager.getBluetoothManager();
    if (manager.startDiscovery())
      System.out.println("Bluetooth discovery started...");
    BluetoothDevice device = getDevice(GOBAN_MAC_ADDRESS);
    manager.stopDiscovery();
    if (device == null) {
      System.err.println("Device not found.");
      System.exit(-1);
    }
    if (device.connect()) {
      System.out.println("Connected to " + GOBAN_MAC_ADDRESS + ".");
    } else {
      System.out.println("Could not connect device.");
      System.exit(-1);
    }

    BluetoothGattService boardService = getService(device, "0000fff0-0000-1000-8000-00805f9b34fb");
    if (boardService == null) {
      System.err.println("Service not found.");
      device.disconnect();
      System.exit(-1);
    } else {
      System.out.println("Found service " + boardService.getUUID());
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
      System.exit(-1);
    } else {
      System.out.println("Found characteristics");
    }

    outChannel.writeValue(reset());

    inChannel.enableValueNotifications((arrby) -> {
      if (arrby.length == 12)
        callback.accept(decodeMovePacket(arrby));
    });
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
      Thread.sleep(2000);
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

  public boolean setLight(int x, int y, int c) {
    String prefix = c == 0 ? "Turning off " : "Lighting on ";
    System.out.println(prefix + Main.showCoord(x, y));
    if (outChannel != null) {
      for (int i = 0; i < 4; ++i) {
        boolean ok = outChannel.writeValue(light(x, 18 - y, c));
        if (ok)
          return true;
      }
    }
    return false;
  }

  private byte[] reset() {
    byte by = 0;
    byte by2 = (byte)(by ^ 38);
    byte[] arrby = new byte[]{(byte)35, (byte)67, (byte)11, (byte)0x34, 0, (byte)0x3b, 0, (byte)0x12, (byte)0x13, 0, 0};
    arrby[10] = checksum(arrby);
    return prefixed(arrby);
  }

  private byte[] light(int x, int y, int c) {
    byte magic = (byte)(((byte)x ^ (byte)y) << (byte)1);
    byte[] arrby = new byte[] { (byte)35, (byte)0x4C, (byte)14, 0, 0, 0, 0, 1, (byte)x, (byte)y, (byte)c, 0, magic, 0 };
    arrby[13] = checksum(arrby);
    return prefixed(arrby);
  }

  private byte checksum(byte[] arrby) {
    int n2 = 0;
    int n = arrby.length - 1;
    for (int i = 0; i < n + 0; ++i) {
      n2 = (byte)(n2 ^ arrby[i]);
    }
    return (byte)(n2 & 255);
  }

  private byte[] prefixed(byte[] arrby) {
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

  private Point decodeMovePacket(byte[] a) {
    assert(a[9] == getPosSendMagicJni(a[7], a[8], a[3], a[4], (byte)0));
    assert(a[10] == getPosSendMagicJni(a[7], a[8], a[3], a[4], (byte)1));
    a[2] = getPosSendMagicJni(a[7], a[8], a[3], a[4], (byte)2);
    a[4] = getPosSendMagicJni(a[7], a[8], a[3], a[4], (byte)3);
    a[3] = a[2];
    a[2] = (byte)5;
    int n2 = a[3] - 1;
    int n = 19 - a[4];
    assert(!(n2 > 17 || n2 % 2 != 1 || n > 19 || n % 2 != 0));
    return new Point(n2, n);
  }

  private byte getPosSendMagicJni(byte a0, byte a1, byte a2, byte a3, byte a4) {
    switch(a4) {
      case 0:  return (byte)(int)((a1 | a0) ^ a2);
      case 1:  return (byte)(int)(a2 & ((int)a0 << 1));
      case 2:  return (byte)(int)((char)(-10) + (a2 ^ a0));
      case 3:  return (byte)(int)((char)(-9) + (a2 ^ a1));
      default: return 0;
    }
  }
}

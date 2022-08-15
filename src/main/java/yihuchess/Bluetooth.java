package yihuchess;

import java.awt.Point;
import java.io.IOException;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;
import static yihuchess.Config.*;
import tinyb.*;

class Bluetooth {
  BluetoothGattCharacteristic incoming;
  BluetoothGattCharacteristic outgoing;
  BluetoothGattService service;
  BluetoothManager manager;
  BluetoothDevice device;

  Bluetooth(BluetoothManager manager) {
    this.manager = manager;
  }

  void init(Consumer<Point> callback) {
    manager.startDiscovery();
    for (;;) {
      try {
        initOnce(callback);
        break;
      } catch (BluetoothException e) {
        System.out.println(e.getMessage());
      } catch (IOException e) {
        System.out.println(e.getMessage());
      }
    }
    manager.stopDiscovery();
  }

  void initOnce(Consumer<Point> callback) throws IOException {
    System.out.println("Waiting for Bluetooth device...");
    device = find(GOBAN_MAC_ADDRESS, "Device not found.");
    if (!device.connect())
      throw new IOException("Could not connect to the device.");
    System.out.println("Connected to " + device.getName());

    service = find(SERVICE_UUID, "Service not found.");
    System.out.println("Service found");

    incoming = find(INCOMING_UUID, "Characteristics not found.");
    outgoing = find(OUTGOING_UUID, "Characteristics not found.");
    System.out.print("Characteristics found");

    incoming.enableValueNotifications((arr) -> {
      try {
        if (arr.length == 12)
          callback.accept(decodeMovePacket(arr));
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    });
  }

  <T extends BluetoothObject> T find(String id, String msg) throws IOException {
    var timeout = Duration.ofMillis(BLUETOOTH_TIMEOUT_MILLI);
    var result = manager.<T>find(null, id, null, timeout);
    if (result == null)
      throw new IOException(msg);
    else
      return result;
  }

  boolean connect() {
    return device.connect();
  }

  boolean isConnected() {
    return device.getConnected();
  }

  boolean setLight(int x, int y, int c) {
    y = 18 - y;
    var arr = new byte[14];
    arr[0] = 35;
    arr[1] = 0x4C;
    arr[2] = 14;
    arr[7] = 1;
    arr[8] = (byte)x;
    arr[9] = (byte)y;
    arr[10] = (byte)c;
    arr[12] = (byte)((x ^ y) << 1);
    setChecksum(arr);
    return sendSliced(arr);
  }

  boolean setAllLights(int[][] board) {
    var flat = new int[19 * 19];
    for (var i = 0; i < 19; ++i)
      for (var j = 0; j < 19; ++j)
        flat[i+19*j] = board[i][18-j];
    return sendSliced(allLightHeaders(allLightsPayload(flat)));
  }

  void setChecksum(byte[] arr) {
    var n2 = 0;
    var n = arr.length - 1;
    for (var i = 0; i < n + 0; ++i)
      n2 = (byte)(n2 ^ arr[i]);
    arr[n] = (byte)(n2 & 255);
  }

  boolean sendSliced(byte[] arr) {
    var arr2 = new byte[arr.length + 1];
    arr2[0] = (byte)(arr.length & 255);
    for (var n = 0; n < arr.length; ++n)
      arr2[n + 1] = arr[n];
    var n2 = 0;
    for (int n = arr2.length; n > 0; n -= n2) {
      n2 = Math.min(n, 19);
      arr = new byte[n2 + 1];
      arr[0] = (byte)(n == arr2.length ? -91 : -89);
      for (int n4 = 0; n4 < n2; ++n4)
        arr[n4 + 1] = arr2[arr2.length - n + n4];
      var ok = outgoing.writeValue(arr);
      if (!ok)
        return false;
    }
    return true;
  }

  Point decodeMovePacket(byte[] arr) {
    var magic2 = (byte)((char)(-10) + (arr[3] ^ arr[7])) - 1;
    var magic3 = 19 - (byte)((char)(-9) + (arr[3] ^ arr[8]));
    return new Point(magic2, magic3);
  }

  byte[] allLightHeaders(byte[] object) {
    var arr = new byte[105];
    arr[0] = 35;
    arr[1] = 66;
    arr[2] = 105;
    arr[3] = 0x16;
    arr[5] = 0x24;
    var n2 = 7;
    for (var n = 0; n < object.length; ++n)
      arr[n2++] = object[n];
    arr[n2] = (byte)(arr[20] ^ (arr[4] | arr[3]));
    setChecksum(arr);
    return arr;
  }

  byte[] allLightsPayload(int[] board) {
    var arr = new byte[96];
    arr[0] = 1;
    for (var n = 0; n < 19; ++n) {
      var n2 = n + 342;
      var n3 = n * 5 + 1;
      for (var i = 18; i >= 0; --i) {
        var object = board[n2];
        if (i > 10) {
          if ((object & 1) == 1)
            arr[n3] |= 1 << 18 - i;
          if ((object & 2) == 2)
            arr[n3 + 3] |= 1 << 18 - i;
        } else if (i <= 10 && i > 2) {
          if ((object & 1) == 1)
            arr[n3 + 1] |= 1 << 10 - i;
          if ((object & 2) == 2)
            arr[n3 + 4] |= 1 << 10 - i;
        } else if (i <= 2) {
          if ((object & 1) == 1)
            arr[n3 + 2] |= 1 << 2 - i;
          if ((object & 2) == 2)
            arr[n3 + 2] |= 1 << 7 - i;
        }
        n2 -= 19;
      }
    }
    return arr;
  }
}

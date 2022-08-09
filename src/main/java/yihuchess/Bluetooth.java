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
    System.out.println("\nWaiting for Bluetooth device...");
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

  boolean setAllLights(int[][] board) {
    var flat = new int[19 * 19];
    for (var i = 0; i < 19; ++i)
      for (var j = 0; j < 19; ++j) {
        var s = board[i][18-j];
        if (UNICOLOR && s != 0)
          s = 3;
        flat[i+19*j] = s;
      }
    return sendSliced(allLightHeaders(allLightsPayload(flat)));
  }

  void setChecksum(byte[] arrby) {
    var n2 = 0;
    var n = arrby.length - 1;
    for (var i = 0; i < n + 0; ++i)
      n2 = (byte)(n2 ^ arrby[i]);
    arrby[n] = (byte)(n2 & 255);
  }

  boolean sendSliced(byte[] arrby) {
    var n2 = arrby.length <= 255 ? 1 : 2;
    var arrby2 = new byte[arrby.length + n2];
    arrby2[0] = (byte)(arrby.length & 255);
    if (n2 == 2)
      arrby2[1] = (byte)(255 & arrby.length >> 8);
    for (var n = 0; n < arrby.length; ++n)
      arrby2[n + n2] = arrby[n];
    var n = arrby2.length;
    for (int n3; n > 0; n -= n3) {
      n3 = 19;
      if (n <= 19)
        n3 = n;
      arrby = new byte[n3 + 1];
      arrby[0] = n == arrby2.length ? (byte)(n2 == 2 ? -90 : -91) : -89;
      var n4 = 0;
      while (n4 < n3) {
        var n5 = n4 + 1;
        arrby[n5] = arrby2[arrby2.length - n + n4];
        n4 = n5;
      }
      var ok = outgoing.writeValue(arrby);
      if (!ok)
        return false;
    }
    return true;
  }

  Point decodeMovePacket(byte[] a) {
    var magic2 = (byte)((char)(-10) + (a[3] ^ a[7]));
    var magic3 = (byte)((char)(-9) + (a[3] ^ a[8]));
    var n2 = magic2 - 1;
    var n = 19 - magic3;
    return new Point(n2, n);
  }

  byte[] allLightHeaders(byte[] object) {
    var arrby = new byte[105];
    var n = 0;
    arrby[0] = 35;
    arrby[1] = 66;
    arrby[2] = 105;
    arrby[3] = 0x16;
    arrby[4] = 0x00;
    arrby[5] = 0x24;
    arrby[6] = 0x00;
    var n3 = 7;
    while (n < object.length) {
      arrby[n3] = object[n];
      ++n;
      ++n3;
    }
    var magic = (byte)(arrby[20] ^ (arrby[4] | arrby[3]));
    arrby[n3] = magic;
    setChecksum(arrby);
    return arrby;
  }

  byte[] allLightsPayload(int[] board) {
    var n2 = 0;
    var arrby = new byte[96];
    for (var n = 0; n < 96; ++n)
      arrby[n] = 0;
    arrby[0] = 1;
    var n = 0;
    while (n < 19) {
      var n3 = n * 5 + 1;
      n2 = n + 342 + 1;
      for (var i = 18; i >= 0; --i) {
        var object = board[n2-1];
        if (i <= 18 && i > 10) {
          if (object != 1) {
            if (object != 2) {
              if (object == 3) {
                var n4 = (int) arrby[n3];
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
          if (object != 1) {
            if (object != 2) {
              if (object == 3) {
                var n5 = n3 + 1;
                var n4 = (int) arrby[n5];
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

import tinyb.*;
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.TimeUnit;

public class HelloTinyB {
  private static final float SCALE_LSB = 0.03125f;
  static boolean running = true;

  static void printDevice(BluetoothDevice device) {
    System.out.print("Address = " + device.getAddress());
    System.out.print(" Name = " + device.getName());
    System.out.print(" Connected = " + device.getConnected());
    System.out.println();
  }

  static float convertCelsius(int raw) {
    return raw / 128f;
  }

  /*
   * After discovery is started, new devices will be detected. We can get a list of all devices through the manager's
   * getDevices method. We can the look through the list of devices to find the device with the MAC which we provided
   * as a parameter. We continue looking until we find it, or we try 15 times (1 minutes).
   */
  static BluetoothDevice getDevice(String address) throws InterruptedException {
    BluetoothManager manager = BluetoothManager.getBluetoothManager();
    BluetoothDevice sensor = null;
    for (int i = 0; (i < 15) && running; ++i) {
      List<BluetoothDevice> list = manager.getDevices();
      if (list == null)
        return null;

      for (BluetoothDevice device : list) {
        printDevice(device);
        /*
         * Here we check if the address matches.
         */
        if (device.getAddress().equals(address))
          sensor = device;
      }

      if (sensor != null) {
        return sensor;
      }
      Thread.sleep(4000);
    }
    return null;
  }

  /*
   * Our device should expose a temperature service, which has a UUID we can find out from the data sheet. The service
   * description of the SensorTag can be found here:
   * http://processors.wiki.ti.com/images/a/a8/BLE_SensorTag_GATT_Server.pdf. The service we are looking for has the
   * short UUID AA00 which we insert into the TI Base UUID: f000XXXX-0451-4000-b000-000000000000
   */
  static BluetoothGattService getService(BluetoothDevice device, String UUID) throws InterruptedException {
    System.out.println("Services exposed by device:");
    BluetoothGattService boardService = null;
    List<BluetoothGattService> bluetoothServices = null;
    do {
      bluetoothServices = device.getServices();
      if (bluetoothServices == null)
        return null;

      for (BluetoothGattService service : bluetoothServices) {
        System.out.println("UUID: " + service.getUUID());
        if (service.getUUID().equals(UUID))
          boardService = service;
      }
      Thread.sleep(4000);
    } while (bluetoothServices.isEmpty() && running);
    return boardService;
  }

  static BluetoothGattCharacteristic getCharacteristic(BluetoothGattService service, String UUID) {
    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
    if (characteristics == null)
      return null;

    for (BluetoothGattCharacteristic characteristic : characteristics) {
      if (characteristic.getUUID().equals(UUID))
        return characteristic;
    }
    return null;
  }

  /*
   * This program connects to a TI SensorTag 2.0 and reads the temperature characteristic exposed by the device over
   * Bluetooth Low Energy. The parameter provided to the program should be the MAC address of the device.
   *
   * A wiki describing the sensor is found here: http://processors.wiki.ti.com/index.php/CC2650_SensorTag_User's_Guide
   *
   * The API used in this example is based on TinyB v0.3, which only supports polling, but v0.4 will introduce a
   * simplied API for discovering devices and services.
   */
  public static void main(String[] args) throws InterruptedException {

    String deviceAddress = "B4:10:7B:24:1B:4B";

    /*
     * To start looking of the device, we first must initialize the TinyB library. The way of interacting with the
     * library is through the BluetoothManager. There can be only one BluetoothManager at one time, and the
     * reference to it is obtained through the getBluetoothManager method.
     */
    BluetoothManager manager = BluetoothManager.getBluetoothManager();

    /*
     * The manager will try to initialize a BluetoothAdapter if any adapter is present in the system. To initialize
     * discovery we can call startDiscovery, which will put the default adapter in discovery mode.
     */
    boolean discoveryStarted = manager.startDiscovery();

    System.out.println("The discovery started: " + (discoveryStarted ? "true" : "false"));
    BluetoothDevice sensor = getDevice(deviceAddress);

    /*
     * After we find the device we can stop looking for other devices.
     */
    try {
      manager.stopDiscovery();
    } catch (BluetoothException e) {
      System.err.println("Discovery could not be stopped.");
    }

    if (sensor == null) {
      System.err.println("No sensor found with the provided address.");
      System.exit(-1);
    }

    System.out.print("Found device: ");
    printDevice(sensor);

    if (sensor.connect())
      System.out.println("Device with the provided address connected");
    else {
      System.out.println("Could not connect device.");
      System.exit(-1);
    }

    Lock lock = new ReentrantLock();
    Condition cv = lock.newCondition();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        running = false;
        lock.lock();
        try {
          cv.signalAll();
        } finally {
          lock.unlock();
        }

      }
    });


    BluetoothGattService boardService = getService(sensor, "0000fff0-0000-1000-8000-00805f9b34fb");

    if (boardService == null) {
      System.err.println("This device does not have the service we are looking for.");
      sensor.disconnect();
      System.exit(-1);
    }
    System.out.println("Found service " + boardService.getUUID());

    BluetoothGattCharacteristic rblName = getCharacteristic(boardService,
      "0000fff1-0000-1000-8000-00805f9b34fb");
    BluetoothGattCharacteristic rblRx = getCharacteristic(boardService,
      "0000fff4-0000-1000-8000-00805f9b34fb"); // not
    BluetoothGattCharacteristic rblKey = getCharacteristic(boardService,
      "0000fff2-0000-1000-8000-00805f9b34fb"); // not
    BluetoothGattCharacteristic rblBat = getCharacteristic(boardService,
      "0000fff5-0000-1000-8000-00805f9b34fb"); // not
    BluetoothGattCharacteristic rblRx2 = getCharacteristic(boardService,
      "0000fff3-0000-1000-8000-00805f9b34fb");

    if (rblName == null || rblRx == null || rblKey == null || rblBat == null || rblRx2 == null) {
      System.err.println("Could not find some of the characteristics.");
      sensor.disconnect();
      System.exit(-1);
    }
    System.out.println("Found the characteristics");

    byte[] raw = rblName.readValue();
    System.out.println("Name = " + new String(raw));

    // "raw = {");
    // for (byte b: raw) {
    //   System.out.print(String.format("%02x,", b));
    // }
    // System.out.print("}");

    // void printarray(byte[] arr) {
    //   for (byte b: arr) { System.out.print(String.format("%02x,", b)); }
    // }
    // rblRx.enableValueNotifications((v) -> {
    //   for (byte b: v) {
    //     System.out.print(String.format("%02x,", b));
    //   };
    //   System.out.println("");
    // });

    // writeV(rblRx2, "a569234269710072000100000000000000000000");
    // writeV(rblRx2, "a702000024000600000880000000040004000000");
    // writeV(rblRx2, "a700000000000000000000000000000000000000");
    // writeV(rblRx2, "a700000000000000000000000000000000000000");
    // writeV(rblRx2, "a7000000000000000000715b");
    writeV(rblRx2, "a50e234c0e0512770001041101002034");
    writeV(rblRx2, "a50e234c0e7100730001040f0200f49f");
    // 0000   a5 0b 23 43 0b 34 00 3b 00 12 13 00 65
    //              23,43,0b,00,00,00,00,26,13,13,4d

    sendSliced(sendReqAllLightOff(), rblRx2);

    Thread.sleep(1000*100);
    sensor.disconnect();
  }

  public static void writeV(BluetoothGattCharacteristic to, String payload) throws InterruptedException {
    System.out.println(payload);
    to.writeValue(hexStringToByteArray(payload));
    Thread.sleep(1000);
  }

  public static byte[] hexStringToByteArray(String s) {
      int len = s.length();
      byte[] data = new byte[len / 2];
      for (int i = 0; i < len; i += 2) {
          data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                               + Character.digit(s.charAt(i+1), 16));
      }
      return data;
  }

  static final byte[] sendReqAllLightOff() {
      System.out.println("UBLProtocol RBLPRotocol: sendReqAllLightOff");
      byte c = 0;
      byte d = 0;
      byte e = 0;
      byte f = 0;
      byte by = c;
      byte by2 = (byte)(by ^ 38);
      byte[] arrby = new byte[]{(byte)35, (byte)67, (byte)11, (byte)0x34, 0, (byte)0x3b, 0, (byte)0x12, (byte)0x13, 0, 0};
      arrby[10] = mkCheckSum(arrby, 10);
      return arrby;
  }

  static byte mkCheckSum(byte[] arrby, int n) {
      int n2 = 0;
      for (int i = 0; i < n + 0; ++i) {
          n2 = (byte)(n2 ^ arrby[i]);
      }
      return (byte)(n2 & 255);
  }

  static boolean sendSliced(byte[] arrby, BluetoothGattCharacteristic dst) {
      int n;
      if (arrby.length == 0) {
          return true;
      }
      int n2 = arrby.length <= 255 ? 1 : 2;
      byte[] arrby2 = new byte[arrby.length + n2];
      arrby2[0] = (byte)(arrby.length & 255);
      if (n2 == 2) {
          arrby2[1] = (byte)(255 & arrby.length >> 8);
      }
      for (n = 0; n < arrby.length; ++n) {
          arrby2[n + n2] = arrby[n];
      }
      if (dst != null) {
          int n3;
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
              for (n4 = 4; n4 >= 0 && !dst.writeValue(arrby); --n4) {
              }
              if (n4 >= 0) continue;
              return false;
          }
      }
      return true;
  }


}

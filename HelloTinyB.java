import tinyb.*;
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.TimeUnit;

public class HelloTinyB {
  static void printDevice(BluetoothDevice device) {
    System.out.print("Address = " + device.getAddress());
    System.out.print(" Name = " + device.getName());
    System.out.print(" Connected = " + device.getConnected());
    System.out.println();
  }

  static BluetoothDevice getDevice(String address) throws InterruptedException {
    BluetoothManager manager = BluetoothManager.getBluetoothManager();
    BluetoothDevice device = null;
    for (int i = 0; i < 15; ++i) {
      List<BluetoothDevice> list = manager.getDevices();
      if (list == null)
        return null;

      for (BluetoothDevice d: list) {
        printDevice(d);
        if (d.getAddress().equals(address))
          device = d;
      }

      if (device != null) {
        return device;
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
    } while (bluetoothServices.isEmpty());
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

  public static void main(String[] args) throws InterruptedException {
    String deviceAddress = "B4:10:7B:24:1B:4B";
    BluetoothManager manager = BluetoothManager.getBluetoothManager();
    boolean discoveryStarted = manager.startDiscovery();

    System.out.println("The discovery started: " + (discoveryStarted ? "true" : "false"));
    BluetoothDevice device = getDevice(deviceAddress);

    try {
      manager.stopDiscovery();
    } catch (BluetoothException e) {
      System.err.println("Discovery could not be stopped.");
    }

    if (device == null) {
      System.err.println("No device found with the provided address.");
      System.exit(-1);
    }

    System.out.print("Found device: ");
    printDevice(device);

    if (device.connect())
      System.out.println("Device with the provided address connected");
    else {
      System.out.println("Could not connect device.");
      System.exit(-1);
    }

    BluetoothGattService boardService = getService(device, "0000fff0-0000-1000-8000-00805f9b34fb");

    if (boardService == null) {
      System.err.println("This device does not have the service we are looking for.");
      device.disconnect();
      System.exit(-1);
    }
    System.out.println("Found service " + boardService.getUUID());

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
    System.out.println("Found the characteristics");

    byte[] raw = rblName.readValue();
    System.out.println("Name = " + new String(raw));

    rblRx2.writeValue(turnOffTheLight());

    for (int i = 0; i < 19; i++) {
      for (int j = 0; j < 19; j++) {
        rblRx2.writeValue(placeStone(i, j, 2));
      }
    }
    for (int i = 0; i < 19; i++) {
      for (int j = 0; j < 19; j++) {
        rblRx2.writeValue(placeStone(i, j, 0));
      }
    }
    for (int i = 0; i < 19; i++) {
      for (int j = 0; j < 19; j++) {
        rblRx2.writeValue(placeStone(i, j, 1));
      }
    }
  }

  static final byte[] placeStone(int x, int y, int c) {
    byte magic = (byte)(((byte)x ^ (byte)y) << (byte)1);
    byte[] arrby = new byte[] { (byte)35, (byte)0x4C, (byte)14, 0, 0, 0, 0, 1, (byte)x, (byte)y, (byte)c, 0, magic, 0 };
    arrby[13] = checksum(arrby);
    return prefixed(arrby);
  }

  static final byte[] turnOffTheLight() {
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
}

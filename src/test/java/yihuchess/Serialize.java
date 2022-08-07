package yihuchess;

import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Arrays;
import javax.imageio.ImageIO;
import tinyb.*;

class Serialize {
  static String serialize(Object obj) {
    if (obj instanceof Call)
      return (serializeCall((Call) obj));
    if (obj instanceof String)
      return String.format("\"%s\"", obj);
    if (obj instanceof Integer || obj instanceof Boolean)
      return obj.toString();
    if (obj instanceof BluetoothNotification)
      return "replay(BluetoothNotification.class)";
    if (obj instanceof BluetoothDevice)
      return "replay(BluetoothDevice.class)";
    if (obj instanceof BluetoothGattCharacteristic)
      return "replay(BluetoothGattCharacteristic.class)";
    if (obj instanceof BluetoothGattService)
      return "replay(BluetoothGattService.class)";
    if (obj instanceof BluetoothNotification)
      return "replay(BluetoothNotification.class)";
    if (obj instanceof byte[])
      return serializeByteArr((byte[]) obj);
    if (obj instanceof Object[])
      return serializeVarargs((Object[]) obj);
    if (obj instanceof Rectangle)
      return serializeRectangle((Rectangle) obj);
    if (obj instanceof BufferedImage)
      return serializeBufferedImage((BufferedImage) obj);
    if (obj instanceof Duration)
      return String.format("Duration.ofMillis(%s)", ((Duration) obj).toMillis());
    if (obj == null)
      return "null";
    throw new IllegalArgumentException(
      String.format("Missing serialization for %s (%s)", obj, obj.getClass()));
  }

  static String serializeCall(Call call) {
    var name = serialize(call.name);
    var result = serialize(call.result);
    var args = serialize(call.args);
    if (call.args.length == 0)
      return String.format("new Call(%s, %s)", name, result);
    else
      return String.format("new Call(%s, %s, %s)", name, result, args);
  }

  static String serializeByteArr(byte[] arr) {
    var sb = new StringBuilder();
    for (int i = 0; i < arr.length; ++i)
      sb.append(arr[i] + (i == arr.length-1 ? "" : ","));
    return String.format("new byte[] {%s}", sb);
  }

  static String serializeVarargs(Object[] arr) {
    var sb = new StringBuilder();
    for (int i = 0; i < arr.length; ++i)
      sb.append(serialize(arr[i]) + (i == arr.length-1 ? "" : ", "));
    return sb.toString();
  }

  static String serializeRectangle(Rectangle r) {
    return String.format("new Rectangle(%s,%s,%s,%s)", r.x, r.y, r.width, r.height);
  }

  static String serializeBufferedImage(BufferedImage i) {
    var rgbs = i.getRGB(0, 0, i.getWidth(), i.getHeight(), null, 0, i.getWidth());
    var hash = Math.abs(Long.valueOf(Arrays.hashCode(rgbs)));
    var name = String.format("src/test/resources/%s.png", hash);
    try {
      var file = new File(name);
      if (!file.exists())
        ImageIO.write(i, "png", file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return String.format("img(\"%s.png\")", hash);
  }

  static BufferedImage img(String name) {
    try {
      return ImageIO.read(new File("src/test/resources/" + name));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}


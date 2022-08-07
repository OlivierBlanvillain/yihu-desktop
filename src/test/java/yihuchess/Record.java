package yihuchess;

import com.ericsson.commonlibrary.proxy.Proxy;
import com.ericsson.commonlibrary.proxy.Invocation;
import java.awt.AWTException;
import java.awt.Robot;
import java.io.File;
import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.function.Consumer;
import tinyb.*;
import static yihuchess.Serialize.serialize;

class Record {
  static String CALLBACK_NAME = "BluetoothNotification.run";
  static Consumer<Call> store;

  public static void main(String[] args) throws AWTException, IOException {
    Config.MAIN_LOOP_PERIOD_MILLI = 1000;
    var file = new FileWriter("dump.log");
    init((call) -> {
      try {
        file.append(serialize(call) + ",\n");
        file.flush();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });
    var r = record(new Robot());
    var m = record(BluetoothManager.getBluetoothManager());
    Main.loop(new Screen(r), new Bluetooth(m));
  }

  static void init(Consumer<Call> cc) {
    Record.store = cc;
  }

  @SuppressWarnings("unchecked")
  static <T> T record(T object) {
    return Proxy
      .with(object)
      .interceptAll((i) -> {
        if (i.getMethodName().equals("finalize"))
          return null;
        if (i.getMethodName().equals("delay"))
          return i.invoke();
        if (i.getMethodName().equals("enableValueNotifications"))
          hijackValueNotifications(i);

        var result = i.invoke();
        store.accept(new Call(callName(i), result, i.getParameters()));
        if (result instanceof BluetoothObject)
          return record(result);
        else
          return result;
      })
      .get();
  }

  @SuppressWarnings("unchecked")
  static void hijackValueNotifications(Invocation i) {
    BluetoothNotification<byte[]> arg0 = (BluetoothNotification<byte[]>) i.getParameter0();
    BluetoothNotification<byte[]> new0 = (arr) -> {
      store.accept(new Call(CALLBACK_NAME, null, arr));
      arg0.run(arr);
    };
    i.getParameters()[0] = new0;
  }

  static String callName(Invocation i) {
    return i.getThis().getClass().getSuperclass().getSimpleName() + "." + i.getMethodName();
  }

  static class PendingResult {}
}

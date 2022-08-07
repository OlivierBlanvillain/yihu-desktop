package yihuchess;

import com.ericsson.commonlibrary.proxy.Proxy;
import java.awt.Robot;
import java.util.ArrayDeque;
import tinyb.BluetoothNotification;
import static yihuchess.Serialize.serialize;

class Replay {
  static BluetoothNotification<Object> callback;
  static ArrayDeque<Call> mutableQ;

  static void init(ArrayDeque<Call> q) {
    callback = null;
    mutableQ = q;
  }

  @SuppressWarnings("unchecked")
  static <T> T replay(Class<T> clazz) {
    return Proxy
      .with(clazz)
      .interceptAll((i) -> {
        if (i.getMethodName().equals("finalize"))
          return null;
        if (i.getMethodName().equals("delay"))
          return null;
        if (i.getMethodName().equals("enableValueNotifications"))
          callback = (BluetoothNotification<Object>) i.getParameter0();

        while (!mutableQ.isEmpty() && mutableQ.peekFirst().name.equals(Record.CALLBACK_NAME))
          callback.run(mutableQ.removeFirst().args[0]);
        if (mutableQ.isEmpty())
          throw new EndOfReplay();

        var expected = mutableQ.removeFirst();
        var actual = new Call(Record.callName(i), expected.result, i.getParameters());
        var a = serialize(actual);
        var e = serialize(expected);
        if (!a.equals(e))
          throw new AssertionError(String.format("\n  A: %s\n  E: %s\n  %s", a, e, expected.pos));
        else
          return expected.result;
      })
      .get();
  }

  static class EndOfReplay extends RuntimeException {
    static final long serialVersionUID = 0;
  }
}

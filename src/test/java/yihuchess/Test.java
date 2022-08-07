package yihuchess;

import java.awt.AWTException;
import java.awt.Robot;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayDeque;
import tinyb.*;
import static yihuchess.Replay.EndOfReplay;
import static yihuchess.Serialize.serialize;

class Test {
  static String ANSI_GREEN = "\u001B[32m";
  static String ANSI_RESET = "\u001B[0m";

  public static void main(String[] args) throws IOException, AWTException {
    Config.INIT_X = 228;
    Config.INIT_Y = 56;
    Config.INIT_W = 596;
    Config.INIT_H = 596;

    test(Tygem.calls());
    fixpoint(Tygem.calls());
  }

  static void test(ArrayDeque<Call> calls) throws IOException, AWTException {
    Replay.init(calls);
    try {
      var r = Replay.replay(Robot.class);
      var m = Replay.replay(BluetoothManager.class);
      Main.loop(new Screen(r), new Bluetooth(m));
    } catch (Replay.EndOfReplay e) {
      System.out.println(ANSI_GREEN + "\nTygem test passed!\n" + ANSI_RESET);
    }
  }

  static void fixpoint(ArrayDeque<Call> calls) throws IOException, AWTException {
    var recorded = new ArrayDeque<Call>();
    Replay.init(calls.clone());
    Record.init((c) -> { recorded.addLast(c); });

    try {
      var r = Record.record(Replay.replay(Robot.class));
      var m = Record.record(Replay.replay(BluetoothManager.class));
      Main.loop(new Screen(r), new Bluetooth(m));
    } catch (EndOfReplay x) {
      var actual = recorded.toArray();
      var expected = calls.toArray();
      for (var i = 0; i < Math.max(actual.length, expected.length); ++i) {
        var a = serialize(actual[i]);
        var e = serialize(expected[i]);
        if (!a.equals(e)) {
          System.out.println(String.format("\n  A: %s\n  E: %s", a, e));
          throw new AssertionError();
        }
      }
      System.out.println(ANSI_GREEN + "\nMeta test passed!\n" + ANSI_RESET);
    }
  }
}

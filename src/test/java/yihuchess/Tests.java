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

class Tests {

  public static void main(String[] args) throws IOException, AWTException {
    Config.INIT_X = 228;
    Config.INIT_Y = 56;
    Config.INIT_W = 596;
    Config.INIT_H = 596;

    integration(Tygem.middlegame(), "Middle game position");
    integration(Tygem.capture(), "Capture");
    integration(Tygem.joseki(), "Joseki");
    fixpoint(Tygem.middlegame());
  }

  static void celebrate(String testName) {
    var ANSI_GREEN = "\u001B[32m";
    var ANSI_RESET = "\u001B[0m";
    System.out.println(ANSI_GREEN + "\n" + testName + " test passed!\n" + ANSI_RESET);
  }

  static void integration(ArrayDeque<Call> calls, String testName) throws IOException, AWTException {
    Replay.init(calls);
    try {
      var r = Replay.replay(Robot.class);
      var m = Replay.replay(BluetoothManager.class);
      Main.loop(new Screen(r), new Bluetooth(m));
    } catch (EndOfReplay e) {
      celebrate(testName);
    }
  }

  static void fixpoint(ArrayDeque<Call> calls) throws IOException, AWTException {
    var recorded = new ArrayDeque<Call>();
    Replay.init(calls);
    Record.init((c) -> { recorded.addLast(c); });

    try {
      var r = Record.record(Replay.replay(Robot.class));
      var m = Record.record(Replay.replay(BluetoothManager.class));
      Main.loop(new Screen(r), new Bluetooth(m));
    } catch (EndOfReplay x) {
      var actual = recorded.toArray(new Call[recorded.size()]);
      var expected = calls.toArray(new Call[calls.size()]);
      for (var i = 0; i < Math.max(actual.length, expected.length); ++i) {
        var a = serialize(actual[i]);
        var e = serialize(expected[i]);
        if (!a.equals(e)) {
          System.out.println(String.format("\n  %s,\n  %s (%s)", a, e, expected[i].pos));
          throw new AssertionError();
        }
      }
      celebrate("Meta");
    }
  }
}

package yihuchess;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;

class Log {
  static FileWriter file;

  static void into(String name) {
    try {
      file = new FileWriter(name);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static void log() {
    var trace = Thread.currentThread().getStackTrace();
    log(trace[2], trace);
  }

  static void log(Object s) {
    var trace = Thread.currentThread().getStackTrace();
    log(trace[2], trace);
    log("> " + s, trace);
  }

  static void log(Object s, StackTraceElement[] trace) {
    if (file != null)
      try {
        file.append(" ".repeat(trace.length) + s + '\n');
        file.flush();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
  }
}

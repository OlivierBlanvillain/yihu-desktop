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

  static boolean isLogging() {
    return file != null;
  }

  static void unindented(Object s) {
    println(s);
    flush();
  }

  static void log() {
    var stack = Thread.currentThread().getStackTrace();
    print(" ".repeat(stack.length));
    println(stack[2]);
    flush();
  }

  static void log(Object s) {
    var stack = Thread.currentThread().getStackTrace();
    print(" ".repeat(stack.length));
    println(stack[2]);
    print(" ".repeat(stack.length));
    println("> " + s);
    flush();
  }

  static void println(Object obj) {
    print(obj.toString() + '\n');
  }

  static void print(Object obj) {
    if (file != null)
      try {
        file.append(obj.toString());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
  }

  static void flush() {
    if (file != null)
      try {
        file.flush();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
  }
}

package yihuchess;

class Config {
  static String GOBAN_MAC_ADDRESS = "B4:10:7B:24:1B:4B";
  static String SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
  static String INCOMING_UUID = "0000fff4-0000-1000-8000-00805f9b34fb";
  static String OUTGOING_UUID = "0000fff3-0000-1000-8000-00805f9b34fb";

  static boolean MANUAL_SCREEN_CAPTURE_SETUP = false;
  static int MAIN_LOOP_PERIOD_MILLI = 10;
  static int BLUETOOTH_TIMEOUT_MILLI = 2000;
  static int MAX_CLICK_ATTEMPS = 10;

  static int INIT_X = 421;
  static int INIT_Y = 70;
  static int INIT_W = 1246;
  static int INIT_H = 1246;

  static int GOBAN_COLOR = 0xFFFDE086;
  static int WHITE_COLOR = 0xFFFFFFFF;
  static int BLACK_COLOR = 0xFF898989;
}

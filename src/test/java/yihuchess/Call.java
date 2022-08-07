package yihuchess;

class Call {
  String name;
  Object result;
  Object[] args;
  String pos;

  Call(String name, Object result, Object... args) {
    this.name = name;
    this.result = result;
    this.args = args;
    var e = Thread.currentThread().getStackTrace()[2];
    this.pos = e.getFileName() + ":" + e.getLineNumber();
  }
}

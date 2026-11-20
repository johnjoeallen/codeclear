# Tests: common/leftover-debug-output

A `// FLAG` marker ends each line that must produce a finding.

## Case: System.out.println (Detect)

covers: example:flag,required:system-out

```java path=src/main/java/C.java
class C {
  void f(int x) {
    System.out.println("x=" + x);  // FLAG
  }
}
```

## Case: System.err and printf

covers: detect:system-err,required:each-call

```java path=src/main/java/C.java
class C {
  void f(int x) {
    System.err.print("a");  // FLAG
    System.out.printf("%d%n", x);  // FLAG
    System.err.println();  // FLAG
  }
}
```

## Case: printStackTrace (Detect)

covers: detect:print-stack-trace,required:each-call

```java path=src/main/java/C.java
class C {
  void f() {
    try { g(); } catch (Exception e) {
      e.printStackTrace();  // FLAG
    }
  }
}
```

## Case: a logger is the fix (Examples: OK)

covers: example:ok

```java path=src/main/java/C.java
class C {
  void f(int x, Exception e) {
    log.info("x={}", x);
    log.error("failed", e);
  }
}
```

## Case: other print-like calls are not console output

covers: detect:console-only

```java path=src/main/java/C.java
class C {
  void f(PrintWriter writer, Exception e) {
    writer.println("x");
    out.print("y");
    System.out.flush();
    e.printStackTrace(writer);
  }
}
```

## Case: a main method is a command-line tool (Do not flag: main)

covers: do-not-flag:cli-tool,required:main

```java path=src/main/java/C.java
class C {
  public static void main(String[] args) {
    System.out.println("usage: tool <file>");
    Runnable r = () -> System.err.println("failed");
  }
}
```

## Case: code in bin, scripts and cli folders (Do not flag: command-line tools)

covers: do-not-flag:cli-tool

```java path=tools/cli/Tool.java
class Tool {
  void run() {
    System.out.println("done");
  }
}
```

## Case: scripts folder

covers: do-not-flag:cli-tool

```java path=scripts/Run.java
class Run {
  void go() {
    System.out.println("go");
  }
}
```

## Case: bin folder

covers: do-not-flag:cli-tool

```java path=app/bin/Run.java
class Run {
  void go() {
    System.out.println("go");
  }
}
```

## Case: a folder merely named like one is not a command-line tool

covers: detect:cli-folder-exact

```java path=src/main/java/cliche/Run.java
class Run {
  void go() {
    System.out.println("go");  // FLAG
  }
}
```

## Case: test files (Do not flag: tests)

covers: do-not-flag:test-file,required:test-file

```java path=src/test/java/CTest.java
class CTest {
  void f() {
    System.out.println("debug");
    new Exception().printStackTrace();
  }
}
```

## Case: a helper method of a class that has a main method (Do not flag: command-line tool)

covers: do-not-flag:cli-tool

```java path=src/main/java/C.java
class Tool {
  public static void main(String[] args) {
    run();
  }
  static void run() {
    System.out.println("done");
  }
  static class Report {
    void show() {
      System.out.println("report");
    }
  }
}
```

## Case: a class with no main method, next to one that has it

covers: detect:main-class-only

```java path=src/main/java/C.java
class Tool {
  public static void main(String[] args) { }
}
class Service {
  void f() {
    System.out.println("debug");  // FLAG
  }
}
```

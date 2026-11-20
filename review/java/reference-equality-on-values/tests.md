# Tests: java/reference-equality-on-values

A `// FLAG` marker ends each line that must produce a finding.

## Case: String compared with a literal (Examples: Flag)

covers: example:flag,required:string-literal

```java path=src/main/java/C.java
class C {
  boolean f(String status) {
    return status == "ACTIVE";  // FLAG
  }
}
```

## Case: literal on the left

covers: detect:string-literal

```java path=src/main/java/C.java
class C {
  boolean f(String status) {
    return "ACTIVE" != status;  // FLAG
  }
}
```

## Case: two Integer variables (Examples: Flag)

covers: example:flag,required:boxed

```java path=src/main/java/C.java
class C {
  boolean f(Integer count1, Integer count2) {
    return count1 == count2;  // FLAG
  }
}
```

## Case: boxed Long, Boolean and Character

covers: detect:boxed-types

```java path=src/main/java/C.java
class C {
  void f(Long a, Long b, Boolean c, Boolean d, Character e, Character g) {
    boolean x = a == b;  // FLAG
    boolean y = c != d;  // FLAG
    boolean z = e == g;  // FLAG
  }
}
```

## Case: equals is the fix (Examples: OK)

covers: example:ok

```java path=src/main/java/C.java
class C {
  boolean f(String status, Integer a, Integer b) {
    return "ACTIVE".equals(status) && a.equals(b);
  }
}
```

## Case: Integer compared with an int (Do not flag: primitive)

covers: do-not-flag:primitive,required:primitive

```java path=src/main/java/C.java
class C {
  boolean f(Integer count, int other) {
    return count == other || count == 5;
  }
}
```

## Case: two primitives

covers: do-not-flag:primitive

```java path=src/main/java/C.java
class C {
  boolean f(int a, long b, char c) {
    return a == b || c == 'x';
  }
}
```

## Case: enum comparison (Do not flag: enums)

covers: do-not-flag:enum,required:enum

```java path=src/main/java/C.java
class C {
  enum Color { RED, GREEN }
  boolean f(Color c) {
    return c == Color.RED;
  }
}
```

## Case: comparison with null (Do not flag: null)

covers: do-not-flag:null,required:null

```java path=src/main/java/C.java
class C {
  boolean f(String s, Integer i) {
    return s == null || null != i;
  }
}
```

## Case: constant documented as an identity sentinel (Do not flag: sentinel)

covers: do-not-flag:sentinel

```java path=src/main/java/C.java
class C {
  /** Identity sentinel: compared by reference on purpose. */
  static final String UNSET = new String("unset");
  boolean f(String s) {
    return s == UNSET;
  }
}
```

## Case: an ordinary constant is not a sentinel

covers: detect:sentinel-needs-comment

```java path=src/main/java/C.java
class C {
  static final String ACTIVE = "ACTIVE";
  boolean f(String s) {
    return s == ACTIVE;  // FLAG
  }
}
```

## Case: an unresolved operand compared with a literal reports at LOW confidence

covers: detect:fallback-literal

```java path=src/main/java/C.java
class C {
  boolean f(Thing thing) {
    return thing.name() == "x";  // FLAG LOW
  }
}
```

## Case: both operands unresolved and no literal: no finding

covers: detect:fallback-literal

```java path=src/main/java/C.java
class C {
  boolean f(Thing a, Thing b) {
    return a.name() == b.name();
  }
}
```

## Case: a String compared with an Object is not both String

covers: detect:both-operands

```java path=src/main/java/C.java
class C {
  boolean f(String s, Object o) {
    return s == o;
  }
}
```

## Case: operators other than == and != are not reference comparisons

covers: detect:operators-only

```java path=src/main/java/C.java
class C {
  void f(String a, String b, Integer x, Integer y) {
    String s = a + b;
    Integer sum = x + y;
    boolean lt = x < y;
  }
}
```

## Case: a string literal compared with null

covers: do-not-flag:null

```java path=src/main/java/C.java
class C {
  boolean f() {
    return "x" == null;
  }
}
```

## Case: the identity sentinel on the left

covers: do-not-flag:sentinel

```java path=src/main/java/C.java
class C {
  /** Identity sentinel: compared by reference on purpose. */
  static final String UNSET = new String("unset");
  boolean f(String s) {
    return UNSET == s;
  }
}
```

## Case: an Object on the left and a String on the right

covers: detect:both-operands

```java path=src/main/java/C.java
class C {
  boolean f(Object o, String s) {
    return o == s;
  }
}
```

## Case: an Object compared with a string literal

covers: detect:both-operands

```java path=src/main/java/C.java
class C {
  boolean f(Object o) {
    return o == "x";
  }
}
```

## Case: an identity shortcut inside equals (Do not flag: equals)

covers: do-not-flag:equals-shortcut

```java path=src/main/java/C.java
class C {
  String area;
  @Override
  public boolean equals(Object other) {
    C rhs = (C) other;
    return (this.area == rhs.area) || (this.area != null && this.area.equals(rhs.area));
  }
}
```

## Case: the same comparison outside equals is still reported

covers: detect:equals-shortcut-only-in-equals

```java path=src/main/java/C.java
class C {
  String area;
  boolean same(C rhs) {
    return this.area == rhs.area;  // FLAG
  }
}
```

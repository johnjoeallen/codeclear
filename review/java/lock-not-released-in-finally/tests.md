# Tests: java/lock-not-released-in-finally

A `// FLAG` marker ends each line that must produce a finding.

## Prelude

```java
import java.util.concurrent.locks.*; import java.util.concurrent.Semaphore;
```

## Case: no try at all (Detect)

covers: example:flag,required:no-try

```java path=src/main/java/C.java
class C {
  private final ReentrantLock lock = new ReentrantLock();
  void f() {
    lock.lock();  // FLAG
    work();
    lock.unlock();
  }
}
```

## Case: unlock outside the finally (Detect)

covers: detect:unlock-outside-finally,required:outside-finally

```java path=src/main/java/C.java
class C {
  private final ReentrantLock lock = new ReentrantLock();
  void f() {
    lock.lock();  // FLAG
    try {
      work();
    } catch (RuntimeException e) {
      log.warn("x");
    }
    lock.unlock();
  }
}
```

## Case: the correct pattern (Examples: OK)

covers: example:ok,required:correct

```java path=src/main/java/C.java
class C {
  private final ReentrantLock lock = new ReentrantLock();
  void f() {
    lock.lock();
    try {
      work();
    } finally {
      lock.unlock();
    }
  }
}
```

## Case: a statement between the acquire and the try

covers: detect:try-immediately

```java path=src/main/java/C.java
class C {
  private final ReentrantLock lock = new ReentrantLock();
  void f() {
    lock.lock();  // FLAG
    prepare();
    try {
      work();
    } finally {
      lock.unlock();
    }
  }
}
```

## Case: the finally releases a different lock

covers: detect:same-lock

```java path=src/main/java/C.java
class C {
  private final ReentrantLock a = new ReentrantLock();
  private final ReentrantLock b = new ReentrantLock();
  void f() {
    a.lock();  // FLAG
    try {
      work();
    } finally {
      b.unlock();
    }
  }
}
```

## Case: lockInterruptibly, right and wrong

covers: detect:lock-interruptibly

```java path=src/main/java/C.java
class C {
  private final ReentrantLock lock = new ReentrantLock();
  void ok() throws InterruptedException {
    lock.lockInterruptibly();
    try { work(); } finally { lock.unlock(); }
  }
  void bad() throws InterruptedException {
    lock.lockInterruptibly();  // FLAG
    work();
    lock.unlock();
  }
}
```

## Case: read and write locks of a ReentrantReadWriteLock

covers: detect:read-write-lock

```java path=src/main/java/C.java
class C {
  private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
  void ok() {
    rw.readLock().lock();
    try { work(); } finally { rw.readLock().unlock(); }
  }
  void bad() {
    rw.writeLock().lock();  // FLAG
    work();
    rw.writeLock().unlock();
  }
}
```

## Case: a Semaphore

covers: detect:semaphore

```java path=src/main/java/C.java
class C {
  private final Semaphore permits = new Semaphore(3);
  void ok() throws InterruptedException {
    permits.acquire();
    try { work(); } finally { permits.release(); }
  }
  void bad() throws InterruptedException {
    permits.acquire();  // FLAG
    work();
    permits.release();
  }
}
```

## Case: tryLock guarded by if, with the unlock in a finally (Do not flag: tryLock)

covers: do-not-flag:trylock,required:trylock

```java path=src/main/java/C.java
class C {
  private final ReentrantLock lock = new ReentrantLock();
  void f() {
    if (lock.tryLock()) {
      try { work(); } finally { lock.unlock(); }
    }
  }
}
```

## Case: acquire and release in two methods of a wrapper (Do not flag: split across methods)

covers: do-not-flag:split-across-methods

```java path=src/main/java/C.java
class Guard {
  private final ReentrantLock lock = new ReentrantLock();
  void acquire() {
    lock.lock();  // FLAG LOW
  }
  void release() {
    lock.unlock();
  }
}
```

## Case: a receiver of unknown type whose name says it is a lock reports at LOW

covers: detect:name-heuristic

```java path=src/main/java/C.java
class C {
  void f(LockManager lockManager) {
    lockManager.lock();  // FLAG LOW
    work();
  }
}
```

## Case: lock() on something that is not a lock

covers: detect:lock-types-only

```java path=src/main/java/C.java
class C {
  void f(Account account) {
    account.lock();
    work();
  }
}
```

## Case: the acquire inside the try whose finally releases it (Do not flag: acquire inside try)

covers: do-not-flag:acquire-inside-try

```java path=src/main/java/C.java
class C {
  private final ReentrantLock lock = new ReentrantLock();
  void f() {
    try {
      lock.lock();
      work();
    } finally {
      lock.unlock();
    }
  }
}
```

## Case: the acquire inside a try, released by a finally on another lock

covers: detect:same-lock

```java path=src/main/java/C.java
class C {
  private final ReentrantLock a = new ReentrantLock();
  private final ReentrantLock b = new ReentrantLock();
  void f() {
    try {
      a.lock();  // FLAG
      work();
    } finally {
      b.unlock();
    }
  }
}
```

## Case: the acquire inside a try that has no finally

covers: detect:finally-needed

```java path=src/main/java/C.java
class C {
  private final ReentrantLock lock = new ReentrantLock();
  void f() {
    try {
      lock.lock();  // FLAG
      work();
    } catch (RuntimeException e) {
      log.warn("x");
    }
  }
}
```

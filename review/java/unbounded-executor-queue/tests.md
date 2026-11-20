# Tests: java/unbounded-executor-queue

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Case: newFixedThreadPool (Detect)

covers: example:flag,required:fixed-pool

```java path=src/main/java/C.java
class C {
  ExecutorService pool = Executors.newFixedThreadPool(8);  // CANDIDATE
}
```

## Case: single-thread and cached pools

covers: detect:executors,required:executors

```java path=src/main/java/C.java
class C {
  void f() {
    ExecutorService a = Executors.newSingleThreadExecutor();  // CANDIDATE
    ExecutorService b = Executors.newCachedThreadPool();  // CANDIDATE
  }
}
```

## Case: other factory methods are not unbounded in this sense

covers: detect:executors-list

```java path=src/main/java/C.java
class C {
  void f() {
    ScheduledExecutorService a = Executors.newScheduledThreadPool(2);
    ExecutorService b = Executors.newVirtualThreadPerTaskExecutor();
    Object c = Other.newFixedThreadPool(3);
  }
}
```

## Case: a ThreadPoolExecutor with an unbounded queue and no handler

covers: detect:unbounded-queue

```java path=src/main/java/C.java
class C {
  ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());  // CANDIDATE
}
```

## Case: a bounded queue with an explicit handler (Do not flag: bounded)

covers: do-not-flag:bounded-with-handler,required:bounded

```java path=src/main/java/C.java
class C {
  ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100),
      new ThreadPoolExecutor.CallerRunsPolicy());
}
```

## Case: a bounded queue without a handler

covers: detect:unbounded-queue

```java path=src/main/java/C.java
class C {
  ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
}
```

## Case: an unbounded queue with an explicit handler

covers: detect:handler

```java path=src/main/java/C.java
class C {
  ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
      new ThreadPoolExecutor.AbortPolicy());
}
```

## Case: a ThreadPoolTaskExecutor that never sets its queue capacity

covers: detect:task-executor

```java path=src/main/java/C.java
class C {
  Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();  // CANDIDATE
    executor.setCorePoolSize(4);
    executor.initialize();
    return executor;
  }
}
```

## Case: a ThreadPoolTaskExecutor with a queue capacity (Do not flag: bounded)

covers: do-not-flag:bounded-with-handler

```java path=src/main/java/C.java
class C {
  Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setQueueCapacity(100);
    return executor;
  }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  ExecutorService pool = Executors.newFixedThreadPool(2);
  ThreadPoolExecutor other = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
}
```

## Case: a class that only takes an unbounded queue is not a thread pool

covers: detect:thread-pool-only

```java path=src/main/java/C.java
class C {
  Worker worker = new Worker(new LinkedBlockingQueue<>());
}
```

## Case: a ThreadPoolTaskExecutor created outside any method

covers: detect:task-executor-initializer

```java path=src/main/java/C.java
class C {
  ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();  // CANDIDATE
}
```

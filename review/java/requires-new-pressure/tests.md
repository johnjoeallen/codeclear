# Tests: java/requires-new-pressure

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a REQUIRES_NEW method called in a loop (Examples: Flag)

covers: example:flag,required:in-loop

```java path=src/main/java/C.java
class C {
  @Autowired private Worker worker;
  @Transactional
  void run(List<Item> items) {
    for (Item i : items) {
      worker.process(i);  // CANDIDATE
    }
  }
}
class Worker {
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void process(Item i) { }
}
```

## Case: a single call outside a loop (Do not flag)

covers: do-not-flag:single-call,required:single

```java path=src/main/java/C.java
class C {
  @Autowired private Worker worker;
  void run(Item i) {
    worker.process(i);
  }
}
class Worker {
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void process(Item i) { }
}
```

## Case: a stream lambda and a listener method

covers: detect:lambda-and-listener

```java path=src/main/java/C.java
class C {
  @Autowired private Worker worker;
  void a(List<Item> items) {
    items.forEach(i -> worker.process(i));  // CANDIDATE
  }
  @KafkaListener(topics = "t")
  void onMessage(Item i) {
    worker.process(i);  // CANDIDATE
  }
}
class Worker {
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void process(Item i) { }
}
```

## Case: audit records that must survive the rollback (Do not flag)

covers: do-not-flag:audit

```java path=src/main/java/C.java
class C {
  @Autowired private Recorder recorder;
  void run(List<Item> items) {
    for (Item i : items) {
      recorder.auditFailure(i);
    }
  }
}
class Recorder {
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void auditFailure(Item i) { }
}
```

## Case: a callee with another propagation

covers: detect:propagation

```java path=src/main/java/C.java
class C {
  @Autowired private Worker worker;
  void run(List<Item> items) {
    for (Item i : items) {
      worker.process(i);
    }
  }
}
class Worker {
  @Transactional
  void process(Item i) { }
}
```

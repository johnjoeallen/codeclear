# Tests: java/kafka-producer-send-unobserved

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Case: a discarded send (Detect)

covers: example:flag,required:discarded

```java path=src/main/java/C.java
class C {
  void f(String msg) {
    kafkaTemplate.send("orders", msg);  // CANDIDATE
  }
}
```

## Case: sendDefault and a plain producer

covers: detect:send-forms

```java path=src/main/java/C.java
class C {
  void f(ProducerRecord<String, String> record, String msg) {
    producer.send(record);  // CANDIDATE
    kafkaTemplate.sendDefault(msg);  // CANDIDATE
    this.template.send("t", "k", msg);  // CANDIDATE
  }
}
```

## Case: get() waits for the result (Do not flag: observed)

covers: do-not-flag:result-observed,required:get

```java path=src/main/java/C.java
class C {
  void f(String msg) throws Exception {
    kafkaTemplate.send("orders", msg).get();
    producer.send(record).get(5, TimeUnit.SECONDS);
  }
}
```

## Case: whenComplete and addCallback chain a failure handler

covers: do-not-flag:result-observed,required:when-complete

```java path=src/main/java/C.java
class C {
  void f(String msg) {
    kafkaTemplate.send("orders", msg).whenComplete((r, ex) -> { if (ex != null) log.error("failed", ex); });
    kafkaTemplate.send("orders", msg).addCallback(ok -> { }, err -> log.error("failed", err));
  }
}
```

## Case: a callback overload (Do not flag: callback)

covers: do-not-flag:callback,required:callback

```java path=src/main/java/C.java
class C {
  void f(ProducerRecord<String, String> record) {
    producer.send(record, (metadata, error) -> { if (error != null) log.error("failed", error); });
    producer.send(record, this::onAck);
    producer.send(record, new Callback() {
      public void onCompletion(RecordMetadata m, Exception e) { }
    });
  }
}
```

## Case: the result is stored or returned (Do not flag: observed)

covers: do-not-flag:result-observed

```java path=src/main/java/C.java
class C {
  ListenableFuture<SendResult<String, String>> f(String msg) {
    ListenableFuture<SendResult<String, String>> pending = kafkaTemplate.send("a", msg);
    track(kafkaTemplate.send("b", msg));
    return kafkaTemplate.send("c", msg);
  }
}
```

## Case: send on something that is not a producer

covers: detect:producer-only

```java path=src/main/java/C.java
class C {
  void f(Message msg) {
    mailer.send(msg);
    emailSender.send(msg);
  }
}
```

## Case: a test source

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  void f() {
    kafkaTemplate.send("orders", "m");
  }
}
```

## Case: the value of an expression lambda is its result (Do not flag: observed)

covers: do-not-flag:result-observed

```java path=src/main/java/C.java
class C {
  void f(List<String> msgs) {
    Function<String, Object> g = m -> kafkaTemplate.send("t", m);
  }
}
```

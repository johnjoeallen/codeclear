# Tests: java/remote-call-in-transaction

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a payment gateway call after a save, in a transaction (Examples: Flag)

covers: example:flag,required:rest-template

```java path=src/main/java/C.java
class C {
  private final RestTemplate rest;
  private final OrderRepository repo;
  @Transactional
  void pay(Order o) {
    repo.save(o);
    rest.postForObject("http://gateway/charge", o, String.class);  // CANDIDATE
  }
}
```

## Case: the same call after the transaction (Examples: OK)

covers: example:ok,required:non-transactional

```java path=src/main/java/C.java
class C {
  private final RestTemplate rest;
  void notifyGateway(Order o) {
    rest.postForObject("http://gateway/charge", o, String.class);
  }
}
```

## Case: other remote clients and Thread.sleep

covers: detect:clients

```java path=src/main/java/C.java
class C {
  private final WebClient web;
  private final KafkaTemplate<String, String> kafka;
  private final RabbitTemplate rabbit;
  private final SqsClient sqs;
  @Transactional
  void f(Order o) throws Exception {
    web.get().retrieve();  // CANDIDATE
    kafka.send("t", "x");  // CANDIDATE
    rabbit.convertAndSend("q", o);  // CANDIDATE
    sqs.sendMessage(req);  // CANDIDATE
    Thread.sleep(500);  // CANDIDATE
  }
}
```

## Case: an after-commit listener (Do not flag: after commit)

covers: do-not-flag:after-commit

```java path=src/main/java/C.java
class C {
  private final RestTemplate rest;
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void onPaid(PaidEvent e) {
    rest.postForObject("http://gateway/notify", e, String.class);
  }
}
```

## Case: propagation that suspends or forbids the transaction (Do not flag)

covers: do-not-flag:no-transaction

```java path=src/main/java/C.java
class C {
  private final RestTemplate rest;
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void a(Order o) { rest.getForObject("http://x", String.class); }
  @Transactional(propagation = Propagation.NEVER)
  void b(Order o) { rest.getForObject("http://x", String.class); }
}
```

## Case: a call in a method that is not transactional (Do not flag)

covers: do-not-flag:other-method,required:non-transactional

```java path=src/main/java/C.java
class C {
  private final RestTemplate rest;
  private final OrderRepository repo;
  @Transactional
  void save(Order o) { repo.save(o); }
  void charge(Order o) { rest.postForObject("http://gateway/charge", o, String.class); }
}
```

## Case: a call on something that is not a remote client

covers: detect:receiver-type

```java path=src/main/java/C.java
class C {
  private final OrderRepository repo;
  private final Map<String, String> cache;
  @Transactional
  void f(Order o) {
    repo.save(o);
    cache.put("a", "b");
  }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  private final RestTemplate rest;
  @Transactional
  void f() { rest.getForObject("http://x", String.class); }
}
```

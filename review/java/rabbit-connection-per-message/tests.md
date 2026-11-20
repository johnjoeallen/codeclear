# Tests: java/rabbit-connection-per-message

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a connection and channel for one message (Examples: Flag)

covers: example:flag,required:per-message

```java path=src/main/java/C.java
class C {
  private final ConnectionFactory factory;
  void publish(byte[] body) throws Exception {
    Connection conn = factory.newConnection();  // CANDIDATE
    Channel ch = conn.createChannel();  // CANDIDATE
    ch.basicPublish("", "q", null, body);
  }
}
```

## Case: a Spring RabbitTemplate (Examples: OK)

covers: example:ok,do-not-flag:rabbit-template,required:template

```java path=src/main/java/C.java
class C {
  private final RabbitTemplate template;
  void publish(byte[] body) {
    template.convertAndSend("q", body);
  }
}
```

## Case: in a loop

covers: detect:loop

```java path=src/main/java/C.java
class C {
  private final ConnectionFactory factory;
  void publishAll(List<byte[]> bodies) throws Exception {
    for (byte[] b : bodies) {
      try (Connection c = factory.newConnection()) {  // CANDIDATE
        use(c);
      }
    }
  }
}
```

## Case: a channel made in execute(ChannelCallback), and in lifecycle code (Do not flag)

covers: do-not-flag:callback-or-lifecycle

```java path=src/main/java/C.java
class C {
  private final ConnectionFactory factory;
  private Connection conn;
  @PostConstruct
  void init() throws Exception {
    conn = factory.newConnection();
  }
  void publish(byte[] body) {
    template.execute(channel -> { channel.basicPublish("", "q", null, body); return null; });
  }
}
```

## Case: try-with-resources used once for a batch (Do not flag)

covers: do-not-flag:bounded-batch

```java path=src/main/java/C.java
class C {
  private final ConnectionFactory factory;
  void publishBatch(List<byte[]> bodies) throws Exception {
    try (Connection c = factory.newConnection(); Channel ch = c.createChannel()) {
      for (byte[] b : bodies) { ch.basicPublish("", "q", null, b); }
    }
  }
}
```

## Case: a method that neither publishes nor consumes

covers: detect:publishing-only

```java path=src/main/java/C.java
class C {
  private final ConnectionFactory factory;
  Connection open() throws Exception {
    return factory.newConnection();
  }
}
```

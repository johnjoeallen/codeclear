# Tests: java/aws-client-not-reused

A `// FLAG` marker ends each line that must produce a finding, and `// FLAG LOW` one with LOW confidence.

## Case: create() in a request method (Examples: Flag)

covers: example:flag,required:per-call-create

```java path=src/main/java/C.java
class C {
  void upload(byte[] b) {
    S3Client s3 = S3Client.create();  // FLAG
    s3.putObject(req, RequestBody.fromBytes(b));
  }
}
```

## Case: the builder form, SDK v2 and v1

covers: detect:builder

```java path=src/main/java/C.java
class C {
  void a() {
    DynamoDbClient d = DynamoDbClient.builder().region(Region.EU_WEST_1).build();  // FLAG
  }
  void b() {
    AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();  // FLAG
  }
  void c() {
    AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();  // FLAG
  }
}
```

## Case: a client injected and reused (Examples: OK)

covers: example:ok

```java path=src/main/java/C.java
class C {
  private final S3Client s3;
  C(S3Client s3) { this.s3 = s3; }
  void upload(byte[] b) {
    s3.putObject(req, RequestBody.fromBytes(b));
  }
}
```

## Case: @Bean and @PostConstruct methods (Do not flag)

covers: do-not-flag:bean,required:bean

```java path=src/main/java/C.java
@Configuration
class C {
  private S3Client held;
  @Bean
  S3Client s3() {
    S3Client c = S3Client.create();
    return c;
  }
  @PostConstruct
  void init() {
    SqsClient q = SqsClient.builder().build();
    this.held = S3Client.create();
  }
}
```

## Case: constructors and field or static initialisers (Do not flag)

covers: do-not-flag:initialisers,required:field-init

```java path=src/main/java/C.java
class C {
  private final S3Client s3 = S3Client.create();
  private static final SqsClient SQS;
  static {
    SQS = SqsClient.create();
  }
  private final DynamoDbClient dynamo;
  C() {
    DynamoDbClient d = DynamoDbClient.builder().build();
    this.dynamo = d;
  }
}
```

## Case: a client stored in a field, or returned, is not a per-call local

covers: detect:local

```java path=src/main/java/C.java
class C {
  private S3Client s3;
  void lazy() {
    this.s3 = S3Client.create();
  }
  S3Client make() {
    return S3Client.create();
  }
}
```

## Case: try-with-resources is not flagged outside a loop, and is LOW inside one

covers: do-not-flag:try-with-resources,required:try-with-resources

```java path=src/main/java/C.java
class C {
  void once() {
    try (S3Client s3 = S3Client.create()) {
      s3.listBuckets();
    }
  }
  void many(List<String> keys) {
    for (String k : keys) {
      try (S3Client s3 = S3Client.create()) {  // FLAG LOW
        s3.getObject(k);
      }
    }
  }
}
```

## Case: clients of other libraries

covers: detect:aws-only

```java path=src/main/java/C.java
class C {
  void f() {
    HttpClient h = HttpClient.create();
    WebClient w = WebClient.builder().baseUrl("x").build();
    RestClient r = RestClient.builder().build();
    OkHttpClient o = OkHttpClient.builder().build();
    Client c = Client.create();
  }
}
```

## Case: test sources (Do not flag)

covers: do-not-flag:tests

```java path=src/test/java/CTest.java
class CTest {
  void t() {
    S3Client s3 = S3Client.create();
  }
}
```

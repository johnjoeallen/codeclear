# Tests: java/aws-async-future-blocked

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: join right after the call (Examples: Flag)

covers: example:flag,required:join

```java path=src/main/java/C.java
class C {
  private final S3AsyncClient s3;
  GetObjectResponse f() {
    return s3.getObject(req, dest).join();  // CANDIDATE
  }
}
```

## Case: a future that is returned or composed (Examples: OK)

covers: example:ok,required:composed

```java path=src/main/java/C.java
class C {
  private final DynamoDbAsyncClient db;
  CompletableFuture<GetItemResponse> f() {
    return db.getItem(req);
  }
  CompletableFuture<String> g() {
    return db.getItem(req).thenApply(r -> r.toString());
  }
}
```

## Case: get() and other async clients

covers: detect:get

```java path=src/main/java/C.java
class C {
  private final SqsAsyncClient sqs;
  void f() throws Exception {
    sqs.sendMessage(m).get();  // CANDIDATE
  }
}
```

## Case: blocking at an application boundary (Do not flag)

covers: do-not-flag:boundary,required:main

```java path=src/main/java/C.java
class C {
  private final S3AsyncClient s3;
  public static void main(String[] args) {
    s3.listBuckets().join();
  }
  @PostConstruct
  void init() {
    s3.listBuckets().join();
  }
}
```

## Case: a blocking client and an unrelated join (Do not flag)

covers: do-not-flag:other-client

```java path=src/main/java/C.java
class C {
  private final S3Client s3;
  private final Executor pool;
  void f() {
    s3.listBuckets();
    CompletableFuture.runAsync(() -> {}, pool).join();
  }
}
```

## Case: a test source

covers: detect:test-source

```java path=src/test/java/CTest.java
class CTest {
  private final S3AsyncClient s3;
  void f() { s3.listBuckets().join(); }
}
```

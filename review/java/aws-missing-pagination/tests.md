# Tests: java/aws-missing-pagination

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a listObjectsV2 with no paging (Examples: Flag)

covers: example:flag,required:no-paging

```java path=src/main/java/C.java
class C {
  private final S3Client s3;
  List<S3Object> all(String b) {
    return s3.listObjectsV2(ListObjectsV2Request.builder().bucket(b).build()).contents();  // CANDIDATE
  }
}
```

## Case: a paginator (Examples: OK)

covers: example:ok,do-not-flag:paginator,required:paginator

```java path=src/main/java/C.java
class C {
  private final S3Client s3;
  void all(String b) {
    s3.listObjectsV2Paginator(r).contents().forEach(this::use);
  }
}
```

## Case: a loop on the continuation token

covers: required:token-loop

```java path=src/main/java/C.java
class C {
  private final S3Client s3;
  void all(String b) {
    String token = null;
    do {
      var r = s3.listObjectsV2(req(b, token));
      token = r.nextContinuationToken();
    } while (token != null);
  }
}
```

## Case: an explicit bound (Do not flag)

covers: do-not-flag:bounded

```java path=src/main/java/C.java
class C {
  private final DynamoDbClient db;
  void sample() {
    db.scan(ScanRequest.builder().tableName("t").limit(10).build());
  }
}
```

## Case: operations that are not paginated, and other receivers (Do not flag)

covers: do-not-flag:not-paginated

```java path=src/main/java/C.java
class C {
  private final S3Client s3;
  private final Map<String, String> map;
  void f() {
    s3.getObject(req);
    map.query("x");
  }
}
```

## Case: other paginated calls

covers: detect:operations

```java path=src/main/java/C.java
class C {
  private final DynamoDbClient db;
  private final SqsClient sqs;
  void f() {
    db.scan(r);  // CANDIDATE
    db.query(q);  // CANDIDATE
    sqs.listQueues();  // CANDIDATE
  }
}
```

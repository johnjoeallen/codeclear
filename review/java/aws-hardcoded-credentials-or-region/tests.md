# Tests: java/aws-hardcoded-credentials-or-region

A `// FLAG` marker ends each line that must produce a finding, and `// CANDIDATE` a line that must produce a candidate for the AI.

## Case: literal access keys (Examples: Flag)

covers: example:flag,required:literal-key

```java path=src/main/java/C.java
class C {
  S3Client build() {
    return S3Client.builder().credentialsProvider(StaticCredentialsProvider.create(
        AwsBasicCredentials.create("AKIAXXXX", "wJalXXXX"))).build();  // FLAG
  }
}
```

## Case: the default provider chain (Examples: OK)

covers: example:ok,do-not-flag:from-config,required:default-provider

```java path=src/main/java/C.java
class C {
  S3Client build(String key, String secret) {
    S3Client a = S3Client.create();
    S3Client b = S3Client.builder().credentialsProvider(DefaultCredentialsProvider.create()).build();
    S3Client c = S3Client.builder().credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(key, secret))).build();
    return a;
  }
}
```

## Case: the SDK v1 credential classes

covers: detect:v1-classes

```java path=src/main/java/C.java
class C {
  void f() {
    var a = new BasicAWSCredentials("AKIAXXXX", "secret");  // FLAG
    var b = new BasicSessionCredentials("AKIAXXXX", "secret", "token");  // FLAG
    var c = new BasicAWSCredentials(key, secret);
  }
}
```

## Case: a literal region and endpoint (candidate)

covers: required:literal-region

```java path=src/main/java/C.java
class C {
  void f() {
    S3Client a = S3Client.builder().region(Region.of("eu-west-1")).build();  // CANDIDATE
    var b = AmazonS3ClientBuilder.standard().withRegion("us-east-1");  // CANDIDATE
    S3Client c = S3Client.builder().endpointOverride(URI.create("https://s3.example.com")).build();  // CANDIDATE
  }
}
```

## Case: a region read from configuration (Do not flag: by design)

covers: do-not-flag:region-from-config

```java path=src/main/java/C.java
class C {
  void f(String region, URI endpoint) {
    S3Client a = S3Client.builder().region(Region.of(region)).build();
    var b = AmazonS3ClientBuilder.standard().withRegion(region);
    S3Client c = S3Client.builder().endpointOverride(endpoint).build();
  }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source,required:test-source

```java path=src/test/java/CTest.java
class CTest {
  void f() {
    var a = AwsBasicCredentials.create("test", "test");
    var b = Region.of("us-east-1");
  }
}
```

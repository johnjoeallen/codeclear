# Tests: java/aws-overbroad-iam-policy

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: every action (Examples: Flag)

covers: example:flag,required:star-action

```java path=src/main/java/C.java
class C {
  PolicyStatement p() {
    return PolicyStatement.Builder.create().actions(List.of("*")).resources(List.of(arn)).build();  // CANDIDATE
  }
}
```

## Case: named actions on a named resource (Examples: OK)

covers: example:ok,required:named

```java path=src/main/java/C.java
class C {
  PolicyStatement p() {
    return PolicyStatement.Builder.create().actions(List.of("s3:GetObject")).resources(List.of(arn)).build();
  }
}
```

## Case: a service wildcard and a wildcard resource

covers: detect:wildcards,required:service-wildcard

```java path=src/main/java/C.java
class C {
  void f(Role role) {
    role.addToPolicy(PolicyStatement.Builder.create().actions(List.of("s3:*")).resources(List.of(arn)).build());  // CANDIDATE
    role.addToPolicy(PolicyStatement.Builder.create().actions(List.of("s3:GetObject")).resources(List.of("*")).build());  // CANDIDATE
  }
}
```

## Case: a JSON policy string

covers: detect:json

```java path=src/main/java/C.java
class C {
  String a = "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"*\",\"Resource\":\"arn:aws:s3:::b\"}]}";  // CANDIDATE
  String b = "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"s3:GetObject\",\"Resource\":\"*\"}]}";  // CANDIDATE
  String c = "{\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"s3:GetObject\",\"Resource\":\"arn:aws:s3:::b\"}]}";
}
```

## Case: a wildcard resource for actions that need it (Do not flag)

covers: do-not-flag:needs-star

```java path=src/main/java/C.java
class C {
  void f(Role role) {
    role.addToPolicy(PolicyStatement.Builder.create().actions(List.of("sts:GetCallerIdentity")).resources(List.of("*")).build());
    role.addToPolicy(PolicyStatement.Builder.create().actions(List.of("logs:CreateLogGroup", "logs:PutLogEvents")).resources(List.of("*")).build());
  }
}
```

## Case: a policy that is not in the code (Do not flag)

covers: do-not-flag:not-in-source

```java path=src/main/java/C.java
class C {
  void f(String action) {
    role.addToPolicy(PolicyStatement.Builder.create().actions(List.of(action)).build());
    log.info("allow *");
  }
}
```

## Case: a test source (Do not flag: tests)

covers: do-not-flag:test-source

```java path=src/test/java/CTest.java
class CTest {
  PolicyStatement p() { return PolicyStatement.Builder.create().actions(List.of("*")).build(); }
}
```

# Tests: java/jpa-entity-equals-hashcode

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: @Data on an entity (Examples: Flag)

covers: example:flag,required:data

```java path=src/main/java/Item.java
@Entity
@Data  // CANDIDATE
class Item {
  @Id @GeneratedValue Long id;
  String name;
}
```

## Case: @EqualsAndHashCode on an entity

covers: detect:lombok

```java path=src/main/java/Item.java
@Entity
@EqualsAndHashCode  // CANDIDATE
class Item {
  @Id @GeneratedValue Long id;
}
```

## Case: @Data on a class that is not an entity

covers: detect:entity-only

```java path=src/main/java/Dto.java
@Data
class Dto {
  Long id;
}
```

## Case: an explicit business key (Do not flag)

covers: do-not-flag:business-key,required:business-key

```java path=src/main/java/Item.java
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
class Item {
  @Id @GeneratedValue Long id;
  @EqualsAndHashCode.Include String sku;
}
```

## Case: hashCode over the generated id

covers: detect:generated-id,required:id-hashcode

```java path=src/main/java/Item.java
@Entity
class Item {
  @Id @GeneratedValue Long id;
  String name;
  @Override
  public int hashCode() {  // CANDIDATE
    return Objects.hash(id);
  }
}
```

## Case: hashCode through the getter

covers: detect:generated-id

```java path=src/main/java/Item.java
@Entity
class Item {
  @Id @GeneratedValue private Long id;
  public Long getId() { return id; }
  @Override
  public int hashCode() {  // CANDIDATE
    return getId() == null ? 0 : getId().hashCode();
  }
}
```

## Case: a constant hashCode with equals on the id (Examples: OK)

covers: example:ok,do-not-flag:constant-hashcode,required:constant

```java path=src/main/java/Item.java
@Entity
class Item {
  @Id @GeneratedValue Long id;
  @Override
  public boolean equals(Object o) {
    return o instanceof Item i && id != null && id.equals(i.id);
  }
  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
```

## Case: hashCode over an assigned business key, and over a class that is not an entity

covers: do-not-flag:business-key

```java path=src/main/java/Item.java
@Entity
class Item {
  @Id Long id;
  String sku;
  @Override
  public int hashCode() {
    return Objects.hash(sku);
  }
}
class Plain {
  @GeneratedValue Long id;
  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
```

## Case: test sources (Do not flag)

covers: do-not-flag:tests

```java path=src/test/java/ItemTest.java
@Entity
@Data
class ItemTest {
  @Id @GeneratedValue Long id;
}
```

# Tests: java/bidirectional-relationship-inconsistent

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a one-sided add (Examples: Flag)

covers: example:flag,required:one-sided

```java path=src/main/java/Order.java
@Entity
class Order {
  @OneToMany(mappedBy = "order")
  private List<Item> items = new ArrayList<>();
  void addItem(Item i) { items.add(i); }  // CANDIDATE
}
```

## Case: a helper that sets both sides (Examples: OK)

covers: example:ok,do-not-flag:both-sides,required:both-sides

```java path=src/main/java/Order.java
@Entity
class Order {
  @OneToMany(mappedBy = "order")
  private List<Item> items = new ArrayList<>();
  void addItem(Item i) { items.add(i); i.setOrder(this); }
  void removeItem(Item i) { items.remove(i); i.setOrder(null); }
}
```

## Case: remove, addAll and clear

covers: detect:mutations

```java path=src/main/java/Order.java
@Entity
class Order {
  @OneToMany(mappedBy = "order")
  private List<Item> items = new ArrayList<>();
  void a(Item i) { items.remove(i); }  // CANDIDATE
  void b(List<Item> all) { items.addAll(all); }  // CANDIDATE
  void c() { items.clear(); }  // CANDIDATE
}
```

## Case: a unidirectional association (Do not flag)

covers: do-not-flag:unidirectional,required:unidirectional

```java path=src/main/java/Order.java
@Entity
class Order {
  @OneToMany
  @JoinColumn(name = "order_id")
  private List<Item> items = new ArrayList<>();
  void addItem(Item i) { items.add(i); }
}
```

## Case: a collection that is not a relationship, and a class that is not an entity

covers: detect:entity-only

```java path=src/main/java/Order.java
@Entity
class Order {
  @Transient
  private List<String> notes = new ArrayList<>();
  void note(String n) { notes.add(n); }
}
class Plain {
  @OneToMany(mappedBy = "order")
  private List<Item> items = new ArrayList<>();
  void addItem(Item i) { items.add(i); }
}
```

## Case: an immediate reload of the graph is left to the AI

covers: do-not-flag:reload

```java path=src/main/java/Order.java
@Entity
class Order {
  @ManyToMany(mappedBy = "orders")
  private Set<Tag> tags = new HashSet<>();
  void tag(Tag t) { tags.add(t); }  // CANDIDATE
}
```

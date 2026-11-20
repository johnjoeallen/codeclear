# Tests: java/lazy-association-outside-session

These cases run both restricted-language rules (`lazy-association-outside-session` and `lazy-entity-serialized`) over a small project. A `// FLAG` marker ends each line that must produce a finding.

## Case: eval 1: entity with LAZY collection returned to Jackson, OSIV disabled

covers: eval:1
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  @GetMapping("/o") Order get(long id) { return svc.find(id); }  // FLAG
}
```

## Case: eval 2: same, OSIV enabled explicitly

covers: eval:2
config: spring.jpa.open-in-view=true

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  @GetMapping("/o") Order get(long id) { return svc.find(id); }
}
```

## Case: eval 2b: same, OSIV property absent (default enabled)

covers: eval:2

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  @GetMapping("/o") Order get(long id) { return svc.find(id); }
}
```

## Case: eval 3: join fetch in the query

covers: eval:3, do-not-flag:fetch-join
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.repository.Query;
@Service class OrderService {
  @Query("select o from Order o join fetch o.items where o.id = :id")
  Order find(long id) { return null; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  @GetMapping("/o") Order get(long id) { return svc.find(id); }
}
```

## Case: EntityGraph on the loading method

covers: do-not-flag:fetch-join
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  @EntityGraph(attributePaths = "items")
  Order find(long id) { return null; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  @GetMapping("/o") Order get(long id) { return svc.find(id); }
}
```

## Case: DTO projection returned

covers: do-not-flag:dto
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  @GetMapping("/o") OrderDto get(long id) { return null; }
}
class OrderDto { }
```

## Case: entity with only EAGER associations returned

covers: do-not-flag:eager
config: spring.jpa.open-in-view=false

```java path=Flat.java
import jakarta.persistence.*;
@Entity class Flat { String name; @ManyToOne Tag tag; }
@Entity class Tag { }
```
```java path=FlatController.java
import org.springframework.web.bind.annotation.*;
@RestController class FlatController {
  @GetMapping("/f") Flat get(long id) { return null; }
}
```

## Case: only a @JsonIgnore lazy association

covers: do-not-flag:json-ignore
config: spring.jpa.open-in-view=false

```java path=Hidden.java
import jakarta.persistence.*;
@Entity class Hidden { @JsonIgnore @OneToMany java.util.List<Tag> tags; }
@Entity class Tag { }
```
```java path=HiddenController.java
import org.springframework.web.bind.annotation.*;
@RestController class HiddenController {
  @GetMapping("/h") Hidden get(long id) { return null; }
}
```

## Case: entity wrapped in ResponseEntity<List<..>>

covers: detect:wrappers
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.ResponseEntity;
@RestController class OrderController {
  OrderService svc;
  @GetMapping("/o") ResponseEntity<List<Order>> all() { return null; }  // FLAG
}
```

## Case: non-controller returning an entity

covers: detect:controller-only
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```

## Case: getter dereferenced outside a transaction, OSIV disabled

covers: detect:lazy-read
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  int n(long id) { return svc.find(id).getItems().size(); }  // FLAG
}
```

## Case: same read inside @Transactional

covers: do-not-flag:transactional
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  @org.springframework.transaction.annotation.Transactional
  int n(long id) { return svc.find(id).getItems().size(); }
}
```

## Case: same read in a @Transactional class

covers: do-not-flag:transactional
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@org.springframework.transaction.annotation.Transactional @RestController class OrderController {
  OrderService svc;
  int n(long id) { return svc.find(id).getItems().size(); }
}
```

## Case: same read with OSIV enabled

covers: do-not-flag:osiv
config: spring.jpa.open-in-view=true

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  int n(long id) { return svc.find(id).getItems().size(); }
}
```

## Case: read via a service method that join-fetches

covers: do-not-flag:fetch-join
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.repository.Query;
@Service class OrderService {
  @Query("select o from Order o join fetch o.items where o.id = :id")
  Order find(long id) { return null; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  int n(long id) { return svc.find(id).getItems().size(); }
}
```

## Case: EAGER-by-default association read outside a transaction

covers: do-not-flag:eager
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  String b(long id) { return svc.find(id).getBilling().getName(); }
}
```

## Case: explicit EAGER collection read outside a transaction

covers: do-not-flag:eager
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  int t(long id) { return svc.find(id).getTags().size(); }
}
```

## Case: getter result only assigned, never dereferenced

covers: do-not-flag:not-touched
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  void keep(long id) { Customer c = svc.find(id).getCustomer(); }
}
```

## Case: eval 4: o.getCustomer().getName() in a loop inside a transaction (N+1)

covers: eval:4, detect:n-plus-one
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=Report.java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service class Report {
  @Transactional void f(List<Order> orders) {
    for (Order o : orders) {
      String n = o.getCustomer().getName();  // FLAG
    }
  }
}
```

## Case: N+1 read inside a transaction is reported even with OSIV enabled

covers: detect:n-plus-one
config: spring.jpa.open-in-view=true

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=Report.java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service class Report {
  @Transactional void f(List<Order> orders) {
    for (Order o : orders) {
      String n = o.getCustomer().getName();  // FLAG
    }
  }
}
```

## Case: same read inside a transaction but not in a loop

covers: do-not-flag:single-read
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=Report.java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service class Report {
  @Transactional void f(List<Order> orders) {
    Order o = orders.get(0);
    String n = o.getCustomer().getName();
  }
}
```

## Case: test source

covers: do-not-flag:test-source
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=src/test/java/OrderControllerTest.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  int n(long id) { return svc.find(id).getItems().size(); }
}
```

## Case: N+1 read in a loop outside any transaction, OSIV enabled (still one query per iteration)

covers: detect:n-plus-one
config: spring.jpa.open-in-view=true

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=Report.java
import org.springframework.stereotype.Service;
import java.util.List;
@Service class Report {
  void f(List<Order> orders) {
    for (Order o : orders) {
      String n = o.getCustomer().getName();  // FLAG
    }
  }
}
```

## Case: test controller returning an entity

covers: do-not-flag:test-source
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=src/test/java/OrderControllerTest.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  @GetMapping("/o") Order get(long id) { return svc.find(id); }
}
```

## Case: deep provenance: controller to service to repository join-fetch query (orElseThrow)

covers: do-not-flag:fetch-join, detect:interprocedural
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderRepo.java
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
interface OrderRepo {
  @Query("select o from Order o join fetch o.items where o.id = :id")
  Optional<Order> findWithItems(long id);
  Optional<Order> findById(long id);
}
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  OrderRepo repo;
  Order find(long id) { return repo.findWithItems(id).orElseThrow(); }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  @GetMapping("/o") Order get(long id) { return svc.find(id); }
}
```

## Case: deep provenance through a local variable

covers: do-not-flag:fetch-join, detect:interprocedural
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderRepo.java
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
interface OrderRepo {
  @Query("select o from Order o join fetch o.items where o.id = :id")
  Optional<Order> findWithItems(long id);
  Optional<Order> findById(long id);
}
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  OrderRepo repo;
  Order find(long id) { Order o = repo.findWithItems(id).orElseThrow(); return o; }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  @GetMapping("/o") Order get(long id) { return svc.find(id); }
}
```

## Case: same chain but the repository method is a plain lookup (not fetched)

covers: detect:interprocedural
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderRepo.java
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
interface OrderRepo {
  @Query("select o from Order o join fetch o.items where o.id = :id")
  Optional<Order> findWithItems(long id);
  Optional<Order> findById(long id);
}
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  OrderRepo repo;
  Order find(long id) { return repo.findById(id).orElseThrow(); }
}
```
```java path=OrderController.java
import org.springframework.web.bind.annotation.*;
@RestController class OrderController {
  OrderService svc;
  @GetMapping("/o") Order get(long id) { return svc.find(id); }  // FLAG
}
```

## Case: parameter provenance: every caller passes a fetched entity

covers: do-not-flag:fetch-join, detect:interprocedural
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderRepo.java
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
interface OrderRepo {
  @Query("select o from Order o join fetch o.items where o.id = :id")
  Optional<Order> findWithItems(long id);
  Optional<Order> findById(long id);
}
```
```java path=Counter.java
import org.springframework.stereotype.Service;
@Service class Counter {
  OrderRepo repo;
  int count(Order o) { return o.getItems().size(); }
  int run(long id) { return count(repo.findWithItems(id).orElseThrow()); }
}
```

## Case: parameter provenance: the caller passes an unfetched entity

covers: detect:interprocedural
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderRepo.java
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
interface OrderRepo {
  @Query("select o from Order o join fetch o.items where o.id = :id")
  Optional<Order> findWithItems(long id);
  Optional<Order> findById(long id);
}
```
```java path=Counter.java
import org.springframework.stereotype.Service;
@Service class Counter {
  OrderRepo repo;
  int count(Order o) { return o.getItems().size(); }  // FLAG
  int run(long id) { return count(repo.findById(id).orElseThrow()); }
}
```

## Case: parameter provenance: one fetched caller and one unfetched caller

covers: detect:interprocedural
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderRepo.java
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
interface OrderRepo {
  @Query("select o from Order o join fetch o.items where o.id = :id")
  Optional<Order> findWithItems(long id);
  Optional<Order> findById(long id);
}
```
```java path=Counter.java
import org.springframework.stereotype.Service;
@Service class Counter {
  OrderRepo repo;
  int count(Order o) { return o.getItems().size(); }  // FLAG
  int a(long id) { return count(repo.findWithItems(id).orElseThrow()); }
  int b(long id) { return count(repo.findById(id).orElseThrow()); }
}
```

## Case: transaction through the call graph: the only caller is @Transactional

covers: do-not-flag:transactional, detect:interprocedural
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=A.java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service class A {
  B b;
  @Transactional void top(long id) { b.helper(id); }
}
```
```java path=B.java
import org.springframework.stereotype.Service;
@Service class B {
  OrderService svc;
  int helper(long id) { return svc.find(id).getItems().size(); }
}
```

## Case: transaction through the call graph: a second caller is not transactional

covers: detect:interprocedural
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=A.java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service class A {
  B b;
  @Transactional void top(long id) { b.helper(id); }
  void other(long id) { b.helper(id); }
}
```
```java path=B.java
import org.springframework.stereotype.Service;
@Service class B {
  OrderService svc;
  int helper(long id) { return svc.find(id).getItems().size(); }  // FLAG
}
```

## Case: transaction through two levels of callers

covers: do-not-flag:transactional, detect:interprocedural
config: spring.jpa.open-in-view=false

```java path=Order.java
import jakarta.persistence.*;
import java.util.*;
@Entity class Order {
  @OneToMany(mappedBy = "order") List<Item> items;
  @ManyToOne(fetch = FetchType.LAZY) Customer customer;
  @ManyToOne Customer billing;
  @OneToMany(fetch = FetchType.EAGER) List<Tag> tags;
  public List<Item> getItems() { return items; }
  public Customer getCustomer() { return customer; }
  public Customer getBilling() { return billing; }
  public List<Tag> getTags() { return tags; }
}
@Entity class Item { }
@Entity class Tag { }
@Entity class Customer { String name; public String getName() { return name; } }
```
```java path=OrderService.java
import org.springframework.stereotype.Service;
@Service class OrderService {
  Order find(long id) { return null; }
}
```
```java path=A.java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service class A {
  B b;
  @Transactional void top(long id) { mid(id); }
  void mid(long id) { b.helper(id); }
}
```
```java path=B.java
import org.springframework.stereotype.Service;
@Service class B {
  OrderService svc;
  int helper(long id) { return svc.find(id).getItems().size(); }
}
```

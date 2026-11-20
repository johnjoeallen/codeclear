# Tests: sql/update-or-delete-without-where

A `-- FLAG` marker ends each line that must produce a finding.

## Case: an UPDATE with no WHERE (Examples: Flag)

covers: example:flag,required:no-where

```sql path=db/migration/V1__close.sql
UPDATE accounts SET status = 'CLOSED';  -- FLAG
```

## Case: with a WHERE (Examples: OK)

covers: example:ok,required:top-level-where

```sql path=db/migration/V1__close.sql
UPDATE accounts SET status = 'CLOSED' WHERE closed_at IS NOT NULL;
DELETE FROM sessions WHERE expires_at < now();
```

## Case: a WHERE only in a subquery

covers: required:subquery-only

```sql path=db/migration/V2__sub.sql
UPDATE accounts SET balance = (SELECT sum(amount) FROM tx WHERE tx.account_id = accounts.id);  -- FLAG
DELETE FROM sessions WHERE id IN (SELECT id FROM expired);
```

## Case: DELETE with no WHERE, spread over lines

covers: detect:delete

```sql path=db/migration/V3__clear.sql
DELETE FROM audit_log;  -- FLAG
UPDATE orders  -- FLAG
   SET region = 'EU';
```

## Case: the opt-out comment (Do not flag)

covers: do-not-flag:marker,required:marker

```sql path=db/migration/V4__reset.sql
-- full-table: intentional
UPDATE accounts SET status = 'CLOSED';
```

## Case: a table created in the same file (Do not flag)

covers: do-not-flag:new-table

```sql path=db/migration/V5__seed.sql
CREATE TABLE settings (k VARCHAR(20), v VARCHAR(20));
INSERT INTO settings VALUES ('a', 'b');
UPDATE settings SET v = 'c';
```

## Case: a WHERE in a string or a comment is not a WHERE

covers: detect:string-where

```sql path=db/migration/V6__text.sql
UPDATE notes SET body = 'where to go';  -- FLAG
-- WHERE id = 1
DELETE FROM notes /* WHERE id = 1 */;  -- FLAG
```

## Case: an UPDATE that joins on a condition

covers: detect:join

```sql path=db/migration/V7__join.sql
UPDATE orders SET total = t.sum FROM totals t JOIN lines l ON l.order_id = t.order_id;
```

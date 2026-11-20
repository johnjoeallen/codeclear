# Tests: sql/blocking-index-creation

A `-- CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: CREATE INDEX with no CONCURRENTLY (Examples: Flag)

covers: example:flag,required:plain-index

```sql path=db/migration/V3__idx.sql
CREATE INDEX idx_orders_region ON orders (region);  -- CANDIDATE
CREATE UNIQUE INDEX idx_orders_ref ON orders (ref);  -- CANDIDATE
```

## Case: CONCURRENTLY, and an online build (Examples: OK)

covers: example:ok,required:concurrently

```sql path=db/migration/V3__idx.sql
CREATE INDEX CONCURRENTLY idx_orders_region ON orders (region);
CREATE INDEX idx_orders_ref ON orders (ref) WITH (ONLINE = ON);
```

## Case: MySQL ADD INDEX

covers: detect:mysql

```sql path=db/migration/V4__idx.sql
ALTER TABLE orders ADD INDEX idx_region (region);  -- CANDIDATE
ALTER TABLE orders ADD INDEX idx_ref (ref), ALGORITHM=INPLACE, LOCK=NONE;
```

## Case: an index on a table created in the same file (Do not flag)

covers: do-not-flag:new-table

```sql path=db/migration/V5__new.sql
CREATE TABLE audit (id BIGINT, actor VARCHAR(20));
CREATE INDEX idx_audit_actor ON audit (actor);
```

## Case: a size hint (Do not flag)

covers: do-not-flag:size-hint

```sql path=db/migration/V6__small.sql
-- table-size: small
CREATE INDEX idx_codes_name ON codes (name);
```

# Tests: sql/destructive-schema-change

A `-- CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: DROP TABLE and DROP COLUMN (Examples: Flag)

covers: example:flag,required:drop

```sql path=db/migration/V5__cleanup.sql
DROP TABLE legacy_orders;  -- CANDIDATE
ALTER TABLE orders DROP COLUMN region;  -- CANDIDATE
```

## Case: additive changes (Examples: OK)

covers: example:ok,required:additive

```sql path=db/migration/V5__add.sql
ALTER TABLE orders ADD COLUMN nickname VARCHAR(20);
CREATE TABLE audit (id BIGINT);
```

## Case: truncate, rename, type change and dropped constraint

covers: detect:statements

```sql path=db/migration/V6__rework.sql
TRUNCATE TABLE sessions;  -- CANDIDATE
ALTER TABLE orders RENAME COLUMN region TO area;  -- CANDIDATE
ALTER TABLE orders RENAME TO purchases;  -- CANDIDATE
ALTER TABLE orders ALTER COLUMN note TYPE VARCHAR(10);  -- CANDIDATE
ALTER TABLE orders DROP CONSTRAINT fk_customer;  -- CANDIDATE
```

## Case: a table created in the same file, and temporary tables (Do not flag)

covers: do-not-flag:staging,do-not-flag:same-file-table,required:same-file

```sql path=db/migration/V7__staging.sql
CREATE TABLE staging_import (id BIGINT);
DROP TABLE IF EXISTS staging_import;
DROP TABLE tmp_report;
DROP TABLE temp_copy;
```

## Case: a rollback migration (Do not flag)

covers: do-not-flag:rollback

```sql path=db/migration/down/V5__cleanup.sql
DROP TABLE legacy_orders;
ALTER TABLE orders DROP COLUMN region;
```

## Case: DROP INDEX and DROP DEFAULT are not column drops

covers: detect:drop-forms

```sql path=db/migration/V8__idx.sql
DROP INDEX idx_orders_region;
ALTER TABLE orders ALTER COLUMN note DROP DEFAULT;
```

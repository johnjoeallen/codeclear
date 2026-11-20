# Tests: sql/blocking-backfill-or-constraint

A `-- CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: one UPDATE over the table (Examples: Flag)

covers: example:flag,required:update

```sql path=db/migration/V9__backfill.sql
UPDATE orders SET region = 'EU' WHERE region IS NULL;  -- CANDIDATE
```

## Case: a batched UPDATE (Examples: OK)

covers: example:ok,do-not-flag:batched,required:batched

```sql path=db/migration/V9__backfill.sql
UPDATE orders SET region = 'EU' WHERE id BETWEEN 1 AND 10000;
UPDATE orders SET region = 'EU' WHERE id > 10000 AND id <= 20000;
UPDATE TOP (1000) orders SET region = 'EU' WHERE region IS NULL;
```

## Case: constraints and type changes

covers: detect:constraints

```sql path=db/migration/V10__constraints.sql
ALTER TABLE orders ADD CONSTRAINT fk_c FOREIGN KEY (customer_id) REFERENCES customers (id);  -- CANDIDATE
ALTER TABLE orders ADD CONSTRAINT ck CHECK (total >= 0);  -- CANDIDATE
ALTER TABLE orders ALTER COLUMN region SET NOT NULL;  -- CANDIDATE
ALTER TABLE orders ALTER COLUMN total TYPE NUMERIC(12, 2);  -- CANDIDATE
```

## Case: NOT VALID constraints (Do not flag)

covers: do-not-flag:not-valid,required:not-valid

```sql path=db/migration/V11__valid.sql
ALTER TABLE orders ADD CONSTRAINT fk_c FOREIGN KEY (customer_id) REFERENCES customers (id) NOT VALID;
ALTER TABLE orders VALIDATE CONSTRAINT fk_c;
```

## Case: a table created in the same file (Do not flag)

covers: do-not-flag:new-table

```sql path=db/migration/V12__new.sql
CREATE TABLE audit (id BIGINT, actor VARCHAR(20));
UPDATE audit SET actor = 'system';
ALTER TABLE audit ALTER COLUMN actor SET NOT NULL;
```

## Case: a size hint (Do not flag)

covers: do-not-flag:size-hint

```sql path=db/migration/V13__small.sql
-- table-size: small
UPDATE codes SET active = TRUE;
```

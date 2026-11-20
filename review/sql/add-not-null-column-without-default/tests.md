# Tests: sql/add-not-null-column-without-default

A `-- FLAG` marker ends each line that must produce a finding.

## Case: a NOT NULL column with no default (Examples: Flag)

covers: example:flag,required:missing-default

```sql path=db/migration/V2__region.sql
ALTER TABLE orders ADD COLUMN region VARCHAR(10) NOT NULL;  -- FLAG
```

## Case: with a default (Examples: OK)

covers: example:ok,do-not-flag:default,required:with-default

```sql path=db/migration/V2__region.sql
ALTER TABLE orders ADD COLUMN region VARCHAR(10) NOT NULL DEFAULT 'EU';
```

## Case: identity and generated columns

covers: required:identity

```sql path=db/migration/V3__ids.sql
ALTER TABLE a ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL;
ALTER TABLE b ADD id SERIAL NOT NULL;
ALTER TABLE c ADD COLUMN n INT NOT NULL AUTO_INCREMENT;
ALTER TABLE d ADD total INT GENERATED ALWAYS AS (a + b) STORED NOT NULL;
```

## Case: several columns in one ADD, one of them without a default

covers: required:multi-column

```sql path=db/migration/V4__many.sql
ALTER TABLE orders
  ADD COLUMN a INT NOT NULL DEFAULT 0,
  ADD COLUMN b INT NOT NULL,  -- FLAG
  ADD COLUMN c INT NULL,
  ADD CONSTRAINT fk FOREIGN KEY (c) REFERENCES other (id);
```

## Case: a commented-out statement

covers: required:commented-out

```sql path=db/migration/V5__old.sql
-- ALTER TABLE orders ADD COLUMN region VARCHAR(10) NOT NULL;
/* ALTER TABLE orders ADD COLUMN zone VARCHAR(10) NOT NULL; */
SELECT 1;
```

## Case: a table created in the same file (Do not flag: new empty table)

covers: do-not-flag:new-table

```sql path=db/migration/V6__new.sql
CREATE TABLE audit (id BIGINT PRIMARY KEY);
ALTER TABLE audit ADD COLUMN actor VARCHAR(50) NOT NULL;
```

## Case: the column is added as nullable and made NOT NULL later (Do not flag)

covers: do-not-flag:nullable-then-backfill

```sql path=db/migration/V7__staged.sql
ALTER TABLE orders ADD COLUMN region VARCHAR(10);
UPDATE orders SET region = 'EU' WHERE region IS NULL;
ALTER TABLE orders ALTER COLUMN region SET NOT NULL;
```

## Case: a NOT NULL in a string or a comment is not a column definition

covers: detect:strings

```sql path=db/migration/V8__strings.sql
ALTER TABLE orders ADD COLUMN note VARCHAR(10) DEFAULT 'NOT NULL';
ALTER TABLE orders ADD COLUMN info VARCHAR(10); -- NOT NULL later
```

# Tests: sql/dynamic-sql-concatenation

A `-- FLAG` marker ends each line that must produce a finding.

## Case: a concatenated EXEC (Examples: Flag)

covers: example:flag,required:concatenated-exec

```sql path=db/procs/find_user.sql
CREATE PROCEDURE find_user @name nvarchar(100) AS
BEGIN
  EXEC('SELECT * FROM users WHERE name = ''' + @name + '''');  -- FLAG
END
```

## Case: sp_executesql with parameters (Examples: OK)

covers: example:ok,do-not-flag:sp-executesql,required:sp-executesql

```sql path=db/procs/find_user.sql
CREATE PROCEDURE find_user @name nvarchar(100) AS
BEGIN
  EXEC sp_executesql N'SELECT * FROM users WHERE name = @n', N'@n nvarchar(100)', @n = @name;
END
```

## Case: EXEC of a variable built by concatenation

covers: detect:built-variable

```sql path=db/procs/search.sql
CREATE PROCEDURE search @col sysname, @v nvarchar(50) AS
BEGIN
  DECLARE @sql nvarchar(max);
  SET @sql = N'SELECT * FROM t WHERE ' + @col + N' = ''' + @v + '''';
  EXEC (@sql);  -- FLAG
END
```

## Case: EXEC of a variable built only from constants (Do not flag)

covers: do-not-flag:constants

```sql path=db/procs/constants.sql
CREATE PROCEDURE cleanup AS
BEGIN
  DECLARE @sql nvarchar(max);
  SET @sql = N'DELETE FROM ' + N'tmp_table';
  SET @sql = @sql + N' WHERE 1 = 1';
  EXEC('DELETE FROM ' + 'tmp_table');
  EXEC (@sql);
END
```

## Case: Oracle EXECUTE IMMEDIATE

covers: detect:execute-immediate

```sql path=db/procs/oracle.sql
CREATE PROCEDURE drop_it(t VARCHAR2) AS
BEGIN
  EXECUTE IMMEDIATE 'DROP TABLE ' || t;  -- FLAG
  EXECUTE IMMEDIATE 'DELETE FROM t WHERE id = :1' USING t;
END;
```

## Case: PL/pgSQL EXECUTE with concatenation, USING and format

covers: do-not-flag:using,do-not-flag:format,required:format

```sql path=db/functions/pg.sql
CREATE FUNCTION f(tbl text, v text) RETURNS void AS $$
BEGIN
  EXECUTE 'DELETE FROM ' || tbl || ' WHERE x = 1';  -- FLAG
  EXECUTE 'DELETE FROM t WHERE x = $1' USING v;
  EXECUTE format('DELETE FROM %I WHERE x = %L', tbl, v);
  EXECUTE 'DELETE FROM ' || quote_ident(tbl);
END;
$$ LANGUAGE plpgsql;
```

## Case: a concatenation inside a string or a comment

covers: detect:string-and-comment

```sql path=db/procs/text.sql
-- EXEC('a' + @x)
SELECT 'EXEC(''a'' + @x)';
```

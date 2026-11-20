# Tests: python/raise-without-cause

A `# FLAG` marker ends each line that must produce a finding.

## Case: a new exception without from (Examples: Flag)

covers: example:flag,required:no-from

```python path=app/config.py
try:
    load()
except KeyError:
    raise ConfigError("missing key")  # FLAG
```

## Case: from e (Examples: OK)

covers: example:ok,do-not-flag:from-e,required:from-e

```python path=app/config.py
try:
    load()
except KeyError as e:
    raise ConfigError("missing key") from e
except OSError as err:
    raise ConfigError("io") from err.__cause__
```

## Case: a bare raise and a re-raise of the name (Do not flag)

covers: do-not-flag:bare-raise,required:bare

```python path=app/config.py
try:
    load()
except KeyError as e:
    log.error("x")
    raise
except OSError as err:
    raise err
```

## Case: from None that discards the original

covers: detect:from-none

```python path=app/config.py
try:
    load()
except KeyError as e:
    raise ConfigError("missing key") from None  # FLAG
```

## Case: from None whose message uses the original, or that has a comment (Do not flag)

covers: do-not-flag:from-none-explained

```python path=app/config.py
try:
    load()
except KeyError as e:
    raise ConfigError(f"missing key {e}") from None
except OSError:
    # the OS error text is noise for the user
    raise ConfigError("io") from None
except ValueError:
    raise ConfigError("value") from None  # hidden on purpose
```

## Case: a raise outside an except block, and in a function inside one

covers: detect:in-except

```python path=app/config.py
def f():
    raise ConfigError("x")

try:
    load()
except KeyError:
    def g():
        raise ConfigError("inner")
```

## Case: a test file

covers: detect:test-file

```python path=tests/test_config.py
try:
    load()
except KeyError:
    raise ConfigError("missing key")
```

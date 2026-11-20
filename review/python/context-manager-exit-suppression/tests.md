# Tests: python/context-manager-exit-suppression

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: __exit__ returning True (Examples: Flag)

covers: example:flag,required:exit-true

```python path=app/ctx.py
class Guard:
    def __exit__(self, exc_type, exc, tb):  # CANDIDATE
        self.log.append(exc)
        return True
```

## Case: __exit__ returning None or False (Examples: OK)

covers: example:ok,required:exit-none

```python path=app/ctx.py
class Guard:
    def __exit__(self, exc_type, exc, tb):
        self.log.append(exc)
        return False
class Other:
    def __exit__(self, exc_type, exc, tb):
        return None
```

## Case: a contextmanager that swallows

covers: detect:generator

```python path=app/ctx.py
@contextmanager
def guard():  # CANDIDATE
    try:
        yield
    except Exception:
        log.error("failed")

@contextmanager
def fine():
    try:
        yield
    except Exception:
        log.error("failed")
        raise
```

## Case: an unguarded cleanup in __exit__

covers: detect:cleanup

```python path=app/ctx.py
class Conn:
    def __exit__(self, exc_type, exc, tb):  # CANDIDATE
        self.conn.close()

class Safe:
    def __exit__(self, exc_type, exc, tb):
        try:
            self.conn.close()
        except Exception:
            log.warning("close failed")
```

## Case: suppress(Exception) and a narrow suppress

covers: detect:suppress,do-not-flag:narrow-suppress

```python path=app/ctx.py
def a():
    with contextlib.suppress(Exception):  # CANDIDATE
        db.commit()
    with suppress(FileNotFoundError):
        os.remove(path)
```

## Case: a test file

covers: detect:test-file

```python path=tests/test_ctx.py
class Guard:
    def __exit__(self, a, b, c):
        return True
```

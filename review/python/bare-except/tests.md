# Tests: python/bare-except

A `# FLAG` marker ends each line that must produce a finding.

## Case: a bare except that only logs (Examples: Flag)

covers: example:flag,required:bare

```python path=app/job.py
try:
    run()
except:  # FLAG
    log.error("failed")
```

## Case: a named exception (Examples: OK)

covers: example:ok,do-not-flag:specific,required:specific

```python path=app/job.py
try:
    run()
except ValueError:
    log.error("failed")
except Exception:
    log.error("broad, but another rule's business")
except (KeyError, IndexError) as e:
    log.error("failed %s", e)
```

## Case: BaseException

covers: detect:base-exception

```python path=app/job.py
try:
    run()
except BaseException:  # FLAG
    log.error("failed")
except builtins.BaseException as e:  # FLAG
    log.error("failed")
```

## Case: a handler that re-raises after cleanup (Do not flag)

covers: do-not-flag:re-raise,required:re-raise

```python path=app/job.py
try:
    run()
except:
    cleanup()
    raise
```

## Case: a re-raise that is not the last statement

covers: detect:raise-last

```python path=app/job.py
try:
    run()
except:  # FLAG
    raise
    cleanup()
```

## Case: a test file (Do not flag: tests)

covers: do-not-flag:test-file

```python path=tests/test_job.py
try:
    run()
except:
    pass
```

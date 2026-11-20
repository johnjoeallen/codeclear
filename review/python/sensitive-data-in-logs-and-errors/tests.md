# Tests: python/sensitive-data-in-logs-and-errors

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a password in a log call (Examples: Flag)

covers: example:flag,required:password

```python path=app/auth.py
def login(user, password):
    logger.info("login %s %s", user, password)  # CANDIDATE
```

## Case: masked values and names that are not sensitive (Examples: OK)

covers: example:ok,do-not-flag:masked,required:masked

```python path=app/auth.py
def login(user, password, token_count):
    logger.info("login %s %s", user, mask(password))
    logger.info("tokens %d", token_count)
    logger.info("password length %d", len(password))
    print("done")
```

## Case: other names, f-strings and whole requests

covers: detect:names

```python path=app/auth.py
def f(request, api_key, authorization):
    logging.debug(f"key {api_key}")  # CANDIDATE
    log.warning("auth %s", authorization)  # CANDIDATE
    logger.info("headers %s", request.headers)  # CANDIDATE
    logger.info("body %s", request.json)  # CANDIDATE
    print("card", card_number)  # CANDIDATE
```

## Case: an error response with the exception text

covers: detect:error-response

```python path=app/views.py
def view(req):
    try:
        run()
    except Exception as e:
        return {"error": str(e)}  # CANDIDATE

def other(req):
    try:
        run()
    except Exception:
        return traceback.format_exc()  # CANDIDATE

def fine(req):
    try:
        run()
    except Exception:
        log.exception("failed")
        return {"error": "internal"}
```

## Case: debug mode

covers: detect:debug

```python path=app/settings.py
DEBUG = True  # CANDIDATE
app.run(debug=True)  # CANDIDATE
DEBUG = False
app.run(debug=False)
```

## Case: debug mode in a dev settings file and a test file (Do not flag)

covers: do-not-flag:test-code

```python path=app/settings_dev.py
DEBUG = True
```

```python path=tests/test_auth.py
logger.info("login %s", password)
```

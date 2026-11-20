# Tests: python/token-verification-and-session-config

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: jwt.decode without verification (Examples: Flag)

covers: example:flag,required:no-verify

```python path=app/auth.py
claims = jwt.decode(token, options={"verify_signature": False})  # CANDIDATE
```

## Case: pinned algorithms (Examples: OK)

covers: example:ok,do-not-flag:pinned,required:pinned

```python path=app/auth.py
claims = jwt.decode(token, key, algorithms=["RS256"], audience=AUD, issuer=ISS)
```

## Case: missing and unsafe algorithms

covers: detect:algorithms

```python path=app/auth.py
a = jwt.decode(token, key)  # CANDIDATE
b = jwt.decode(token, key, algorithms=["HS256", "none"])  # CANDIDATE
c = jwt.decode(token, key, algorithms=["HS256"], verify=False)  # CANDIDATE
```

## Case: cookies and session settings

covers: detect:cookies,required:cookies

```python path=app/web.py
resp.set_cookie("sid", sid)  # CANDIDATE
resp.set_cookie("sid", sid, secure=True, httponly=True, samesite="Lax")
SESSION_COOKIE_SECURE = False  # CANDIDATE
CSRF_COOKIE_HTTPONLY = False  # CANDIDATE
SESSION_COOKIE_SECURE = True
```

## Case: csrf_exempt and CORS

covers: detect:csrf-cors

```python path=app/web.py
@csrf_exempt  # CANDIDATE
def view(request):
    pass

app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=True)  # CANDIDATE
app.add_middleware(CORSMiddleware, allow_origins=["https://a.io"], allow_credentials=True)
CORS_ALLOW_ALL_ORIGINS = True  # CANDIDATE
```

## Case: X-Forwarded headers

covers: detect:forwarded

```python path=app/web.py
ip = request.headers["X-Forwarded-For"]  # CANDIDATE
```

```python path=app/web2.py
app = ProxyFix(app)
ip = request.headers["X-Forwarded-For"]
```

## Case: a test file

covers: detect:test-file

```python path=tests/test_auth.py
claims = jwt.decode(token, options={"verify_signature": False})
```

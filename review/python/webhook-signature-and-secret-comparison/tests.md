# Tests: python/webhook-signature-and-secret-comparison

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: == on a signature (Examples: Flag)

covers: example:flag,required:equals

```python path=app/hooks.py
def check(request, secret):
    expected = hmac.new(secret, request.body, "sha256").hexdigest()
    if request.headers["X-Signature"] == expected:  # CANDIDATE
        return True
```

## Case: compare_digest (Examples: OK)

covers: example:ok,do-not-flag:compare-digest,required:compare-digest

```python path=app/hooks.py
def check(request, secret):
    expected = hmac.new(secret, request.body, "sha256").hexdigest()
    return hmac.compare_digest(request.headers["X-Signature"], expected)
```

## Case: other secret names and !=

covers: detect:names

```python path=app/hooks.py
def f(token, api_key, sig, digest, user):
    a = token != stored  # CANDIDATE
    b = api_key == other  # CANDIDATE
    c = sig == mac.hexdigest()  # CANDIDATE
    d = user.token == given  # CANDIDATE
```

## Case: comparison with None, a literal or an empty string (Do not flag)

covers: do-not-flag:none-and-literals

```python path=app/hooks.py
def f(token, signature):
    if token is None:
        return
    a = token == ""
    b = signature != None
    c = token == 0
```

## Case: a webhook handler that never verifies

covers: detect:handler

```python path=app/hooks.py
@app.route("/webhook", methods=["POST"])
def stripe_webhook():  # CANDIDATE
    event = request.get_json()
    process(event)
    return "ok"
```

## Case: handlers that verify or check a timestamp (Do not flag)

covers: do-not-flag:verified-handler

```python path=app/hooks.py
def github_webhook():
    payload = request.get_data()
    verify_signature(payload, request.headers["X-Hub-Signature-256"])
    process(payload)

def stripe_webhook():
    payload = request.data
    event = stripe.Webhook.construct_event(payload, sig, secret)

def callback():
    body = request.json
    if time.time() - body["timestamp"] > 300:
        return
```

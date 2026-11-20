# python/webhook-signature-and-secret-comparison: Missing webhook signature/replay checks and timing-unsafe secret comparison

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Security |

## Intent
Secrets, tokens and HMAC signatures compared with `==` leak timing information; webhook handlers that the protocol requires to verify signatures (and timestamps against replay) process unauthenticated payloads.

## Detect
Candidates:
- `==`/`!=` where one operand is named or assigned from `signature`, `sig`, `token`, `secret`, `api_key`, `hmac`, `hexdigest()`/`digest()` result;
- route handlers named/routed as `webhook`/`callback` that read the body and never call a verification function (`hmac.compare_digest`, provider `construct_event`/`verify`) and never compare a timestamp.

## Do not flag
- `hmac.compare_digest`/`secrets.compare_digest`; provider SDK verification; handlers authenticated by mTLS/IP allow-list shown.

## Examples
Flag:
```python
if request.headers["X-Signature"] == expected: ...
```
OK:
```python
if hmac.compare_digest(request.headers["X-Signature"], expected): ...
```

## Implementation notes
Python `ast` name heuristics for comparisons; webhook route detection by decorator path/function name.

## AI prompt
Question: "Does the provider protocol require signature verification and replay protection here, and does a visible middleware already perform it?" Supply the route, middleware and provider docs/config if shown. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Why hybrid
The comparison pattern is syntactic; whether a route needs verification depends on the protocol and middleware.

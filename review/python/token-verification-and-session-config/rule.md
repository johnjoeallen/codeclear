# python/token-verification-and-session-config: JWT verification, cookies/session, CSRF, CORS and proxy trust

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | hybrid |
| Source | python-review: Security |

## Intent
Tokens accepted without signature/algorithm/issuer/audience/expiry verification; insecure cookie or session settings; CSRF protection disabled on cookie-authenticated endpoints; wildcard CORS with credentials; trusting forwarded headers from untrusted peers.

## Detect
Candidates:
- `jwt.decode(..., options={"verify_signature": False})`, `verify=False`, missing `algorithms=`, or `algorithms` containing `none`; no `audience`/`issuer` where the call otherwise accepts third-party tokens;
- `set_cookie(...)`/`SESSION_COOKIE_*` without `secure`/`httponly`/`samesite`; `@csrf_exempt` on POST views using session auth;
- `CORSMiddleware(allow_origins=["*"], allow_credentials=True)`, `CORS_ALLOW_ALL_ORIGINS=True` with credentials;
- `X-Forwarded-*` used for client IP/scheme without a trusted-proxy setting.

## Do not flag
- Decode with signature verification disabled only to read the `kid` header before verification; endpoints using bearer tokens (CSRF-immune); tests.

## Examples
Flag:
```python
claims = jwt.decode(token, options={"verify_signature": False})
```
OK:
```python
claims = jwt.decode(token, key, algorithms=["RS256"], audience=AUD, issuer=ISS)
```

## Implementation notes
Python `ast` plus settings-file keyword scan; anchor to the call/setting.

## AI prompt
Question: "Given the actual authentication model, can an attacker exploit this configuration (forged token, CSRF, cross-origin credentialed read, spoofed IP)?" Supply the auth flow and middleware. Report only with a concrete trigger scenario; public endpoints are not missing authentication by default; otherwise NO_FINDING.

## Why hybrid
Insecure options are keyword-detectable; exploitability depends on the authentication model.

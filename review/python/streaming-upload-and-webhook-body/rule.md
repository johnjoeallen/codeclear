# python/streaming-upload-and-webhook-body: Streaming failures after headers, body/upload limits and raw-body signatures

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | RELIABILITY |
| Severity | MEDIUM |
| Confidence | LOW |
| Detection type | ai |
| Source | python-review: Web frameworks and backend boundaries |

## Intent
A streaming response that fails after headers are sent cannot change its status; uploads and bodies without size limits exhaust memory/disk; webhook signature verification must use the exact raw body, not re-serialized parsed JSON.

## Why AI
Needs framework limits (`DATA_UPLOAD_MAX_MEMORY_SIZE`, proxy limits), streaming implementation and how the signature is computed.

## AI prompt
"If this stream fails mid-way, what does the client observe and is partial data treated as complete? Is the upload/body size bounded by code or visible config? Is the webhook signature computed over the raw bytes?" Supply the route, framework/proxy config and verification code. Report only with a concrete trigger scenario; otherwise NO_FINDING.

## Do not flag
- Limits enforced in shown middleware/config; signature computed on `request.body`/`await request.body()`.

## Examples
Flag:
```python
payload = await request.json()
verify(sig, json.dumps(payload).encode())      # not the raw body
```
OK:
```python
raw = await request.body(); verify(sig, raw); payload = json.loads(raw)
```

## Evaluation cases
1. Above flag example -> flag.
2. `content = await file.read()` on an upload endpoint with no size limit anywhere shown -> flag.
3. Generator response raising after first chunk, client expected to detect truncation without checksum -> flag.
4. Upload limited by shown `client_max_body_size` and `MAX_UPLOAD` check -> no finding.

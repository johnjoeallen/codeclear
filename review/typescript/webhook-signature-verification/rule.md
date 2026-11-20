# typescript/webhook-signature-verification: Webhook signature, raw body and replay protection

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | node-review: HTTP frameworks and backend boundaries; node-review: Security |

## Intent
A webhook that acts on a payload must verify its signature over the raw body with a constant-time comparison and, where the provider defines it, reject replays (timestamp/nonce). A JSON-parsed body re-serialized for hashing fails or can be bypassed.

## Why AI
Whether the endpoint is a signed webhook and what the provider protocol requires is outside the code.

## AI prompt
"Is this a webhook the provider signs? Is the signature verified over the raw bytes (route-level `express.raw`/`rawBody`), compared in constant time, with timestamp tolerance/replay protection if the protocol requires it? Name the forged or replayed request that is accepted. Do not call public endpoints unauthenticated without a restricted-access contract." Evidence: route, body-parser order, provider docs/ticket.

## Do not flag
- Endpoints verified by the provider SDK (`stripe.webhooks.constructEvent`) with raw body.

## Examples
Flag:
```ts
app.use(express.json()); app.post('/hook', (req, res) => { if (sig(JSON.stringify(req.body)) === req.headers['x-sig']) ... });
```
OK:
```ts
app.post('/hook', express.raw({ type: '*/*' }), (req, res) => { crypto.timingSafeEqual(...) });
```

## Evaluation cases
1. HMAC over `JSON.stringify(req.body)`: flag.
2. `===` comparison of signatures: flag.
3. SDK `constructEvent(rawBody, sig, secret)`: ok.
4. No signature check, provider requires one: flag.

# python/untrusted-payload-runtime-validation: Type hints treated as validation of untrusted payloads

| Field | Value |
|---|---|
| Applies to | `**/*.py` |
| Category | CORRECTNESS |
| Severity | MEDIUM |
| Confidence | MEDIUM |
| Detection type | ai |
| Source | python-review: Values, state, and control flow; python-bug-hunter: Trace the execution before judging it |

## Intent
`TypedDict`, dataclasses, annotations and decorators do not validate JSON, message or form data at runtime. Unvalidated nested fields reach business logic or persistence.

## Why AI
Validation may live in Pydantic config (`strict`, `extra`), a framework dependency, middleware or a decorator in another file; only tracing to first use shows whether it runs.

## AI prompt
"Trace this untrusted payload from the entry point to its first use. Is each used field (including nested ones) validated at runtime for type, range and presence, given the actual model configuration and decorators?" Supply the handler, model/schema definitions, decorators/middleware and Pydantic config. Report only with a concrete trigger scenario (malformed input, wrong outcome, changed line); otherwise NO_FINDING.

## Do not flag
- Pydantic/marshmallow/DRF serializers actually invoked on the path.
- Internal data produced by trusted code.

## Examples
Flag:
```python
def handle(msg: OrderDict):        # TypedDict, json.loads upstream
    total = msg["items"][0]["price"] * msg["qty"]
```
OK:
```python
order = Order.model_validate(json.loads(raw))
```

## Evaluation cases
1. `@dataclass Order` built via `Order(**json.loads(body))` and used for arithmetic -> flag.
2. FastAPI handler with `order: OrderModel` body param -> no finding.
3. Model has `model_config = ConfigDict(extra="allow")` and an unvalidated extra key is written to the DB -> flag.
4. Decorator named `@validate` that is shown to only log -> flag.

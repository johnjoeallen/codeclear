# typescript/unchecked-network-data: Type assertion or non-null assertion on network data

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.tsx` |
| Category | CORRECTNESS |
| Severity | LOW |
| Confidence | LOW |
| Detection type | hybrid |
| Source | typescript-review: Correctness |

## Intent
`as T` and `!` only silence the compiler. If the server response lacks the field, access fails at run time.

## Detect
- `(await res.json()) as T` or `response.data as T` with no validation call before first use.
- `!` non-null assertion on a property read from `fetch`, `axios`, or `JSON.parse` results.

## Do not flag
- Data passed through a schema validator (`zod`, `io-ts`, `yup`, `ajv`) before the assertion.
- Assertions in test files.

## Examples
Flag:
```ts
const user = (await res.json()) as User;
```
OK:
```ts
const user = UserSchema.parse(await res.json());
```

## Implementation notes
Candidate detection only; the AI reviewer decides whether the field can really be absent.

Detected on `x as T` and on `x!` where `x` contains `.json()`, `.data`, or `JSON.parse(`, or is a name that the function assigned (one step) from something mentioning `json`, `JSON`, `axios` or `fetch`. `as unknown`, `as any` and `as const` are not flagged. A function that mentions a validator (`zod`, `io-ts`, `yup`, `ajv`, `valibot`, `superstruct`, `safeParse`, `validate(`, `Joi`, `typebox`) or calls `.parse(` on the data is skipped; test files are skipped.

## AI prompt
For each candidate, can the asserted shape be absent or different in the real response, and does the code then fail on first use? Check for validation earlier on the path. Report only with a concrete failing input. Evidence: the call site, the type definition, any server contract in the supplied files.

## Why hybrid
The assertion is syntactic, but whether the server can omit the field is contract knowledge.

## Required tests
`json() as T` (finding), `UserSchema.parse` (no finding), `data!.name` on `JSON.parse` result (finding).

# typescript/command-injection: Shell command built from untrusted input

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | CRITICAL |
| Confidence | HIGH |
| Detection type | hybrid |
| Source | node-review: Security |

## Intent
`exec` and `spawn({ shell: true })` interpret input as shell syntax. Argument-array APIs without a shell are far safer and need separate scrutiny.

## Detect
- `child_process.exec/execSync` (or `shelljs.exec`) with a template/concatenated string containing non-literal values.
- `spawn/execFile` with `{ shell: true }` and non-literal arguments.

## Do not flag
- `execFile`/`spawn` with an args array and no shell, constant commands, allow-listed values.

## Examples
Flag:
```ts
exec(`convert ${req.query.file} out.png`);
```
OK:
```ts
execFile('convert', [file, 'out.png']);
```

## Implementation notes
Anchor to the call. Equivalent: `security/detect-child-process`.

## Required tests
Interpolated `exec` (finding), `execFile` array (no finding), `spawn` with `shell: true` and variable (finding), constant command (no finding).

## AI prompt
"Is the interpolated value attacker-controlled and unsanitized? For arg-array APIs, can the value be parsed as an option (leading `-`)?" Evidence: call, value origin, validation.

## Why hybrid
The sink is syntactic; taint needs data flow and validation context.

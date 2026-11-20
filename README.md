# codeclear
CodeClear: AI code review that gets your branch clear before it ever reaches a PR/MR.

## Command-line review

The launcher scripts review the Java, SQL, YAML, JSON and properties files under the paths you give; with none, the current folder. No server starts and no
browser opens.

```
./codeclear.sh src                                 # review the Java, SQL and config files under src
./codeclear.sh                                     # review the current folder
./codeclear.sh --diff=main --fail-on=high          # only lines changed since main; exit 1 on a high finding
./codeclear.sh --format=json src > report.json
./codeclear.sh --report=out/review.md src          # choose where the Markdown report goes (--report=none to skip it)
./codeclear.sh --help
./codeclear.sh serve                               # start the web application instead
```

Every review also writes `target/codeclear/report.md` inside the folder that was scanned: a summary, then each finding with a numbered snippet of the code around
it, the candidates the AI could not judge, and the rules that were not checked. `codeclear.ps1` takes the same arguments on Windows. The review needs no AI backend: it runs every rule that has a
deterministic detector and lists the ones it could not check.

To ignore a finding on purpose, put a comment that starts with `CodeClear:` and gives the reason. On a line of its own it ignores
the next non-blank line; after code it ignores its own line (`// CodeClear: the caller retries`, `/* CodeClear: ... */`, and `# CodeClear: ...` in
YAML, properties, Python and shell files). It ignores every finding on that line, or only the rules it names in parentheses
(`// CodeClear(java/empty-catch-block, java/log-and-rethrow): the caller handles it`), and the report lists each ignored finding under "Suppressed" (rule and line, marked suppressed, no code). See `docs/design.md` (sections 2.2 and 7.6).

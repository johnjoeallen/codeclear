# codeclear
CodeClear: AI code review that gets your branch clear before it ever reaches a PR/MR.

## Command-line review

```
./codeclear.sh review src                       # review the Java files under src
./codeclear.sh review --diff=main --fail-on=high   # only lines changed since main; exit 1 on a high finding
./codeclear.sh review --format=json src > report.json
./codeclear.sh review --help
```

`codeclear.ps1` takes the same arguments on Windows. The review needs no AI backend: it runs every rule that has a
deterministic detector and lists the ones it could not check. See `docs/design.md` (sections 2.2 and 7.6).

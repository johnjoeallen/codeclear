# codeclear
CodeClear: AI code review that gets your branch clear before it ever reaches a PR/MR.

## Command-line review

The launcher scripts review the Java files under the paths you give; with none, the current folder. No server starts and no
browser opens.

```
./codeclear.sh src                                 # review the Java files under src
./codeclear.sh                                     # review the current folder
./codeclear.sh --diff=main --fail-on=high          # only lines changed since main; exit 1 on a high finding
./codeclear.sh --format=json src > report.json
./codeclear.sh --help
./codeclear.sh serve                               # start the web application instead
```

`codeclear.ps1` takes the same arguments on Windows. The review needs no AI backend: it runs every rule that has a
deterministic detector and lists the ones it could not check. See `docs/design.md` (sections 2.2 and 7.6).

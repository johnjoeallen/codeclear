# Tests: typescript/command-injection

A `// CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: exec with a template string (Examples: Flag)

covers: example:flag,required:exec-template

```typescript path=src/run.ts
import { exec } from 'child_process';
export function convert(name: string) {
  exec(`convert ${name} out.png`);  // CANDIDATE
}
```

## Case: execFile with an array (Examples: OK)

covers: example:ok,do-not-flag:array-no-shell,required:exec-file

```typescript path=src/run.ts
import { execFile, spawn, exec } from 'child_process';
export function convert(name: string) {
  execFile('convert', [name, 'out.png']);
  spawn('convert', [name]);
  exec('ls -l');
}
```

## Case: concatenation, execSync and shelljs

covers: detect:forms

```typescript path=src/run.ts
import * as cp from 'child_process';
export function a(name: string) {
  cp.execSync('convert ' + name);  // CANDIDATE
  cp.exec("tar xf " + name + ".tar");  // CANDIDATE
}
```

## Case: spawn with shell true

covers: detect:shell-true

```typescript path=src/run.ts
import { spawn } from 'child_process';
export function a(cmd: string) {
  spawn(cmd, ['-l'], { shell: true });  // CANDIDATE
  spawn('ls', ['-l'], { shell: true });
}
```

## Case: exec in a file that does not use child_process

covers: detect:other-exec

```typescript path=src/regex.ts
export function a(s: string) {
  return /x/.exec(s + 'y');
}
```

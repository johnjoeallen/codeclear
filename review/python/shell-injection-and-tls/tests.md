# Tests: python/shell-injection-and-tls

A `# FLAG` marker ends each line that must produce a finding.

## Case: an f-string through the shell, and verify=False (Examples: Flag)

covers: example:flag,required:shell-true

```python path=app/run.py
subprocess.run(f"convert {name}", shell=True)  # FLAG
requests.get(url, verify=False, timeout=5)  # FLAG
```

## Case: an argument list (Examples: OK)

covers: example:ok,required:list

```python path=app/run.py
subprocess.run(["convert", name], check=True)
```

## Case: shell=True with a constant string (Do not flag)

covers: do-not-flag:constant-command,required:constant

```python path=app/run.py
subprocess.run("ls -l", shell=True)
subprocess.call("ls " "-l", shell=True)
os.system("clear")
```

## Case: the other subprocess functions and os.system / os.popen

covers: detect:commands

```python path=app/run.py
subprocess.call(cmd, shell=True)  # FLAG
subprocess.check_output("ls " + path, shell=True)  # FLAG
subprocess.Popen(cmd, shell=True)  # FLAG
subprocess.run(cmd, shell=False)
os.system(cmd)  # FLAG
os.popen("ls " + path)  # FLAG
```

## Case: unverified TLS

covers: detect:tls,required:tls

```python path=app/run.py
ctx = ssl._create_unverified_context()  # FLAG
ctx.verify_mode = ssl.CERT_NONE  # FLAG
ctx.check_hostname = False  # FLAG
ok = ssl.create_default_context()
ctx.check_hostname = True
```

## Case: verify=False in a test file (Do not flag)

covers: do-not-flag:test-file

```python path=tests/test_run.py
requests.get(url, verify=False, timeout=5)
```

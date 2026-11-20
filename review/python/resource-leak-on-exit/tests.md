# Tests: python/resource-leak-on-exit

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: open without a with block (Examples: Flag)

covers: example:flag,required:open

```python path=app/io.py
def read(path):
    f = open(path)  # CANDIDATE
    data = f.read()
    f.close()
    return data
```

## Case: with, finally, and closing (Examples: OK)

covers: example:ok,do-not-flag:with-and-finally,required:with

```python path=app/io.py
def a(path):
    with open(path) as f:
        return f.read()

def b(path):
    f = open(path)
    try:
        return f.read()
    finally:
        f.close()

def c(url):
    with closing(urlopen(url)) as r:
        return r.read()
```

## Case: sockets, subprocesses, sessions and streams

covers: detect:resources

```python path=app/io.py
def a():
    s = socket.socket()  # CANDIDATE
    s.connect(("h", 80))

def b():
    p = subprocess.Popen(["ls"])  # CANDIDATE
    p.wait()

def c(url):
    r = requests.get(url, stream=True)  # CANDIDATE
    size = len(r.content)
    return size
```

## Case: a lock acquired without a finally

covers: detect:lock

```python path=app/io.py
def a(lock):
    lock.acquire()  # CANDIDATE
    work()
    lock.release()

def b(lock):
    lock.acquire()
    try:
        work()
    finally:
        lock.release()
```

## Case: a handle that is returned or kept (Do not flag)

covers: do-not-flag:handed-on

```python path=app/io.py
def a(path):
    f = open(path)
    return f

class C:
    def __init__(self, path):
        self.f = open(path)
```

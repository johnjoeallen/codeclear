# Tests: python/untrusted-archive-and-ssrf

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: extractall on an upload (Examples: Flag)

covers: example:flag,required:extractall

```python path=app/upload.py
import tarfile
def handle(request):
    tar = tarfile.open(fileobj=request.files["a"])
    tar.extractall("/data")  # CANDIDATE
```

## Case: a data filter (Examples: OK)

covers: example:ok,do-not-flag:filter,required:filter

```python path=app/upload.py
import tarfile
def handle(request):
    tar = tarfile.open(fileobj=request.files["a"])
    tar.extractall("/data", filter="data")
```

## Case: zip and shutil

covers: detect:archives

```python path=app/upload.py
import zipfile
def handle(request):
    z = zipfile.ZipFile(request.files["a"])
    z.extractall("/data")  # CANDIDATE
    shutil.unpack_archive(request.files["a"].filename, "/data")  # CANDIDATE
```

## Case: a URL from the request

covers: detect:ssrf,required:ssrf

```python path=app/proxy.py
def fetch(request):
    r = requests.get(request.args["url"], timeout=5)  # CANDIDATE
    target = request.json["target"]
    s = httpx.get(target)  # CANDIDATE
    return r.text
```

## Case: an allow-list, redirects off, and a constant URL (Do not flag)

covers: do-not-flag:allow-list

```python path=app/proxy.py
def a(request):
    if urlparse(request.args["url"]).hostname not in ALLOWED_HOSTS:
        return
    return requests.get(request.args["url"], timeout=5)

def b(request):
    return requests.get(request.args["url"], timeout=5, allow_redirects=False)

def c():
    return requests.get("https://api.acme.io/v1", timeout=5)
```

## Case: an archive that does not come from a request

covers: detect:trusted-archive

```python path=app/job.py
import tarfile
def nightly():
    tar = tarfile.open("/var/backups/a.tar")
    tar.extractall("/restore")
```

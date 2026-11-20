# Tests: python/http-call-without-timeout

A `# FLAG` marker ends each line that must produce a finding.

## Case: requests.get without a timeout (Examples: Flag)

covers: example:flag,required:no-timeout

```python path=app/client.py
r = requests.get(url)  # FLAG
```

## Case: a timeout tuple (Examples: OK)

covers: example:ok,do-not-flag:timeout,required:timeout

```python path=app/client.py
r = requests.get(url, timeout=(3.05, 10))
s = requests.post(url, json=body, timeout=5)
```

## Case: the other verbs, urlopen, and timeout=None

covers: detect:verbs,required:none

```python path=app/client.py
a = requests.post(url, data=d)  # FLAG
b = requests.put(url)  # FLAG
c = requests.patch(url)  # FLAG
d = requests.delete(url)  # FLAG
e = requests.head(url)  # FLAG
f = requests.options(url)  # FLAG
g = requests.request("GET", url)  # FLAG
h = urllib.request.urlopen(url)  # FLAG
i = urlopen(url)  # FLAG
j = requests.get(url, timeout=None)  # FLAG
k = urlopen(url, timeout=5)
```

## Case: httpx and other libraries (Do not flag)

covers: do-not-flag:httpx

```python path=app/client.py
a = httpx.get(url)
b = aiohttp.request("GET", url)
c = client.get(url)
```

## Case: a requests Session

covers: detect:session

```python path=app/client.py
import requests
session = requests.Session()
a = session.get(url)  # FLAG
b = session.post(url, timeout=3)
```

## Case: a Session with a timeout adapter in the same file (Do not flag)

covers: do-not-flag:adapter

```python path=app/client.py
import requests
from requests.adapters import HTTPAdapter
session = requests.Session()
session.mount("https://", TimeoutAdapter(timeout=5))
a = session.get(url)
```

## Case: options passed as **kwargs

covers: detect:kwargs

```python path=app/client.py
a = requests.get(url, **options)
```

## Case: a test file

covers: detect:test-file

```python path=tests/test_client.py
a = requests.get(url)
```

# Tests: python/test-defects

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: an async test with no marker (Examples: Flag)

covers: example:flag,required:async-no-marker

```python path=tests/test_api.py
async def test_fetch():  # CANDIDATE
    assert await fetch() == 1
```

## Case: a marked async test (Examples: OK)

covers: example:ok,do-not-flag:asyncio-mode,required:marked

```python path=tests/test_marked.py
@pytest.mark.asyncio
async def test_fetch():
    assert await fetch() == 1
```

```python path=tests/test_auto.py
pytestmark = pytest.mark.asyncio
async def test_fetch():
    assert await fetch() == 1
```

## Case: assertions that cannot fail

covers: detect:assertions

```python path=tests/test_assert.py
def test_a():
    assert (x == 1, "x must be 1")  # CANDIDATE
    assert True  # CANDIDATE
    assert x == 1, "x must be 1"
    self.assertTrue(a, b)  # CANDIDATE
    self.assertTrue(a, "message")
```

## Case: the clock, chance and the network

covers: detect:nondeterminism

```python path=tests/test_clock.py
def test_a():
    time.sleep(1)  # CANDIDATE
    now = datetime.now()  # CANDIDATE
    n = random.randint(1, 6)  # CANDIDATE
    r = requests.get("https://x")  # CANDIDATE
```

## Case: frozen time and a seeded random (Do not flag)

covers: do-not-flag:frozen-time

```python path=tests/test_frozen.py
from freezegun import freeze_time
@freeze_time("2024-01-01")
def test_a():
    now = datetime.now()
    time.sleep(0)
```

## Case: an integration test

covers: detect:integration

```python path=tests/integration/test_live.py
def test_a():
    r = requests.get("https://x")
```

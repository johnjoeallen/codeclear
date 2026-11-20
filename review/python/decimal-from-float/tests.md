# Tests: python/decimal-from-float

A `# FLAG` marker ends each line that must produce a finding.

## Case: a float literal (Examples: Flag)

covers: example:flag,required:float-literal

```python path=app/price.py
price = Decimal(19.99)  # FLAG
```

## Case: a string (Examples: OK)

covers: example:ok,do-not-flag:string,required:string

```python path=app/price.py
price = Decimal("19.99")
```

## Case: float() and arithmetic with a float, qualified

covers: detect:forms,required:float-call

```python path=app/price.py
a = Decimal(float(x))  # FLAG
b = decimal.Decimal(x * 1.1)  # FLAG
c = Decimal(-0.5)  # FLAG
d = Decimal((a + 2.5) / 3)  # FLAG
```

## Case: int, str(x), a name and from_float (Do not flag)

covers: do-not-flag:int-str-from-float,required:int

```python path=app/price.py
a = Decimal(5)
b = Decimal(str(x))
c = Decimal(x)
d = Decimal.from_float(1.5)
e = Decimal("1.5") * 2
f = Decimal(f"{x}")
```

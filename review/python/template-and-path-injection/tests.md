# Tests: python/template-and-path-injection

A `# CANDIDATE` marker ends each line that must produce a candidate for the AI.

## Case: a template from the request (Examples: Flag)

covers: example:flag,required:template

```python path=app/views.py
def page(request):
    return render_template_string(request.args["t"])  # CANDIDATE
```

## Case: a constant template with context variables (Examples: OK)

covers: example:ok,do-not-flag:constant-template,required:context-variable

```python path=app/views.py
def page(request):
    return render_template_string("<h1>{{ name }}</h1>", name=request.args["name"])
    return render_template("page.html", name=request.args["name"])
```

## Case: Template and from_string with a parameter

covers: detect:template-sinks

```python path=app/views.py
def a(source):
    return jinja2.Template(source).render()  # CANDIDATE
def b(env, text):
    return env.from_string(text).render()  # CANDIDATE
```

## Case: a path from the request

covers: detect:paths,required:path

```python path=app/files.py
def download(request):
    name = request.args["file"]
    return send_file(os.path.join("/data", name))  # CANDIDATE

def read(filename):
    return open(filename).read()  # CANDIDATE

def drop(request):
    os.remove(request.form["path"])  # CANDIDATE
```

## Case: secure_filename or a containment check (Do not flag)

covers: do-not-flag:checked-path

```python path=app/files.py
def a(request):
    name = secure_filename(request.args["file"])
    return send_file(os.path.join("/data", name))

def b(filename):
    p = (BASE / filename).resolve()
    if not p.is_relative_to(BASE):
        abort(404)
    return open(p).read()
```

## Case: constants

covers: detect:constants

```python path=app/files.py
def a():
    return open("/etc/app.conf").read()
    return render_template_string("<b>x</b>")
```

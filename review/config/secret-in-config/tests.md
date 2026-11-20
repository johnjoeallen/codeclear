# Tests: config/secret-in-config

A `# FLAG` marker ends each line that must produce a finding.

## Case: a literal password (Examples: Flag)

covers: example:flag,required:literal-password

```properties path=src/main/resources/application.properties
spring.datasource.password=Pr0dPassw0rd!  # FLAG
```

## Case: an environment placeholder (Examples: OK)

covers: example:ok,do-not-flag:placeholder,required:env-placeholder

```properties path=src/main/resources/application.properties
spring.datasource.password=${DB_PASSWORD}
api.token={{ vault_token }}
client.secret=<secret>
db.password=ENC(abc123==)
jwt.secret=vault:secret/data/app
aws.secret=secretsmanager:prod/aws
other.password=changeme
empty.password=
masked.password=******
```

## Case: an AWS key in a value of any key (finding)

covers: required:aws-key

```yaml path=src/main/resources/application.yml
storage:
  bucket: assets
  access: AKIAIOSFODNN7EXAMPL1  # FLAG
  gh: ghp_abcdefghijklmnopqrstuvwxyz0123456789  # FLAG
```

## Case: keys that name a secret location, not a secret (Do not flag)

covers: do-not-flag:location-key,required:password-file

```yaml path=src/main/resources/application.yml
db:
  passwordFile: /run/secrets/db
  secretName: prod-db
  token-url: https://auth.internal.local/token
  client-id: abc123
```

## Case: a URL with a password and a private key

covers: detect:formats

```yaml path=src/main/resources/application.yml
db:
  url: postgresql://admin:s3cr3tpw@db.internal.local/app  # FLAG
```

## Case: secret keys in YAML, nested and as list items

covers: detect:key-paths

```yaml path=src/main/resources/application.yml
auth:
  client_secret: abcd1234efgh  # FLAG
  apiKey: sk_live_9f8a7d6c  # FLAG
  keys:
    - private-key: abcdef123456  # FLAG
```

## Case: a test path (Do not flag)

covers: do-not-flag:test-path,required:test-path

```properties path=src/test/resources/application.properties
spring.datasource.password=Pr0dPassw0rd!
```

## Case: the finding does not contain the secret

covers: required:masked-output

```properties path=src/main/resources/application.properties
my.password=sup3rS3cretValue  # FLAG
```

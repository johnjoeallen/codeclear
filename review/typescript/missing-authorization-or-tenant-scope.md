# typescript/missing-authorization-or-tenant-scope: Authorization or tenant ownership not enforced on the path

| Field | Value |
|---|---|
| Applies to | `**/*.ts`, `**/*.js`, `**/*.mjs`, `**/*.cjs` |
| Category | SECURITY |
| Severity | HIGH |
| Confidence | LOW |
| Detection type | ai |
| Source | node-review: HTTP frameworks and backend boundaries; node-bug-hunter: Trace the execution before judging it |

## Intent
Every resource access must be authorized for the caller and scoped to its tenant/owner through the actual middleware, guard and query predicates. Looking up by id alone allows cross-tenant access (IDOR).

## Why AI
Guards may be global, in decorators, or in other files; the ownership model is business-defined. Public endpoints are not "missing authentication" without a restricted-access contract.

## AI prompt
"Trace the user/tenant identifier from the request through guard order, the DB predicate, downstream calls, cache keys and response mapping. Name the endpoint and request where user A reads or changes user B's resource, or authorization happens after the privileged action. Do not infer missing guards when global guards are unseen." Evidence: route, middleware/guard registration, repository query, tenancy model.

## Do not flag
- Endpoints documented as public.
- Queries already filtered by owner/tenant, or protected by a visible global guard.

## Examples
Flag:
```ts
app.get('/orders/:id', auth, async (req, res) => res.json(await Order.findByPk(req.params.id)));
```
OK:
```ts
Order.findOne({ where: { id: req.params.id, tenantId: req.user.tenantId } });
```

## Evaluation cases
1. Lookup by id only, auth middleware present: flag.
2. Predicate includes `tenantId` from the token: ok.
3. NestJS handler with no guard shown but a global `APP_GUARD` shown: ok.
4. Role check done after `delete()`: flag.

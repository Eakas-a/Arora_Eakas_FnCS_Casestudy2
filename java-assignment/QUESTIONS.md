# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yeah, I'd tidy this up. Right now there are basically three different ways of touching the
database in the same project:

- `Store` extends `PanacheEntity` directly (active record style, `store.persist()`, `Store.findById()`)
- `Product` goes through a separate `ProductRepository` that implements `PanacheRepository<Product>`
- `Warehouse` goes even further with a full hexagonal split - domain model, a `WarehouseStore`
  port, and a `WarehouseRepository`/`DbWarehouse` adapter that maps back and forth

None of these are wrong on their own, Panache actually supports both the active-record and
repository style on purpose. My concern is having all three side by side in one small
codebase - it means every new dev has to learn three conventions instead of one, and it's easy
to end up "translating" the same warehouse three times (API bean -> domain model -> DB entity)
for very little payoff when the module is this small.

If I were maintaining this long-term I'd pick one pattern per "type" of concern, not per
entity:
- Repository/DAO style everywhere data access is involved (drop the active-record `PanacheEntity`
  usage on `Store`), so persistence logic is testable without an actual database and doesn't leak
  into the resource layer.
- Keep the domain/port/adapter split for Warehouse specifically, because it has real business
  rules (capacity checks, replace/archive) that genuinely benefit from being independent of
  Hibernate. I wouldn't force that same ceremony onto `Product`, which is a plain CRUD entity with
  no business logic attached to it - it doesn't earn the extra layers.

Basically: the fancier architecture should track where the actual complexity lives, not be
applied uniformly just for consistency. I'd rather have "boring but consistent" for the simple
entities and "a bit more layered" for the one that actually needs it, than the same amount of
ceremony everywhere.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
I've worked both ways at LTIMindtree (mostly on Spring Boot services, but the trade-off is
identical). Contract-first (the Warehouse approach here) gives you a spec that's the single
source of truth: front-end and downstream teams can start against it before the implementation
even exists, you get generated request/response classes and basic validation for free, and the
yaml doubles as living documentation that can't drift from what's actually being served (or at
least drifts a lot less). The cost is friction during active development - every time you tweak
a field you're editing yaml, regenerating, and then fitting your handler code to whatever the
generator decided to name things, which is a bit of what happened here too (the "id" field is
typed as a string in the schema but the actual DB uses a Long primary key, so the resource ends
up parsing strings back into longs).

Code-first (Product/Store) is faster to iterate on early - you just write the endpoint and move
on - but the spec, if you even bother producing one, is now something you maintain by hand and it
will quietly go out of date the first time someone's in a rush.

My honest choice: contract-first for anything that's a real external-facing API other teams or
clients depend on, especially once it's got some business rules to enforce (which is exactly why
it makes sense for Warehouse - it has the replace/archive semantics that are worth nailing down
in a spec other people can read). For internal CRUD-ish endpoints like Product and Store that
mostly exist to back the app's own UI, I'd lean code-first with something like SpringDoc/OpenAPI
annotations to still generate a spec afterwards for documentation's sake, without paying the
generator overhead on something so simple. I wouldn't apply the same rule dogmatically to every
endpoint in a codebase - it should match how much that endpoint's contract actually matters to
people outside the team.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I'd start from where the actual risk is, not from "let's hit some coverage percentage." In this
project the risk is concentrated in the Warehouse use cases - business unit code uniqueness,
location validation, the capacity math, and the replace/archive rules where a mistake means
either silently losing stock history or letting a location go over budget. That's where I put
most of my effort here: fast, dependency-free unit tests on `CreateWarehouseUseCase`,
`ReplaceWarehouseUseCase` and `ArchiveWarehouseUseCase` with the store and location resolver
mocked out, so each rule can be tested in isolation and runs in milliseconds. Those are the tests
I'd want blocking a PR merge.

Above that, I'd want a thinner layer of integration tests that go through the real REST endpoint
and an actual (or Testcontainers-managed) Postgres, like the existing `ProductEndpointTest` and
`WarehouseEndpointIT` - mainly to catch wiring problems: wrong status codes, JSON mapping issues,
the OpenAPI-generated bean not lining up with what the resource returns, that sort of thing. I
wouldn't try to re-test every business rule at this layer since the unit tests already cover the
logic - I'd just pick one or two representative cases per endpoint (happy path + one clear
rejection) to prove the wiring holds together.

I'd deliberately spend less time on Store and Product, since they're plain CRUD with no rules to
break - a basic create/read/update/delete pass is enough there, and over-testing them just adds
maintenance weight with no real payoff.

To keep this useful over time rather than becoming a checkbox: I'd wire the unit + integration
tests into CI so they run on every push (would fit naturally alongside the Jenkins pipelines I
already maintain), treat a new production bug as a signal that a test was missing and add it
before closing the ticket, and periodically prune tests that just mirror the implementation
detail-for-detail instead of asserting an actual business outcome - those tend to rot into noise
and get skipped by whoever's in a hurry six months from now.
```
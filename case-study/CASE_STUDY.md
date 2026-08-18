# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

The main difficulty here is that most of these costs don't naturally belong to a single
Warehouse or Store. Labor is closer to that (a shift belongs to a physical location), but
inventory holding cost, transportation, and overhead almost always get shared across several
units and need a rule to be split fairly. If that allocation rule is fuzzy or changes without
everyone agreeing, two people can look at the same numbers and get a different picture of which
unit is actually profitable - which is a real problem once cost data starts feeding into
decisions like "should we replace this warehouse" or "should we shut this store down."

From the systems I've worked on, this is usually where a "simple to build, hard to trust" system
shows up: it's easy to insert a cost record with an amount and a warehouse id, it's much harder
to guarantee that record reflects reality (was the transportation cost split by weight, by
distance, or evenly across the shipment? did someone book a cost against the wrong business unit
code because two warehouses share a similar name?). Timing is the other recurring issue - a
transportation invoice can arrive weeks after the delivery happened, so the system needs a clear
answer for what happens to a month's numbers that already got reported before the correction
came in.

Given the domain here specifically, the Warehouse replace/archive flow makes this sharper: if a
warehouse gets archived and a new one takes over its business unit code, whatever cost tracking
sits on top needs to keep attributing historical costs to the archived warehouse and only new
costs to the live one - otherwise the whole point of preserving that code (a continuous cost
history for the business unit) breaks the moment someone builds a report on top of it.

Questions I'd want answered before scoping this:
- Is the allocation meant to be actual cost (what really happened) or standard/budgeted cost
  (a normalized number used for planning)? Those need different data and different tolerance for
  approximation.
- Who owns the allocation rules - finance or engineering - and how often do they change?
- What's the correction process when a cost lands after the reporting period already closed?
- Is a warehouse ever shared across stores in a way that a single cost needs splitting by volume,
  or is the allocation always 1:1 with a business unit code?

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

Looking at just what's modeled in this system already, a few obvious levers stand out:

- Warehouses have a hard capacity and each location has a max number of warehouses and a max
  combined capacity - that's basically a utilization problem. A warehouse sitting at 20% of its
  capacity while another one at the same location is near its ceiling is wasted overhead, so
  utilization rate per warehouse (stock vs. capacity) is probably the single most direct metric
  to optimize against.
- Store-to-warehouse fulfillment (the bonus part of this assignment - each store limited to 3
  warehouses, each product to 2 warehouses per store) is really a routing/distance problem in
  disguise. Fewer, better-chosen warehouse-store pairings usually mean less transportation cost,
  so this is worth analyzing even before touching labor costs.
- The warehouse "replace" flow itself is a cost decision - you're intentionally taking on the
  cost of standing up a new warehouse instead of just letting the old one keep running, so
  whatever triggers a replace decision should be backed by a clear cost comparison, not just
  "the lease expired."

For prioritization, I'd lean on impact vs. effort: start with whatever change needs no new
infrastructure, just better use of what already exists (rebalancing stock across underused
warehouses, tightening which warehouse-store pairs get used), before touching anything that
involves physically opening/closing/moving a warehouse, since that's slow and expensive to
reverse if you get it wrong.

To actually implement it I'd want a short pilot on a small subset of warehouses/stores first,
with a clear before/after cost comparison, rather than rolling a new allocation strategy out
company-wide on a hunch. And "without compromising service quality" needs a concrete guardrail
up front - something like stock-out rate or delivery time - so that a change which lowers cost
but starts causing stock-outs gets caught immediately rather than three months later in a
different report.

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

Without this integration, cost control ends up running on numbers that are always a step behind
what finance considers the "real" ledger, which means any decision made off the fulfillment
side's own reports is on borrowed trust until someone reconciles it against the books, usually at
month-end, usually manually. Tying the two together properly means fulfillment decisions
(opening a warehouse, replacing one, rebalancing stock) get made against numbers finance would
actually recognize, and finance stops having to chase the warehouse team for explanations every
close.

On "real-time" specifically, I'd push back a little in the actual conversation with
stakeholders before committing to it - true real-time sync with a financial system is a lot more
expensive to build and operate correctly than near-real-time (an event fired the moment a cost
record is created, consumed and posted within seconds to minutes). Given the domain I mostly
work in day to day, I'd reach for an event-driven approach here - something like Kafka carrying
"warehouse cost recorded" or "warehouse archived" events - rather than one system polling the
other, since that keeps the two sides decoupled and lets the financial system's own team build
their consumer independently instead of us dictating their integration pattern.

The parts I'd want to get right before writing any integration code:
- Idempotency: financial postings absolutely cannot be double-counted if an event gets
  redelivered, so every event needs a stable identifier the receiving side can dedupe on.
- Reconciliation, not blind trust: even with sync in place, I'd still want a scheduled job that
  compares totals on both sides and flags drift, because "the pipe is running" isn't the same
  guarantee as "the numbers agree."
- Ownership of the mapping between our domain concepts (business unit code, warehouse, store) and
  whatever cost-center/GL-account structure finance uses - that mapping tends to be where
  ambiguity quietly lives.
- What happens when the financial system is down - do we queue and retry, and for how long, before
  someone gets paged?

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

Without forecasting, warehouse capacity decisions (like the ones this system already models -
max warehouses per location, max capacity) end up reactive: you only realize a location is out
of room when someone tries to create a warehouse and gets rejected by the validation. Budgeting
turns that around - you'd want to see the trend of stock/capacity utilization per location and
flag "AMSTERDAM-001 will hit its capacity ceiling in about two months at current growth" well
before it becomes an operational blocker.

The main thing I'd take into account designing this is that a forecast is only as good as the
historical data feeding it, and this domain already has some structural gaps to be aware of:
once a warehouse is archived, do we still keep its full cost/stock history queryable for trend
analysis, or does archiving effectively bury it? The briefing is explicit that cost history has
to be preserved through a replace, so the forecasting layer needs to actually read across an
archived warehouse and its replacement as one continuous series, not treat them as two unrelated
warehouses just because the underlying row changed.

A few other design considerations:
- Granularity: forecasting at the business-unit-code level (so it survives a replace) rather than
  at the DB row/id level, since the row can change but the business unit code is the thing that
  has continuity.
- Separating "budget" (a target set by finance/ops ahead of time) from "forecast" (a model's best
  guess based on trends) - they answer different questions and get compared against each other,
  not merged into one number.
- Seasonality - fulfillment costs for a retail-adjacent business are rarely flat month to month,
  so whatever model gets used needs to account for that instead of naively extrapolating the last
  few weeks.
- Making the forecast auditable - if it's going to inform real budget decisions, someone needs to
  be able to ask "why does the model think Q3 costs will jump" and get an actual answer, not just
  a black-box number.

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

This is the one scenario that maps almost directly onto the code I just implemented, so it's
easiest to reason about concretely. The whole point of reusing the business unit code across the
replace is that, from a cost-control point of view, the old and new warehouse are the same
"thing" - one continuous operation that happens to change physical premises at some point. If
cost history got wiped or orphaned the moment the old warehouse is archived, you'd lose the
ability to answer basic questions like "is this business unit's cost per unit of stock trending
up or down over the last two years," because the trend line would reset to zero every time a
physical warehouse gets replaced - which, operationally, has nothing to do with whether the
underlying business is getting more or less efficient.

There's also a very immediate budget reason: the replacement validations already enforce that
the new warehouse's capacity accommodates the old one's stock and that the stock itself carries
over unchanged. That's not just a data-integrity rule - it's a cost-control guardrail. If the
system let a replacement warehouse be created with a much larger capacity "just in case," or with
mismatched stock, you'd effectively be letting someone quietly upgrade to a more expensive
warehouse under the disguise of a routine replace, without whatever approved the original budget
ever having a chance to weigh in.

Practically, I'd want:
- The archived warehouse's row (and its cost records) to stay queryable indefinitely under its
  original id, purely for audit/history - archiving should mean "no longer active," not
  "disappeared."
- Any cost report or dashboard built on top of this to group by business unit code first and
  treat archived-vs-active as a filter, not as two separate identities, so a chart showing
  "MWH.001 monthly cost" keeps working straight through a replace.
- A clear answer for what happens to costs that were booked against the old warehouse but arrive
  (invoice-wise) after the replace already happened - they should still land on the archived
  warehouse's history, not accidentally get attributed to the new one just because that's now the
  active row for that business unit code.

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.

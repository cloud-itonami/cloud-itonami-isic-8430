# cloud-itonami-isic-8430

Open Occupation Blueprint for **ISIC Rev.5 8430**: Compulsory Social Security Administration.

This repository designs a forkable OSS business for a social security agency's administrative operations: a document-handling and verification robot performs benefits application intake, eligibility checklist completion, payment record logging, and appeal intake under a governor-gated actor, so a social security office keeps its own benefits records and audit trail instead of renting a closed casework SaaS.

## IMPORTANT: SCOPE BOUNDARIES

**This actor is EXPLICITLY NOT a benefits determination authority.**

### What this actor DOES

- Administrative and intake operations only:
  - Claimant registration and verification
  - Benefits application intake and document collection
  - Eligibility checklist completion (documentation only, not determination)
  - Payment record logging and tracking
  - Appeal and grievance intake
  - Audit trail and compliance documentation

### What this actor DOES NOT (hard boundaries, permanently out of scope)

These operations are **permanently forbidden** — they are not gated by risk level or approval hierarchy, they cannot be escalated for human override, and the actor's proposal vocabulary has no path to construct them. A closed allowlist enforces this at the governance layer:

- **Claim approval or denial** — the actor cannot approve, deny, or determine eligibility for any benefits claim. This determination is exclusively a human (typically a benefits officer) decision, never delegated to this advisor.
- **Payment disbursement or modification** — the actor cannot initiate, authorize, or modify benefit payments. Payment decisions are exclusively human.
- **Benefit amount calculation** — the actor cannot determine, calculate, or propose benefit amounts. The calculation methodology and final amounts are exclusively human decisions.
- **Appeal determinations** — the actor can intake appeals and escalate them to human review, but cannot make appeal decisions.

These are not "high-risk operations requiring escalation" — they are entirely outside the actor's design vocabulary. The governor will **permanently :hold** any proposal that touches these categories (it is not a matter of confidence, approval chain, or budget threshold).

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs the physical domain work**. Here a document-handling and verification robot performs benefits application intake, eligibility checklist completion, payment record logging, and appeal intake under an actor that proposes actions and an independent **Social Security Governor** that gates them. The governor never dispatches a robot action itself; escalated actions (such as appeal intake or low-confidence operations) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
claim request + claimant identity + eligibility data
        |
        v
Social Security Advisor -> Social Security Governor -> application intake, checklist, payment log, or human approval
        |
        v
robot actions (gated) + claim record + payment record + audit ledger
```

No automated advice can dispatch a claim action the governor refuses, process an application for an unregistered claimant, or publish a record without governor approval and audit evidence. No proposal can touch claim approval, benefit determination, or payment disbursement.

## Capability layer

Resolves via [`kotoba-lang/industry`](https://github.com/kotoba-lang/industry)
(ISIC `8430`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation (`:maturity :implemented`)

Full itonami Actor pattern (per ADR-2607011000 / CLAUDE.md's Actors
section): a real [`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph)
`StateGraph`, with the Advisor and Governor as distinct graph nodes and
human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                           +-> :request-approval   (:escalate? true, interrupt-before)
                                           +-> :hold               (:hard? true)
```

- `src/social_security/store.cljc` — `Store` protocol + `MemStore`:
  registered claimants, committed claim records, an append-only audit ledger.
- `src/social_security/advisor.cljc` — `Advisor` protocol; `mock-advisor`
  (deterministic, default) proposes a claim administration operation from a
  request; `llm-advisor` wraps a `langchain.model/ChatModel` — either
  way the advisor only ever produces a `:propose`-effect proposal,
  never a claim determination, and LLM parse failures always yield
  `confidence 0.0` (forces escalation, never fabricated confidence).
- `src/social_security/governor.cljc` — `SocialSecurityGovernor/check`: a pure
  function, wired as its own `:govern` node. Hard invariants
  (unregistered claimant, a proposal whose `:effect` isn't `:propose`, any
  proposal touching claim determination or payment) always route to `:hold`.
  Escalation invariants (appeal intake or low advisor confidence) always
  route to `:request-approval` — an `interrupt-before` node that the graph
  checkpoints and only resumes on explicit human approval (`actor/approve!`).
- `src/social_security/actor.cljc` — `build-graph`, `run-request!`,
  `approve!`: the `langgraph.graph/state-graph` wiring itself.

```bash
clojure -M:test
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry).

## License

AGPL-3.0-or-later.

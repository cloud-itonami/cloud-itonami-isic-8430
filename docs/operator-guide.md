# Operator Guide

## Running the System

### Local Development

```bash
clojure -M:test
```

This runs the test suite against the in-memory store and mock advisor. It validates:
- Governor rule enforcement (hard holds and escalations)
- Store append-only semantics
- Graph flow (intake → advise → govern → decide → commit/hold)
- Human-in-the-loop interrupts

### Production Deployment

In production, the `MemStore` is replaced with a persistent store (e.g., Datomic, kotoba-server), and the `mock-advisor` is replaced with an `llm-advisor` backed by a real language model.

```clojure
(let [persistent-store (datomic-store)
      chat-model (init-llm)
      advisor (advisor/llm-advisor chat-model model-generate gen-opts)
      graph (actor/build-graph {:store persistent-store :advisor advisor})]
  ; Run requests
  (actor/run-request! graph request context thread-id)
  ; Resume escalated requests
  (actor/approve! graph thread-id))
```

## Safety Guarantees

### Hard Invariants (Always Held, Never Escalated)

- **No unregistered claimants**: proposal is rejected if claimant is not in the registry
- **No direct store writes**: proposal must have `:effect :propose`; no proposal can directly commit to store or dispatch an action
- **No determination or payment decisions**: proposal that touches claim approval/denial, benefit amount, or payment disbursement is ALWAYS held

If any of these fail, the governor routes to `:hold` with no path to override.

### Escalation Invariants (Require Human Sign-Off)

- **Appeal intake**: all appeals must be reviewed by a human staff member
- **Low confidence**: any proposal with confidence < 0.6 requires human review

If either of these is true, the governor routes to `:request-approval`, which checkpoints the graph and waits for human approval via `actor/approve!`.

## Audit Trail

Every proposal, verdict, and decision is logged to the append-only ledger:

```clojure
{:node :advise :request request :proposal proposal}
{:node :govern :verdict verdict}
{:node :commit :record record}
; or
{:node :hold :verdict verdict}
```

Export the ledger regularly (daily or weekly) for compliance review, appeals audits, or regulatory reporting.

## Claimant Registry

Claimants must be registered before any operation can proceed. Registration captures:
- `:claimant-id` — unique identifier
- `:name` — full name
- `:verified-at` — ISO timestamp of identity verification

```clojure
(store/register-claimant! store
  {:claimant-id "clm-2026-0042"
   :name "Alice Smith"
   :verified-at "2026-01-15"})
```

## Operations

### Intake Benefits Application

```clojure
{:claimant-id "clm-2026-0042"
 :op :intake-benefits-application}
```

Gathers application and supporting documents. Escalates if confidence is low or checklist is incomplete.

### Verify Eligibility Checklist

```clojure
{:claimant-id "clm-2026-0042"
 :op :verify-eligibility-checklist}
```

Walks through an externally-defined checklist (e.g., "proof of residence", "income documentation"). Records which items are verified. Does NOT determine eligibility; that is the benefits officer's role.

### Log Payment Record

```clojure
{:claimant-id "clm-2026-0042"
 :op :log-payment-record}
```

Records a payment decision made by a human staff member. Committed immediately (no escalation).

### Intake Appeal

```clojure
{:claimant-id "clm-2026-0042"
 :op :intake-appeal}
```

Receives and organizes an appeal. ALWAYS escalates to human review (not decided by the advisor).

## Escalation Workflow

When a proposal escalates to `:request-approval`:

1. The graph checkpoints its state
2. The operator's console displays the proposal and context for human review
3. A benefits officer or supervisor reviews the proposal
4. The officer approves (or denies) by calling `actor/approve!` (approval resumes the graph to `:commit`)
5. The final decision is logged to the audit trail

If the officer denies the proposal, the system returns to `:hold` with an explanation. The claimant can appeal or contact the agency for clarification.

## Monitoring

Monitor the audit ledger for:
- High escalation rate (may indicate model misconfiguration or unusual claimant populations)
- Holds (should be rare; may indicate policy violations or parse errors)
- Appeal rate (normal; provides feedback on operator confidence)

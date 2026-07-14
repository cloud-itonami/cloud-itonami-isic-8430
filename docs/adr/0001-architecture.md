# ADR-0001: Core Architecture and Scope Boundaries

## Status

ACCEPTED

## Context

ISIC Rev.5 8430 (Compulsory social security activities) is one of the open occupation blueprints in the cloud-itonami actor ecosystem. The domain is highly sensitive — benefits delivery and social protection are matters of real livelihood. Any automation in this space must be extremely disciplined about **what it does NOT do**.

## Decision

The `cloud-itonami-isic-8430` actor is designed as a **benefits administrative support system**, not a benefits determination authority.

### Scope (What This Actor Does)

- **Claimant intake and registration**: verify claimant identity, collect intake documentation
- **Application processing (intake phase only)**: receive and organize benefits applications, collect required supporting documents
- **Eligibility checklist completion**: walk through a documented, externally-defined eligibility checklist (e.g., "claimant provided proof of residence"; "claimant meets age requirement" — **statements of fact, never the determination itself**)
- **Payment record logging**: log and track payment records for audit and transparency
- **Appeal intake**: receive and organize appeals and grievances, escalate to human review
- **Audit trail**: maintain complete, append-only record of every proposal, verdict, and action

### Scope Exclusion (What This Actor DOES NOT Do)

**These operations are PERMANENTLY FORBIDDEN.** They are not negotiable, not subject to escalation, not subject to confidence levels. The actor's vocabulary has no path to construct them, and the governor will ALWAYS hold any proposal that touches them:

1. **Claim approval or denial**: The actor cannot approve, deny, or determine eligibility for benefits claims. That is exclusively a human decision (typically a benefits officer or caseworker).
2. **Benefit amount calculation or disbursement**: The actor cannot calculate benefit amounts or authorize payment disbursement. Payment decisions are exclusively human.
3. **Eligibility determination**: The actor cannot make or propose eligibility determinations. It can verify that checklist items have been completed, but the final eligibility decision is human.
4. **Appeal decisions**: The actor can intake appeals, but cannot decide appeals.
5. **Policy decisions**: The actor cannot determine or modify benefits policy, entitlement rules, or verification requirements.

### Design Rationale

This boundary is intentional and reflects the safety discipline required for social security operations:

1. **Human accountability**: Every decision that affects a claimant's entitlement or payment is a human decision, personally accountable to the claimant and to law.
2. **Legal authority**: Only humans (typically legally designated benefits officers) have the authority to approve or deny benefits.
3. **No hidden bias**: Benefits decisions must be transparent and explainable to the claimant; an LLM-based determination would fail that standard.
4. **Auditability**: Appeal and oversight processes require a clear human decision-maker to appeal to or scrutinize.

## Implementation

- **Advisor** (`social-security.advisor`): proposes only intake and administrative operations from a closed allowlist. Cannot construct determination/disbursement operations.
- **Governor** (`social-security.governor`): enforces hard invariants that any determination/disbursement proposal is ALWAYS held, no escalation path.
- **Store** (`social-security.store`): records claimants, operations, and an append-only audit ledger.
- **Actor** (`social-security.actor`): a `langgraph` StateGraph with Advisor → Governor → Decide → Commit/Hold flow. Escalations (appeals, low confidence) require human sign-off.

## Consequences

- The actor is operationally simpler and safer than a system that attempted to determine eligibility.
- Operators must maintain a clear **human approval process** for escalations (appeals, low-confidence intakes).
- The actor's value is in **administrative efficiency** (document collection, checklist walking, record logging), not in automating the benefits decision itself.

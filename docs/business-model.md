# Business Model

## Overview

Social security operations are administratively intensive. Benefits officers spend a significant portion of their time on document collection, checklist verification, and record logging — work that is structured, repetitive, and high-volume. Meanwhile, the actual benefits decisions (eligibility determination, payment amounts) remain deeply human, legally required, and contextual.

This blueprint automates the administrative side (the 70% of caseworker time that is paperwork), while leaving all determination and disbursement decisions to human staff (the 30% that is judgment and accountability).

## Typical Operation

1. **Claimant applies** (in person, online, or by mail)
2. **Intake robot receives application**, verifies claimant identity against registry, collects and organizes supporting documents
3. **Eligibility checklist is walked** (via the robot): "proof of residence on file?", "age documented?", "income documentation received?" — statements of fact, not determinations
4. **Case is escalated to benefits officer** if any checklist item fails or confidence is low
5. **Benefits officer reviews application and checklist**, makes final eligibility and payment decision (human, accountable, appealable)
6. **Payment approved by officer**, then robot logs payment record for audit trail
7. **Full audit ledger (all proposals, verdicts, actions) remains available** for oversight, appeals, or regulatory review

## Governance Model

- **Advisor**: proposes intake and administrative operations (application intake, checklist completion, payment logging, appeal intake)
- **Governor**: enforces hard rules (no unregistered claimants, no direct store writes, no determination/disbursement proposals) and escalation rules (appeals always escalate, low confidence escalates)
- **Human Staff**: approves all escalations and makes all benefits determinations
- **Audit Ledger**: complete record of every proposal, verdict, and final action

## Social Impact

- **Administrative efficiency**: caseworkers spend less time on paperwork, more time on judgment-call cases
- **Claimant experience**: faster application intake, transparent checklist
- **Operational transparency**: append-only audit trail proves every action and decision
- **Regulatory compliance**: complete records for oversight and appeals review

# Governance: Agricultural Technician Actor

## Architecture

This actor is built on the **itonami Actor pattern** (see ADR-2607011000 in the kotoba-lang monorepo):

```
Advisor (proposes) -> Governor (gates) -> StateGraph (decides/commits) -> Audit Ledger (records)
```

## The Governor's Authority

The `TechnicianGovernor` is the sole gatekeeper for all operations. It enforces:

1. **Hard Invariants** (ALWAYS `:hold`, never overridable)
   - The requesting technician must be registered and verified
   - The target farm must be registered and supervised by the technician
   - Proposals must have `:effect :propose` only (no direct actuation)

2. **Escalation Invariants** (ALWAYS human sign-off)
   - `:flag-pest-disease-risk` always escalates
   - Low-confidence proposals (below `governor/confidence-floor`) always escalate

## Checkpointing & Resume

The StateGraph checkpoints before each escalation node (`:request-approval`). A technician or supervisor can:

1. Review the proposal and escalation reason
2. Approve via `actor/approve!` to advance to commit
3. Reject by not resuming (the checkpoint persists for audit)

## Audit Trail

All decisions are appended to an immutable ledger:

```clojure
{:disposition :commit|:hold|:escalate
 :record {...}      ; for commit
 :verdict {...}}    ; for hold/escalate
```

This ensures full traceability: what the advisor proposed, what the governor decided, and why.

## No Backdoor Actuation

The actor never directly writes a site assessment, test result, pest/disease flag, or schedule to the farm's record without passing through the governor. It only proposes and records — a human technician or supervisor remains in control of any risk-critical outcome.

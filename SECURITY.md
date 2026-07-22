# Security & Safety

## Critical Boundaries

This actor **never** directly actuates farm equipment or writes technical records on its own authority. The following are **hard boundaries that cannot be overridden**:

- **No direct record commit** — every proposal passes through `TechnicianGovernor/check` before anything is written to the store
- **No unverified technician** — a request from an unregistered or unverified technician is always held
- **No unregistered farm** — a request against a farm that isn't registered and supervised by the technician is always held

The Governor enforces these as irreversible hard violations. A proposal attempting any of these is rejected and audit-logged, never committed.

## Escalation as a Safety Feature

The actor escalates:
- Any `:flag-pest-disease-risk` proposal (biosecurity/crop-health signal)
- Any proposal with confidence below `governor/confidence-floor` (LLM parse failures, uncertainty)

These escalations require explicit human approval (`actor/approve!`) before the proposed action — a site assessment, test protocol result, risk flag, or visit schedule — is committed.

## Audit Logging

Every proposal, decision, and rejection is recorded in an immutable ledger (`store/append-ledger!`). This ensures:
- Full traceability of all actor recommendations
- Proof of human oversight and approval on escalated (pest/disease risk, low-confidence) actions
- Compliance with agricultural extension and biosecurity reporting norms

## Reporting Security Issues

If you discover a security vulnerability, please email `root@junkawasaki.com` with details. Do not open a public issue.

## Supply Chain

Dependencies are pinned to specific git SHAs in `deps.edn`. Before using, verify:
- `io.github.kotoba-lang/langgraph` SHA matches the expected commit
- No unexpected transitive dependencies are introduced

## Compliance

This actor is designed to support human-in-the-loop agricultural extension workflows. It is not a replacement for:
- Licensed agronomists or plant pathologists
- Professional soil/crop testing laboratories
- Regulatory biosecurity or pesticide-use compliance systems
- Emergency pest/disease outbreak response procedures

# cloud-itonami-isco-3142

Open Occupation Blueprint for **ISCO-08 3142**: Agricultural Technicians.

This repository designs a forkable OSS agricultural technician support system: a technician proposes technical field actions (field data collection, test protocol execution, pest/disease risk flagging, site visit scheduling) for a registered farm, under a governor-gated actor that ensures all proposals remain proposals (never direct actuation) and escalates risks.

## Technician Premise

All cloud-itonami verticals are designed on the premise that a **human expert or robot performs the domain work**. Here an agricultural technician proposes technical actions (site assessments, crop/soil testing, risk flags, visit schedules) for a registered farm, under an actor that proposes actions and an independent **Technician Governor** that gates them. The governor never executes actions directly; risk-critical actions (such as pest/disease risk escalations) require human sign-off from the technician/supervisor.

## Core Contract

```text
technician registration + farm registration + technical history
        |
        v
Technician Advisor -> Technician Governor -> site assessment, test results, visit schedule, or human sign-off
        |
        v
technician action + technical records + audit ledger
```

No automated action can suppress a record or disclose sensitive data without governor approval and audit evidence.

## Capability Layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation) (ISCO-08 `3142`). Required capabilities:

- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

## Reference Implementation (`:maturity :implemented`)

Full itonami Actor pattern (per ADR-2607011000 / CLAUDE.md's Actors section): a real [`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph) `StateGraph`, with the Advisor and Governor as distinct graph nodes and human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                          +-> :request-approval   (:escalate? true, interrupt-before)
                                          +-> :hold               (:hard? true)
```

- `src/agri_technician/store.kotoba` — `Store` protocol + `MemStore`: registered technicians, farms, technical records, an append-only audit ledger.
- `src/agri_technician/advisor.kotoba` — `Advisor` protocol; `mock-advisor` (deterministic, default) proposes a technical action from a request; `llm-advisor` wraps a `langchain.model/ChatModel` — either way the advisor only ever produces a `:propose`-effect proposal, never a committed record, and LLM parse failures always yield `{:op :unknown :confidence 0.0}` (never fabricated confidence), which the governor holds as an op outside the catalog.
- `src/agri_technician/operations.kotoba` — the closed vocabulary of ops: `:log-site-assessment`, `:run-test-protocol`, `:flag-pest-disease-risk`, `:schedule-site-visit`. An op outside it is refused.
- `src/agri_technician/facts.kotoba` — the soil test methods a `:run-test-protocol` proposal may name (`:method`): five FAO GLOSOLAN SOPs (`GLOSOLAN-SOP-01`, `-06`, `-10`, `-11`, `-14`), each with the openknowledge.fao.org URL it was fetched from on 2026-09-24.
- `src/agri_technician/governor.kotoba` — `TechnicianGovernor/check`: a pure function, wired as its own `:govern` node. Hard invariants (unregistered technician, unregistered farm, a proposal whose `:effect` isn't `:propose`, an `:op` outside `agri-technician.operations`) always route to `:hold`. Escalation invariants (`:flag-pest-disease-risk`, low advisor confidence, or a `:run-test-protocol` whose `:method` is not in `agri-technician.facts` — reported as `:unrecognized-method` under `:escalations`) always route to `:request-approval` — an `interrupt-before` node that the graph checkpoints and only resumes on explicit human approval (`actor/approve!`), matching the premise that pest/disease risks always require human sign-off.
- `src/agri_technician/actor.kotoba` — `build-graph`, `run-request!`, `approve!`: the `langgraph.graph/state-graph` wiring itself.

```bash
kbb --backend sci test/run_suite.cljk
```

The suite is **24 tests / 97 assertions**. `test/run_suite.cljk` reads that
sentence and refuses (exit 2) any run that comes in under it. `kbb -M:test`
does not run this suite: the sources are `.kotoba`, which the test runner does
not collect.

Before `agri-technician.operations` (2026-09-24) the governor accepted any op
it had not heard of: `{:op :apply-pesticide :effect :propose :confidence 0.9}`
for a verified technician's supervised farm was `:ok? true` and committed a
technical record with no human sign-off. `hard-on-op-outside-the-catalog` and
`end-to-end-hold-on-op-outside-the-catalog` pin the refusal.

Before `agri-technician.facts` (2026-09-24) a `:run-test-protocol` proposal
needed no method: `{:op :run-test-protocol :effect :propose :confidence 0.9}`
committed a test-protocol record that did not say how the soil was tested.
`escalates-test-protocol-without-a-recognized-method` and
`interrupts-test-protocol-that-names-no-method` pin the escalation; a human can
still sign off on a method the catalog does not yet list.

This is what backs this repo's `:maturity :implemented` entry in [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation).

## License

AGPL-3.0-or-later.

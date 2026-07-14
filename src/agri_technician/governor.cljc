(ns agri-technician.governor
  "TechnicianGovernor — the independent safety/traceability layer for
  the ISCO-08 3142 agricultural technician actor. Wired as its own `:govern`
  node in `agri-technician.actor`'s StateGraph, downstream of `:advise` — the
  Advisor has no notion of technician/farm provenance or risk, so this MUST
  be a separate system able to reject a proposal (itonami actor pattern, per
  ADR-2607011000 / CLAUDE.md Actors section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. technician provenance   — the request's technician must be registered
                                and verified.
    2. farm provenance         — the request's farm must be registered
                                and supervised by the technician.
    3. no-actuation            — proposal :effect must be :propose.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off, per
  the premise that pest/disease risks always require human sign-off):
    4. :op :flag-pest-disease-risk (always escalates).
    5. low confidence (< `confidence-floor`)."
  (:require [agri-technician.store :as store]))

(def confidence-floor 0.6)
(def ^:private escalating-ops #{:flag-pest-disease-risk})

(defn- hard-violations [{:keys [proposal]} technician-record farm-record]
  (cond-> []
    (or (nil? technician-record) (not (:verified? technician-record)))
    (conj {:rule :no-technician :detail "unregistered or unverified technician"})

    (or (nil? farm-record) (not (:registered? farm-record)))
    (conj {:rule :no-farm :detail "unregistered farm or farm not supervised by technician"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `agri-technician.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [technician-record (store/technician-by-id store (:technician-id request))
        farm-record (store/farm-by-id store (:farm-id request))
        hard (hard-violations {:proposal proposal} technician-record farm-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (contains? escalating-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))

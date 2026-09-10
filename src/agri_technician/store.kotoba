(ns agri-technician.store
  "SSoT for the ISCO-08 3142 agricultural technician actor.
  Store is a protocol injected into the `agri-technician.actor` StateGraph — `MemStore`
  is the default, deterministic, zero-dep backend; a Datomic/kotoba-server-backed
  implementation can be swapped in without touching the actor or governor (itonami
  actor pattern, per ADR-2607011000 / CLAUDE.md Actors section).

  Domain:

    technician — a registered agricultural technician performing field work
                 (:technician-id, :name, :verified?)
    farm       — a registered farm/site under technician supervision
                 (:farm-id, :technician-id, :location, :registered?)
    record     — a committed technical record under a farm (test result,
                 assessment data, site visit log, risk flag) — written ONLY via
                 commit-record!, never mutated in place
    ledger     — an append-only audit trail of every proposal/verdict/
                 disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (technician-by-id [s technician-id])
  (farm-by-id [s farm-id])
  (records-of [s farm-id])
  (ledger [s])
  (register-technician! [s technician])
  (register-farm! [s farm])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (technician-by-id [_ technician-id] (get-in @a [:technicians technician-id]))
  (farm-by-id [_ farm-id] (get-in @a [:farms farm-id]))
  (records-of [_ farm-id] (filter #(= farm-id (:farm-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-technician! [s technician]
    (swap! a assoc-in [:technicians (:technician-id technician)] technician) s)
  (register-farm! [s farm]
    (swap! a assoc-in [:farms (:farm-id farm)] farm) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:technicians {} :farms {} :records [] :ledger []} seed)))))

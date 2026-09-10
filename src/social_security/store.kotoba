(ns social-security.store
  "SSoT for the ISIC Rev.5 8430 compulsory social security administration
  actor. Store is a protocol injected into the `social-security.actor`
  StateGraph — `MemStore` is the default, deterministic, zero-dep backend;
  a Datomic/kotoba-server-backed implementation can be swapped in without
  touching the actor or governor (itonami actor pattern, per ADR-2607011000
  / CLAUDE.md's Actors section).

  Domain:

    claimant  — a registered benefits claimant (:claimant-id, :name, :verified-at)
    record    — a committed benefits administration operating record
                (application intake, eligibility checklist, payment logging,
                appeal intake) — written ONLY via commit-record!, never mutated
                in place
    ledger    — an append-only audit trail of every proposal/verdict/
                disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (claimant [s claimant-id])
  (records-of [s claimant-id])
  (ledger [s])
  (register-claimant! [s claimant])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (claimant [_ claimant-id] (get-in @a [:claimants claimant-id]))
  (records-of [_ claimant-id] (filter #(= claimant-id (:claimant-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-claimant! [s claimant]
    (swap! a assoc-in [:claimants (:claimant-id claimant)] claimant) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:claimants {} :records [] :ledger []} seed)))))

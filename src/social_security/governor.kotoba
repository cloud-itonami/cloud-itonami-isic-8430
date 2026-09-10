(ns social-security.governor
  "SocialSecurityGovernor — the independent safety/traceability layer
  for the ISIC Rev.5 8430 compulsory social security administration actor.
  Wired as its own `:govern` node in `social-security.actor`'s StateGraph,
  downstream of `:advise` — the Advisor has no notion of claimant provenance
  or benefit eligibility, so this MUST be a separate system able to reject a
  proposal (itonami actor pattern, per ADR-2607011000 / CLAUDE.md Actors
  section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. claimant-provenance      — the request's claimant must be registered.
    2. no-actuation            — proposal :effect must be :propose.
    3. no-determination-or-payment — proposals touching claim approval/denial,
                                     payment disbursement, or payment
                                     modification are PERMANENTLY FORBIDDEN
                                     (closed allowlist enforced here + in
                                     advisor). This is not negotiable and
                                     not subject to escalation.

  ESCALATION invariants (:escalate? true, ALWAYS human sign-off):
    4. intake-appeal           — all appeal intakes require human review.
    5. low-confidence          (< `confidence-floor`)."
  (:require [social-security.store :as store]))

(def confidence-floor 0.6)

; Permanently forbidden operation categories
; :unknown catches out-of-scope proposals from advisor
; All operations involving determination or payment must stay hard-blocked
(def ^:private forbidden-ops #{:unknown})

; Escalating operations (require human approval)
(def ^:private escalating-ops #{:intake-appeal})

(defn- hard-violations [{:keys [proposal]} claimant-record]
  (cond-> []
    (nil? claimant-record)
    (conj {:rule :no-claimant :detail "claimant not registered"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect must be :propose only (no direct store writes)"})

    (contains? forbidden-ops (:op proposal))
    (conj {:rule :scope-boundary
           :detail "operation outside permitted scope (claim approval/denial, payment disbursement/modification are permanently forbidden)"})))

(defn check
  "Assess a proposal against `request`/`proposal` and a
  `store` implementing `social-security.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request proposal store]
  (let [claimant-record (store/claimant store (:claimant-id request))
        hard (hard-violations {:proposal proposal} claimant-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        escalating-op? (contains? escalating-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not escalating-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? escalating-op?))}))

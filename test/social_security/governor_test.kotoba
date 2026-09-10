(ns social-security.governor-test
  (:require [clojure.test :refer [deftest is]]
            [social-security.store :as store]
            [social-security.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-claimant! st {:claimant-id "claimant-1" :name "Alice Smith" :verified-at "2026-01-01"})
    st))

(deftest ok-on-clean-benefits-intake
  (let [st (fresh-store)
        proposal {:op :intake-benefits-application :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:claimant-id "claimant-1"} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-claimant
  (let [st (fresh-store)
        proposal {:op :intake-benefits-application :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:claimant-id "no-such-claimant"} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-claimant (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :intake-benefits-application :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:claimant-id "claimant-1"} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-scope-boundary-violation
  (let [st (fresh-store)
        proposal {:op :unknown :effect :propose :confidence 0.0 :stake :high}
        v (governor/check {:claimant-id "claimant-1"} proposal st)]
    (is (:hard? v))
    (is (some #(= :scope-boundary (:rule %)) (:violations v)))))

(deftest escalates-on-appeal-intake
  (let [st (fresh-store)
        proposal {:op :intake-appeal :effect :propose :confidence 0.9 :stake :medium}
        v (governor/check {:claimant-id "claimant-1"} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :verify-eligibility-checklist :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:claimant-id "claimant-1"} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:claimant-id "claimant-1" :op :intake-benefits-application})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "claimant-1"))))
    (is (= 1 (count (store/ledger st))))))

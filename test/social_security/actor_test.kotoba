(ns social-security.actor-test
  (:require [clojure.test :refer [deftest is]]
            [social-security.store :as store]
            [social-security.advisor :as advisor]
            [social-security.actor :as actor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-claimant! st {:claimant-id "claimant-1" :name "Alice Smith" :verified-at "2026-01-01"})
    st))

(deftest run-clean-benefits-application-intake
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        result (actor/run-request! graph {:claimant-id "claimant-1" :op :intake-benefits-application} {} "thread-1")]
    (is (= :done (:status result)))
    (is (> (count (:state result)) 0))))

(deftest run-appeal-intake-escalates
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        result (actor/run-request! graph {:claimant-id "claimant-1" :op :intake-appeal} {} "thread-2")]
    (is (= :interrupted (:status result)))
    (is (contains? (:state result) :record))))

(deftest graph-refuses-unknown-operation
  (let [st (fresh-store)
        graph (actor/build-graph {:store st :advisor (advisor/mock-advisor)})
        result (actor/run-request! graph {:claimant-id "claimant-1" :op :unknown} {} "thread-3")]
    (is (= :done (:status result)))))

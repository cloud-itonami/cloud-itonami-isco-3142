(ns agri-technician.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [agri-technician.store :as store]
            [agri-technician.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-technician! st {:technician-id "tech-1" :name "Alice Technician" :verified? true})
    (store/register-farm! st {:farm-id "farm-1" :technician-id "tech-1" :location "North Field" :registered? true})
    st))

(deftest ok-on-clean-site-assessment
  (let [st (fresh-store)
        proposal {:op :log-site-assessment :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:technician-id "tech-1" :farm-id "farm-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-technician
  (let [st (fresh-store)
        proposal {:op :log-site-assessment :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:technician-id "no-such-tech" :farm-id "farm-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-technician (:rule %)) (:violations v)))))

(deftest hard-on-unverified-technician
  (let [st (store/mem-store)
        _ (store/register-technician! st {:technician-id "tech-2" :name "Unverified Tech" :verified? false})
        _ (store/register-farm! st {:farm-id "farm-2" :technician-id "tech-2" :location "Field" :registered? true})
        proposal {:op :log-site-assessment :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:technician-id "tech-2" :farm-id "farm-2"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-technician (:rule %)) (:violations v)))))

(deftest hard-on-unregistered-farm
  (let [st (fresh-store)
        proposal {:op :log-site-assessment :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:technician-id "tech-1" :farm-id "no-such-farm"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-farm (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :log-site-assessment :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:technician-id "tech-1" :farm-id "farm-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-on-pest-disease-flag
  (let [st (fresh-store)
        proposal {:op :flag-pest-disease-risk :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:technician-id "tech-1" :farm-id "farm-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest ok-on-test-protocol-execution
  (let [st (fresh-store)
        proposal {:op :run-test-protocol :effect :propose :confidence 0.9 :stake :medium}
        v (governor/check {:technician-id "tech-1" :farm-id "farm-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest ok-on-site-visit-scheduling
  (let [st (fresh-store)
        proposal {:op :schedule-site-visit :effect :propose :confidence 0.95 :stake :low}
        v (governor/check {:technician-id "tech-1" :farm-id "farm-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :run-test-protocol :effect :propose :confidence 0.2 :stake :medium}
        v (governor/check {:technician-id "tech-1" :farm-id "farm-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:farm-id "farm-1" :op :log-site-assessment})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "farm-1"))))
    (is (= 1 (count (store/ledger st))))))

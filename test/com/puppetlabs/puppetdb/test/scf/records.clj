(ns com.puppetlabs.puppetdb.test.scf.records
  (:use [clj-time.coerce :only [to-timestamp]]
        [clj-time.core :only [now]]
        [clojure.test]
        [com.puppetlabs.jdbc :only [query-to-vec]]
        [com.puppetlabs.puppetdb.fixtures]
        [com.puppetlabs.puppetdb.scf.storage :only [add-certname!]])
  (:require [com.puppetlabs.puppetdb.scf.records :refer :all])
  (:import [com.puppetlabs.puppetdb.scf.records Fact]))

(use-fixtures :each with-test-db)

(deftest test-facts
  (let [time (to-timestamp (now))
        certname "some_certname.local"]
    (add-certname! certname)
    (testing "it has the relevant queryable keys"
      (let [fact (Fact. "kernel" "Linux" time certname)]
        (are [field result] (= result (get-in fact [field]))
             :name      "kernel"
             :value     "Linux"
             :timestamp time
             :certname  certname)))
    (testing "protocols"
      (testing "can successfully save a fact to the database"
        (let [fact (Fact. "kernel" "Linux" time certname)]
          (save! fact)
          (is (= (query-to-vec (str "SELECT f.name, f.value, m.certname, m.timestamp "
                                    "FROM facts AS f "
                                    "INNER JOIN facts_metadata AS m "
                                    "ON f.fact_id = m.fact_id"))
                 [{:name      "kernel"
                   :value     "Linux"
                   :timestamp time
                   :certname  certname}]))))
      (testing "will re-use facts that are already in the database"
        (let [name       "architecture"
              value      "i386"
              cert-alpha "alpha.certname"
              cert-beta  "beta.certname"
              fact-alpha (Fact. name value time cert-alpha)
              fact-beta  (Fact. name value time cert-beta)]
          (add-certname! cert-alpha)
          (add-certname! cert-beta)
          (save! fact-alpha)
          (save! fact-beta)
          (let [fact-ids (query-to-vec [(str "SELECT f.fact_id "
                                             "FROM facts AS f "
                                             "INNER JOIN facts_metadata AS m "
                                             "ON f.fact_id = m.fact_id "
                                             "WHERE f.name = ? "
                                             "AND f.value = ?")
                                        name value])]
            (is (= 1
                   (->> fact-ids
                        (map :fact_id)
                        set
                        count)))))))))

(ns com.puppetlabs.puppetdb.test.scf.records
  (:use [clojure.test])
  (:require [com.puppetlabs.puppetdb.scf.records])
  (:import [com.puppetlabs.puppetdb.scf.records Fact]))

(deftest test-facts
  (testing "it has the relevant queryable keys"
    (let [fact (Fact. "kernel" "Linux" "some timestamp" "host.local")]
      (are [field result] (= result (get-in fact [field]))
           :name      "kernel"
           :value     "Linux"
           :timestamp "some timestamp"
           :certname  "host.local"))))

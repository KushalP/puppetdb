(ns com.puppetlabs.puppetdb.scf.records
  (:use [com.puppetlabs.jdbc :only [query-to-vec]])
  (:require [clojure.java.jdbc :as sql]))

(defprotocol Commandable
  (save! [this]))

(defrecord Fact [name value timestamp certname]
  Commandable
  (save! [this]
    (let [existing-fact-id (query-to-vec [(str "SELECT fact_id "
                                               "FROM facts "
                                               "WHERE name = ? "
                                               "AND value = ?")
                                          (:name this)
                                          (:value this)])
          add-fact (fn [fact-map]
                     (->> fact-map
                          (sql/insert-record :facts)
                          (:fact_id)))
          add-fact-metadata (fn [fact-id]
                              (->> (select-keys this [:timestamp :certname])
                                   (merge {:fact_id fact-id})
                                   (sql/insert-record :facts_metadata)))]
      (if (seq existing-fact-id)
        (add-fact-metadata (:fact_id (first existing-fact-id)))
        (add-fact-metadata (add-fact (select-keys this [:name :value])))))))

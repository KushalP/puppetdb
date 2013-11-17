(ns com.puppetlabs.puppetdb.scf.records
  (:require [clojure.java.jdbc :as sql]))

(defprotocol Commandable
  (save! [this]))

(defrecord Fact [name value timestamp certname]
  Commandable
  (save! [this]
    (let [fact-id-map (sql/insert-record :facts (select-keys this [:name :value]))]
      (->> (select-keys this [:timestamp :certname])
           (merge fact-id-map)
           (sql/insert-record :facts_metadata)))))

(ns com.puppetlabs.puppetdb.test.schema
  (:require [clojure.test :refer :all]
            [com.puppetlabs.puppetdb.schema :refer :all]
            [schema.core :as s]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]
            [com.puppetlabs.time :as pl-time]))

(deftest defaulted-maybe-test
  (let [defaulted-schema {:foo (defaulted-maybe s/Int 10)}]
    (is (= {:foo 10}
           (s/validate defaulted-schema
                       {:foo 10})))
    (is (= {:foo nil}
           (s/validate defaulted-schema
                       {:foo nil})))

    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Value does not match schema: \{:foo missing-required-key\}"
                          (s/validate {:foo (defaulted-maybe s/Int 10)}
                                      {})))

    (is (= {:foo 10}
           (->> {:foo nil}
                (s/validate defaulted-schema)
                (defaulted-data defaulted-schema)
                (s/validate defaulted-schema))))))

(deftest defaulted-maybe?-test
  (is (true? (defaulted-maybe? (defaulted-maybe s/Int 10))))
  (is (false? (defaulted-maybe? (s/maybe s/Int)))))

(deftest defaulted-maybe-keys-test
  (is (= [:foo]
         (defaulted-maybe-keys
           {:foo (defaulted-maybe s/Int 1)
            :bar (s/maybe s/Int)
            :baz s/String}))))

(deftest schema-key->data-key-test
  (are [x y] (= x (schema-key->data-key y))
       :foo (s/optional-key :foo)
       :foo :foo
       :foo (s/required-key :foo)))

(deftest strip-unknown-keys-test
  (let [schema {(s/required-key :foo) s/Int
                (s/optional-key :bar) s/Int
                :baz s/String}]
    (is (= {:foo 1
            :bar 3
            :baz 5}
           (strip-unknown-keys
            schema
            {:foo 1
             :foo-1 2
             :bar 3
             :baz 5})))
    (is (= {:foo 1}
           (strip-unknown-keys
            schema
            {:foo 1
             :foo-1 2})))
    (is (= {}
           (strip-unknown-keys
            schema
            {})))
    (is (= {}
           (strip-unknown-keys
            schema
            {:foo-1 "foo"
             :foo-2 "baz"})))))

(deftest defaulted-data-test
  (let [schema {:foo s/Int
                (s/optional-key :foo-1) (defaulted-maybe s/Int 1)
                (s/required-key :bar) s/Int
                (s/optional-key :baz-2) (defaulted-maybe s/String "bar-2")
                (s/optional-key :baz) (defaulted-maybe s/String "baz")}]

    (testing "all defaulted"
      (is (= {:foo-1 1
              :baz-2 "bar-2"
              :baz "baz"}
             (defaulted-data schema {}))))

    (testing "some defaulted"
      (is (= {:foo 20
              :foo-1 10000
              :bar 2
              :baz-2 "bar-2"
              :baz "baz"}
             (defaulted-data schema {:foo 20
                                     :foo-1 10000
                                     :bar 2}))))

    (testing "none defaulted"
      (is (= {:foo 20
              :foo-1 10000
              :bar 2
              :baz-2 "really baz 2"
              :baz "not baz"}
             (defaulted-data schema  {:foo 20
                                      :foo-1 10000
                                      :bar 2
                                      :baz-2 "really baz 2"
                                      :baz "not baz"}))))))

(deftest test-strip-unknown-keys
  (let [schema {:foo s/Int
                (s/optional-key :bar) (defaulted-maybe s/Int 1)
                (s/required-key :baz) s/Int}]
    (testing "strip all keys"
      (is (empty? (strip-unknown-keys schema {:not-foo 1
                                              :not-bar 2
                                              :not-baz 3}))))
    (testing "strip some keys"
      (is (is (= {:foo 1
                  :bar 3
                  :baz 5}
                 (strip-unknown-keys schema {:foo 1
                                             :not-foo 2
                                             :bar 3
                                             :not-bar 4
                                             :baz 5
                                             :not-baz 6})))))
    (testing "strip no keys"
      (is (is (= {:foo 1
                  :bar 3
                  :baz 5}
                 (strip-unknown-keys schema {:foo 1
                                             :bar 3
                                             :baz 5})))))))

(deftest schema-type-construction
  (are [expected target-schema source-schema value]
    (= expected ((get-construct-fn target-schema) source-schema value))

    (time/minutes 10) Minutes s/String "10"
    (time/minutes 10) Minutes s/Int 10

    (time/secs 10) Seconds s/String "10"
    (time/secs 10) Seconds s/Int 10

    (time/days 10) Days s/String "10"
    (time/days 10) Days s/Int 10

    (pl-time/parse-period "10d") Period s/String "10d"

    true SchemaBoolean s/String "true"
    false SchemaBoolean s/String "false"
    true SchemaBoolean s/String "TRUE"
    false SchemaBoolean s/String "FALSE"
    true SchemaBoolean s/String "True"
    false SchemaBoolean s/String "False"
    false SchemaBoolean s/String "really false"))

(deftest schema-conversion
  (testing "conversion of days/minutes/seconds"
    (let [schema {:foo Days
                  :bar Minutes
                  :baz Seconds}

          result {:foo (time/days 10)
                  :bar (time/minutes 20)
                  :baz (time/secs 30)}]
      (is (= result
             (convert-to-schema schema {:foo 10
                                        :bar 20
                                        :baz 30})))
      (is (= result
             (convert-to-schema schema {:foo "10"
                                        :bar "20"
                                        :baz "30"})))))
  (testing "conversion with time periods"
    (let [schema {:foo Period
                  :bar Minutes
                  :baz Period}
          result {:foo (pl-time/parse-period "10d")
                  :bar (time/minutes 20)
                  :baz (pl-time/parse-period "30s")}]
      (is (= result
             (convert-to-schema schema {:foo "10d"
                                        :bar "20"
                                        :baz "30s"})))
      (is (= result
             (convert-to-schema schema {:foo "10d"
                                        :bar 20
                                        :baz "30s"})))))

  (testing "partial conversion"
    (let [schema {:foo s/String
                  :bar s/Int
                  :baz Period}
          result {:foo "foo"
                  :bar 10
                  :baz (pl-time/parse-period "30s")}]
      (is (= result
             (convert-to-schema schema {:foo "foo"
                                        :bar 10
                                        :baz "30s"})))))

  (testing "conversion with an optional and a maybe"
    (let [schema {:foo s/String
                  :bar s/Int
                  (s/optional-key :baz) (s/maybe Period)}
          result {:foo "foo"
                  :bar 10
                  :baz (pl-time/parse-period "30s")}]
      (is (= result
             (convert-to-schema schema {:foo "foo"
                                        :bar 10
                                        :baz "30s"}))))))




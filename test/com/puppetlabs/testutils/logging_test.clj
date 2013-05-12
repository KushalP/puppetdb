(ns com.puppetlabs.testutils.logging-test
  (:use [clojure.test :only (deftest testing is)]
        [com.puppetlabs.testutils.logging]))

(deftest test-logs-matching
  (testing "throws an exception when expected inputs aren't provided"
    (is (thrown? AssertionError (logs-matching nil nil)))
    (is (thrown? AssertionError (logs-matching 1 2)))
    (is (thrown? AssertionError (logs-matching "ab" ["a" "b"]))))
  (testing "extracts the log messages that match the output we provide"
    (is (= '({:namespace :a, :level :b, :exception :c, :message "abc"})
           (logs-matching #"ab" [[:a :b :c "abc"]
                                 [:x :y :z "qe"]])))))

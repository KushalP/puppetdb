(ns com.puppetlabs.testutils.logging-test
  (:use [clojure.test :only (deftest testing is)]
        [com.puppetlabs.testutils.logging]
        [taoensso.timbre :as log]))

(deftest test-logs-matching
  (testing "throws an exception when expected inputs aren't provided"
    (is (thrown? AssertionError (logs-matching nil nil)))
    (is (thrown? AssertionError (logs-matching 1 2)))
    (is (thrown? AssertionError (logs-matching "ab" ["a" "b"]))))
  (testing "extracts the log messages that match the output we provide"
    (is (= '({:namespace :time, :level :info, :message "abc"})
           (logs-matching #"ab" [[:time :info "abc"]
                                 [:time :info "qe"]])))))

(deftest test-atom-logger
  (testing "pipes any log output to the provided atom"
    (let [logs (atom [])]
      (do
        (atom-logger logs)
        (log/info "test"))
      (is (= "test" (-> @logs (first) (get 2)))))))

(deftest test-with-log-output
  (testing "pipes log output to the provided atom"
    (let [logs (atom [])]
      (with-log-output logs
        (log/info "Hello")
        (is (= "Hello" (-> @logs (first) (get 2))))))))

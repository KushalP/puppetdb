(ns com.puppetlabs.testutils.logging
  (:use [taoensso.timbre :as timbre]))

(defn- log-entry->map
  [log-entry]
  {:namespace (get log-entry 0)
   :level     (get log-entry 1)
   :message   (get log-entry 2)})

(defn logs-matching
  "Given a regular expression pattern and a sequence of log messages (in the format
  used by `clojure.tools.logging`, return only the logs whose message matches the
  specified regular expression pattern.  (Intended to be used alongside
  `with-log-output` for tests that are validating log output.)  The result is
  a sequence of maps, each of which contains the following keys:
  `:namespace`, `:level`, `:exception`, and `:message`."
  [pattern logs]
  {:pre  [(instance? java.util.regex.Pattern pattern)
          (coll? logs)]}
  ;; the logs are formatted as sequences, where the string at index 3 contains
  ;; the actual log message.
  (let [matches (filter #(re-find pattern (get % 2)) logs)]
    (map log-entry->map matches)))

(defn atom-logger [output-atom]
  "Send logs to the supplied atom"
  (timbre/set-config!
   [:appenders :puppetdb-test-appender]
   {:doc       "PuppetDB Test Appender"
    :min-level :debug
    :enabled?  true
    :async?    false
    :max-message-per-msecs nil ; No rate limiting
    :fn (fn [{:keys [ap-config level prefix message more] :as args}]
          (when-not (:my-production-mode? ap-config)
            (swap! output-atom conj [prefix level message more])))}))

(defmacro with-log-output
  "Sets up a temporary logger to capture all log output to a sequence, and
  evaluates `body` in this logging context.

  `log-output-var` - Inside of `body`, the variable named `log-output-var`
  is a clojure atom containing the sequence of log messages that have been logged
  so far.  You can access the individual log messages by dereferencing this
  variable (with either `deref` or `@`).

  Example:

      (with-log-output logs
        (log/info \"Hello There\")
        (is (= 1 (num-logs-matching #\"Hello There\" @logs))))
  "
  [log-output-var & body]
  `(let [~log-output-var (atom [])]
     (do (atom-logger ~log-output-var)
         ~@body)))

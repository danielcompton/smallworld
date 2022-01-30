(ns smallworld.memoize
  (:refer-clojure :exclude [memoize]))

(def debug? true)

(defprotocol ICache
  ; TODO: consider additing a #validate method, which I'd use for the db version
  (update! [this request-key value])
  (read!   [this request-key]))

(extend-protocol ICache
  clojure.lang.Atom
  (update! [this request-key value] (swap! this #(assoc % request-key value)))
  (read!   [this request-key]       (get @this request-key ::not-found))

  java.io.File
  (update! [this request-key value]
    (when (.createNewFile this) ;; creates new file & returns true iff it doesn't exist
      (spit this "{}"))
    (spit this (assoc (read-string (slurp this)) request-key value)))
  (read! [this request-key]
    (when (.createNewFile this) ;; creates new file & returns true iff it doesn't exist
      (spit this "{}"))
    (get (read-string (slurp this)) request-key ::not-found))

  java.lang.String
  (update! [this request-key value]
    (println)
    (println)
    (println "---------- update! was called ---------------")
    (println)
    (println "              this: " this)
    (println "       request-key: " request-key)
    (println "             value: " value)
    1)
  (read! [this request-key]
    ; TODO: return :failed if couldn't find the result
    (println)
    (println "---------- read! was called ---------------")
    (println)
    (println "              this: " this)
    (println "       request-key: " request-key)
    2))

(defn my-memoize
  ([expensive-fn cache]
   (if debug? (println "\ninitializing cache: " (str cache)))
   (fn [& [request-key & optional-args :as all-args]]
     (assert (string? request-key) "my-memoize requires the request key to be a string")

     (when debug?
       (println "          all-args: " all-args)
       (println "     optional-args: " (or optional-args "no optional args")))

     (if (= ::not-found (read! cache request-key))
       ;; if we haven't seen the request before, then we need to compute the value
       (let [result (if optional-args
                      (apply expensive-fn all-args)
                      (expensive-fn request-key))]
         (if (= :failed result)
           ;; if the expensive function failed, don't cache the result
           (do (when debug?
                 (println "\n🔴 failed to fetch result for: " request-key)
                 (println))
               :failed)
           ;; else if the expensive function succeeded, cache the result
           (do (when debug?
                 (println "\n🟢 fetch for first time: " request-key " → " result)
                 (println))
               (update! cache request-key result)
               result)))

       ;; if we've seen the request before, then just return the cached value
       (let [result (read! cache request-key)]
         (when debug?
           (println "\n🟡 retrieving stored result: " request-key " → " result)
           (println))
         result)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; scratchpad ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(when debug?
  (defn expensive-fn-1 [key]
    (println "expensive-fn-1 was called with key" key))

  (def memoized-fn-1a (my-memoize expensive-fn-1 "db-url"))
  (memoized-fn-1a "a"))
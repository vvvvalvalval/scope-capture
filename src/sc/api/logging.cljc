(ns sc.api.logging
  (:require [sc.impl.logging :as il]))

(defn register-cs-logger
  [logger-id f]
  (defmethod il/log-cs logger-id
    [_ cs]
    (f cs)))

;; TODO handle :default with friendly message (Val, 02 Oct 2017)

(defn- source-info [cs]
  (str (get cs :sc.cs/file "(Unknown file)") ":" (:sc.cs/line cs)))

(defn log-cs
  [prefix cs]
  (println prefix
    (str "<" (:sc.cs/id cs) ">")
    (source-info cs)
    "\n "
    (str "At Code Site " (:sc.cs/id cs) ", will save scope with locals " (pr-str (:sc.cs/local-names cs)))))

(register-cs-logger
  ::log-spy-cs
  (fn [cs] (log-cs "SPY" cs)))

(register-cs-logger
  ::log-brk-cs
  (fn [cs] (log-cs "BRK" cs)))

(defn- epid-info
  [ep]
  (let [cs (:sc.ep/code-site ep)]
    (pr-str [(:sc.ep/id ep) (:sc.cs/id cs)])))

(defn log-spy-ep-pre-eval
  [ep]
  (let [cs (:sc.ep/code-site ep)]
    (println "SPY"
      (epid-info ep)
      (source-info cs)
      (str
        "\n  "
        "At Execution Point " (:sc.ep/id ep) " of Code Site " (:sc.cs/id cs) ", "
        "saved scope with locals "
        (pr-str (:sc.cs/local-names cs))))))

(defn log-spy-ep-post-eval
  [ep]
  (let [cs (:sc.ep/code-site ep)]
    (println "SPY"
      (epid-info ep)
      (source-info cs)
      (str
        "\n"
        (pr-str (:sc.cs/expr cs))
        (cond
          (:sc.ep/value ep)
          (str "\n=>\n" (pr-str (:sc.ep/value ep)))
          (:sc.ep/error ep)
          (str "\nthrew\n" (pr-str (:sc.ep/error ep)))
          :else
          "")))))



(defn log-brk-ep-pre-eval
  [ep]
  (let [cs (:sc.ep/code-site ep)]
    (println "BRK"
      (epid-info ep)
      (source-info cs)
      (str
        "\n  "
        "saved scope with locals "
        (pr-str (:sc.cs/local-names cs))
        ", use sc.api/loose(-...) to resume execution."))))

(defn log-brk-ep-post-eval
  [ep]
  (let [cs (:sc.ep/code-site ep)]
    (println "BRK"
      (epid-info ep)
      (source-info cs)
      (str
        "\n"
        (pr-str (:sc.cs/expr cs))
        (cond
          (:sc.ep/value ep)
          (str "\n=>\n" (pr-str (:sc.ep/value ep)))
          (:sc.ep/error ep)
          (str "\nthrew\n" (pr-str (:sc.ep/error ep)))
          :else
          "")))))



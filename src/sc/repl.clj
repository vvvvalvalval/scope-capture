(ns sc.repl
  (:require [clojure.main :as main]
            [sc.api]))

(defn ep-repl
  [ep-id]
  (let [ep (sc.api/ep-info ep-id)
        ep-id (:sc.ep/id ep)
        cs-id (-> ep :sc.ep/code-site :sc.cs/id)]
    (main/repl
      :prompt
      (fn sc-prompt []
        (print (str "SC" (pr-str [ep-id cs-id]) "=>")))
      :eval
      (fn sc-eval [expr]
        (eval (list `sc.api/letsc [ep-id cs-id]
                 expr)))
      :read
      (fn sc-read
        [request-prompt request-exit]
        (let [input (main/repl-read request-prompt request-exit)]
          (if (= input :repl/quit)
            request-exit
            input))))))

(ns sc.impl.logging)

(defmulti log-cs
  (fn [logger-id cs-data] logger-id))

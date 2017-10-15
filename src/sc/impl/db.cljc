(ns sc.impl.db)

(defonce cs-id (atom 0))

(defonce ep-id (atom 0))

(defonce db
  (atom {:code-sites {}
         :execution-points {}}))

(defproject vvvvalvalval/scope-capture "0.1.1"
  :description "Easier REPL-based debugging for Clojure by saving and restoring snapshots of the local environment."
  :url "https://github.com/vvvvalvalval/scope-capture"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.395"]]
  :profiles
  {:lab
   {:source-paths ["src" "lab"]
    :dependencies [[org.clojure/clojurescript "1.9.908"]]}})

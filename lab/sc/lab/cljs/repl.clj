(ns sc.lab.cljs.repl
  (:require [cljs.repl :as repl]
            [cljs.repl.rhino :as rhino]))

(def env (rhino/repl-env))

(repl/repl env)

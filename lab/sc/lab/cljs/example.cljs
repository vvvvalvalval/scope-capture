(ns sc.lab.cljs.example
  (:require [sc.api :as sc])
  (:require-macros [sc.api :as scm]))

(def foo
  (let [a 1
        b (inc a)]
    (fn [x y]
      (let [z (+ x (* a y))]
        (* 3
          (scm/spy
            (throw (ex-info "Arrrrrg" {}))))))))

(foo 1 2)



#_'{:protocol-inline nil,
  ;:repl-env #cljs.repl.rhino.RhinoEnv{:cx #object[org.mozilla.javascript.Context 0x51769dcd org.mozilla.javascript.Context @51769dcd],
  ;                                    :scope #object[org.mozilla.javascript.NativeObject 0x37cb7264 [object Object]]},
  :ns {:rename-macros {},
       :renames {},
       :use-macros {},
       :excludes #{},
       :name sc.lab.cljs.example,
       :imports nil,
       :requires {sc sc.api, sc.api sc.api},
       :uses nil,
       :require-macros {scm sc.api, sc.api sc.api}, :doc nil},
  :def-emits-var true,
  :protocol-impl nil,
  :column 9,
  :root-source-info
  {:source-type :fragment,
   :source-form (def foo (let [a 1 b (inc a)] (fn [x y] (let [z (+ x (* a y))] (scm/spy (+ z b))))))},
  :line 6,
  :context :return}

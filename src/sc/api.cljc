(ns sc.api
  (:require [sc.impl :as i]))

(defn spy-emit
  [opts expr amp-env amp-form]
  (i/spy-emit opts expr amp-env amp-form))

(defmacro spy
  ([] (spy-emit nil nil &env &form))
  ([expr] (spy-emit nil expr &env &form))
  ([opts expr] (spy-emit opts expr &env &form)))

(defn brk-emit
  [opts expr amp-env amp-form]
  (i/brk-emit opts expr amp-env amp-form))

(defmacro brk
  ([] (brk-emit nil nil &env &form))
  ([expr] (brk-emit nil expr &env &form))
  ([opts expr] (brk-emit opts expr &env &form)))

(defn ep-info
  [ep-id]
  (i/ep-info ep-id))

(defn cs-info
  [cs-id]
  (i/cs-info cs-id))

(defmacro letsc
  [ep-id & body]
  (let [cs (i/resolve-code-site ep-id)
        ep-id (i/resolve-ep-id ep-id)]
    `(binding
       ~(into []
          (mapcat (fn [dvn]
                    [dvn `(i/ep-var-binding ~ep-id (quote ~dvn))]))
          (:sc.cs/dynamic-var-names cs))
       (let ~(into []
               (mapcat
                 (fn [ln]
                   [ln `(i/ep-binding ~ep-id (quote ~ln))]))
               (:sc.cs/local-names cs))
         ~@body))))

(defmacro defsc
  [ep-id]
  (let [cs (i/resolve-code-site ep-id)
        ep-id (i/resolve-ep-id ep-id)]
    (into []
      (map (fn [ln]
             `(def ~ln (i/ep-binding ~ep-id (quote ~ln)))))
      (:sc.cs/local-names cs))))

(defmacro undefsc
  [ep-or-cs-id]
  (let [cs (cond
             (vector? ep-or-cs-id) (cs-info (second ep-or-cs-id))
             (integer? ep-or-cs-id)
             (cond
               (pos? ep-or-cs-id) (:sc.ep/code-site (ep-info ep-or-cs-id))
               (neg? ep-or-cs-id) (cs-info ep-or-cs-id))
             :else (throw (ex-info "ep-or-cs-id should be a positive integer, a negative integer, or a vector of one positive integer and one negative integer."
                            {:ep-or-cs-id ep-or-cs-id})))
        nz (ns-name *ns*)]
    `(do
       ~@(map
           (fn [ln]
             `(ns-unmap (quote ~nz) (quote ~ln)))
           (:sc.cs/local-names cs))
       nil)))

(defn loose
  [ep-id]
  (i/brk-send-chan ep-id {:sc.brk/type :sc.brk.type/loose}))

(defn loose-with
  [ep-id v]
  (i/brk-send-chan ep-id {:sc.brk/type :sc.brk.type/loose-with
                          :sc.brk/loose-value v}))

(defn loose-with-err
  [ep-id err]
  (i/brk-send-chan ep-id {:sc.brk/type :sc.brk.type/loose-with-err
                          :sc.brk/loose-error err}))

(defn enable!
  [cs-id]
  (i/enable! cs-id))

(defn disable!
  [cs-id]
  (i/disable! cs-id))

(defn dispose!
  [ep-id]
  (i/dispose! ep-id))

(defn dispose-all!
  []
  (i/dispose-all!))







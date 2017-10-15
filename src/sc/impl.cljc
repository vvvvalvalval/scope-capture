(ns sc.impl
  (:require [#?(:clj clojure.core.async :cljs cljs.core.async) :as a]
            [sc.impl.db :as db]
            [sc.impl.logging :as il]
            [sc.api.logging]))

(defn gen-cs-id
  []
  (swap! db/cs-id dec))

(defn gen-ep-id
  []
  (swap! db/ep-id inc))

(defn find-ep
  [db ep-id]
  (-> db :execution-points (get ep-id)
    (or (throw (ex-info (str "No Execution Point with ID " ep-id) {:sc.ep/id ep-id})))))

(defn read-ep-info
  [db ep-id]
  (let [ep (find-ep db ep-id)
        cs (-> db :code-sites (get (-> ep :sc.ep/code-site :sc.cs/id)))]
    (-> ep
      (dissoc :sc.ep/private)
      (assoc :sc.ep/code-site cs))))

(defn ep-info
  [ep-id]
  (read-ep-info @db/db ep-id))

(defn read-cs-info
  [db cs-id]
  (-> db :code-sites (get cs-id)
    (or (throw (ex-info "No Code Site found with id " cs-id {:sc.cs/id cs-id})))))

(defn cs-info
  [cs-id]
  (read-cs-info @db/db cs-id))

(defn compilation-target
  [amp-env]
  ;; HACK detecting whether we're compiling to Clojure or ClojureScript (Val, 02 Oct 2017)
  ;; see https://groups.google.com/forum/#!msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ
  (if (:ns amp-env)
    :cljs
    :clj))

(defmulti extract-local-names
  (fn [strategy amp-env amp-form expr] strategy))

(defmethod extract-local-names :default
  [_ amp-env _ _]
  (case (compilation-target amp-env)
    :clj
    (into []
      (filter symbol?)                  ;; in CLJS will also have an :ns key
      (keys amp-env))
    :cljs
    (into []
      (keys (:locals amp-env)))))


(defn make-cs-data
  [opts cs-id expr amp-env amp-form]
  (let [fm (meta amp-form)]
    {:sc.cs/id cs-id
     :sc.cs/expr expr
     :sc.cs/local-names
     (extract-local-names (:sc/local-names-extractor opts)
       amp-env amp-form expr)
     :sc.cs/dynamic-var-names
     (get opts :sc/dynamic-vars nil)
     :sc.cs/file *file*                                     ;; TODO is this portable? (Val, 02 Oct 2017)
     :sc.cs/line (:line fm)
     :sc.cs/column (:column fm)}))

(defn save-cs
  [cs-data]
  (swap! db/db
    (fn [db]
      (update-in db [:code-sites (:sc.cs/id cs-data)] merge cs-data))))

(defn save-and-log-ep-scope
  [logger-fn ep-id cs-data local-bindings dynamic-var-bindings]
  (let [ep {:sc.ep/id ep-id
            :sc.ep/code-site (select-keys cs-data [:sc.cs/id])
            :sc.ep/local-bindings local-bindings
            :sc.ep/dynamic-var-bindings dynamic-var-bindings}]
    (swap! db/db
      (fn [db]
        (-> db
          (update-in [:code-sites (:sc.cs/id cs-data)] merge cs-data)
          (update-in [:execution-points ep-id] merge ep))))
    (logger-fn (ep-info ep-id))
    nil))

(defn cs-disabled?
  [cs-id]
  (get-in @db/db [:code-sites cs-id :sc.cs/disabled]))

(defn save-v
  [ep-id err? v]
  (read-ep-info
    (swap! db/db
      assoc-in [:execution-points ep-id (if err? :sc.ep/error :sc.ep/value)] v)
    ep-id))

(defn save-and-log-v
  [log-v ep-id error? v]
  (let [ep (save-v ep-id error? v)]
    (try
      (log-v ep)
      (catch Throwable err nil)))
  nil)

(defn emit-save-scope
  [logger-fn ep-id-s cs-data]
  `(when-not (cs-disabled? ~(:sc.cs/id cs-data))
     (save-and-log-ep-scope
       ~logger-fn
       ~ep-id-s
       (quote ~cs-data)
       ~(into {}
          (map (fn [l]
                 [`(quote ~l) l]))
          (:sc.cs/local-names cs-data))
       ~(into {}
          (map (fn [l]
                 [`(quote ~l) l]))
          (:sc.cs/dynamic-var-names cs-data)))))

(defn spy-emit
  [opts expr amp-env amp-form]
  (let [cp-target (compilation-target amp-env)
        {cs-logger-id :sc/spy-cs-logger-id
         spy-pre-eval-logger :sc/spy-ep-pre-eval-logger
         spy-post-eval-logger :sc/spy-ep-post-eval-logger
         :or {cs-logger-id :sc.api.logging/log-spy-cs
              spy-pre-eval-logger `sc.api.logging/log-spy-ep-pre-eval
              spy-post-eval-logger `sc.api.logging/log-spy-ep-post-eval
              }} opts
        cs-id (gen-cs-id)
        cs-data (make-cs-data opts cs-id expr amp-env amp-form)]
    (save-cs cs-data)
    (il/log-cs cs-logger-id cs-data)
    (let [local-names (:sc.cs/local-names cs-data)
          ep-id-s (gensym "ep-id")]
      `(let [~ep-id-s (gen-ep-id)]
         ~(emit-save-scope spy-pre-eval-logger ep-id-s cs-data)
         ~(when expr
            `(try
               (let [v# ~expr]
                 (when-not (cs-disabled? ~(:sc.cs/id cs-data))
                   (save-and-log-v ~spy-post-eval-logger ~ep-id-s false v#))
                 v#)
               (catch ~(case cp-target
                         :clj 'java.lang.Throwable
                         :cljs `:default) err#
                 (when-not (cs-disabled? ~(:sc.cs/id cs-data))
                   (save-and-log-v ~spy-post-eval-logger ~ep-id-s true err#))
                 (throw err#))))))))

(defn add-ep-brk-chan
  [ep-id]
  (let [ch (a/chan 1)]
    (swap! db/db assoc-in [:execution-points ep-id :sc.ep/private :sc.ep/brk-chan] ch)
    ch))

(defn brk-emit
  [opts expr amp-env amp-form]
  (let [cp-target (compilation-target amp-env)
        {cs-logger-id :sc/brk-cs-logger-id
         brk-pre-eval-logger :sc/brk-ep-pre-eval-logger
         brk-post-eval-logger :sc/brk-ep-post-eval-logger
         :or {cs-logger-id :sc.api.logging/log-brk-cs
              brk-pre-eval-logger `sc.api.logging/log-brk-ep-pre-eval
              brk-post-eval-logger `sc.api.logging/log-brk-ep-post-eval
              }} opts
        cs-id (gen-cs-id)
        cs-data (make-cs-data opts cs-id expr amp-env amp-form)]
    (save-cs cs-data)
    (il/log-cs cs-logger-id cs-data)
    (let [local-names (:sc.cs/local-names cs-data)
          ep-id-s (gensym "ep-id")]
      `(let [~ep-id-s (gen-ep-id)]
         (try
           (let [cs-disabled?# (cs-disabled? ~cs-id)
                 ch# (when-not cs-disabled?#
                       (add-ep-brk-chan ~ep-id-s))
                 _# ~(emit-save-scope brk-pre-eval-logger ep-id-s cs-data)
                 v# (let [cmd# (if cs-disabled?#
                                 {:sc.brk/type :sc.brk.type/loose}
                                 (a/<!! ch#))]
                      (if (nil? cmd#)
                        (throw (ex-info "BRK channel was closed." {:sc.ep/id ~ep-id-s}))
                        (case (:sc.brk/type cmd#)
                          :sc.brk.type/loose
                          ~expr

                          :sc.brk.type/loose-with
                          (:sc.brk/loose-value cmd#)

                          :sc.brk.type/loose-with-err
                          (throw (:sc.brk/loose-error cmd#))

                          (throw (ex-info "Malformed BRK command" {:sc.ep/id ~ep-id-s
                                                                   :cmd cmd#}))))
                      )]
             (when-not cs-disabled?#
               (save-and-log-v ~brk-post-eval-logger ~ep-id-s false v#))
             v#)
           (catch ~(case cp-target
                     :clj 'java.lang.Throwable
                     :cljs `:default) err#
             (when-not (cs-disabled? ~(:sc.cs/id cs-data))
               (save-and-log-v ~brk-post-eval-logger ~ep-id-s true err#))
             (throw err#)))))))

(defn brk-send-chan
  [ep-id cmd]
  (a/put! (-> (find-ep @db/db ep-id)
            :sc.ep/private :sc.ep/brk-chan
            (or (throw (ex-info "This Execution Point was not created via sc.api/brk" {:ep-id ep-id}))))
    cmd))

(defn valid-ep-identifier?
  [v]
  (or
    (and
      (integer? v)
      (pos? v))
    (and
      (vector? v)
      (let [[ep-id cs-id] v]
        (and
          (integer? ep-id) (pos? ep-id)
          (integer? cs-id) (neg? cs-id))))))

(defn validate-ep-identifier
  [ep-id]
  (when-not (valid-ep-identifier? ep-id)
    (throw (ex-info "ep-id should be either a positive number or a [(positive-number) (negative-number)] tuple."
             {:ep-id ep-id}))))

(defn resolve-code-site
  [ep-id]
  (validate-ep-identifier ep-id)
  (let [cs-id
        (cond
          (vector? ep-id)
          (second ep-id)

          (integer? ep-id)
          (-> (ep-info ep-id) :sc.ep/code-site :sc.cs/id))]
    (cs-info cs-id)))

(defn ep-binding
  [ep-id local-name]
  (-> (find-ep @db/db ep-id)
    :sc.ep/local-bindings (get local-name)))

(defn ep-var-binding
  [ep-id var-name]
  (-> (find-ep @db/db ep-id)
    :sc.ep/dynamic-var-bindings (get var-name)))

(defn resolve-ep-id
  [ep-id]
  (cond
    (vector? ep-id) (first ep-id)
    :else ep-id))

(defn disable!
  [cs-id]
  (swap! db/db assoc-in [:code-sites cs-id :sc.cs/disabled] true)
  cs-id)

(defn enable!
  [cs-id]
  (swap! db/db assoc-in [:code-sites cs-id :sc.cs/disabled] false)
  cs-id)

(defn dispose!
  [ep-id]
  (when-let [ch (get-in @db/db [:execution-points ep-id :sc.ep/private :sc.ep/brk-chan])]
    (a/close! ch))
  (swap! db/db update :execution-points dissoc ep-id)
  nil)

(defn dispose-all!
  []
  (->> @db/db :execution-points
    (map :sc.ep/id)
    (run! dispose!)))

(defn save-ep
  [ep-id ep-data]
  {:pre [(integer? ep-id) (pos? ep-id)
         (contains? ep-data :sc.ep/code-site)
         (contains? ep-data :sc.ep/local-bindings)]}
  (swap! db/db update-in [:execution-points ep-id]
    (fn [old-ep]
      (-> ep-data
        (assoc :ep.sc/id ep-id)
        (assoc :ep.sc/private (:sc.ep/private old-ep))
        (update :sc.ep/code-site #(select-keys % [:sc.cs/id]))))))

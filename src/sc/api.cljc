(ns sc.api
  (:require [sc.impl :as i]))

(defn spy-emit
  "Helper function for implementing (your own version of) the
  `spy` macro. See the source of `spy` for a usage example."
  [opts expr amp-env amp-form]
  (i/spy-emit opts expr amp-env amp-form))

(defmacro spy
  "Records the scope (i.e bindings of local names) and value
  of the expression it wraps at runtime;
  yields the value of the wrapped expression.

  At macro-expansion time, a Code Site Id (a negative integer)
  will be created and logged. Using this id, static information
  about the Code Site may be retrieved using (sc.api/cs-info cs-id).

  Each time the expression is evaluated, an Execution Point Id
  (a positive integer) will be created and logged.
  Dynamic information about this Execution Point
  (and the associated Code Site) may be retrieved using
  (sc.api/ep-info ep-id).

  Having noted the Execution Point Id, you can then 'recreate'
  the local runtime environment of the expression (its scope)
  using the `letsc` and `defsc` macros:

  (letsc ep-id
    ...)

  (defsc ep-id)

  The behaviour of this macro may be customized by passing a map literal
  as a first argument, containing some of the following keys:

  - :sc/spy-cs-logger-id (keyword, defaults to :sc.api.logging/log-spy-cs)
  The id of the logger function that will be called at macro-expansion time
  with Code Site information. This function must have been registered using
  `sc.api/register-cs-logger`.

  - :sc/spy-ep-pre-eval-logger-fn (expression, defaults to `sc.api.logging/log-spy-ep-pre-eval)
  An expression which evaluates to a function (typically a Var name), which will
  be invoked at runtime before the wrapped expression is evaluated with
  Execution Point information - typically to log the Execution Point Id.

  - :sc/spy-ep-post-eval-logger (expression, defaults to `sc.api.logging/log-spy-ep-post-eval)
  Like :sc/spy-ep-pre-eval-logger-fn, but the function will be invoked after the
  wrapped expression is evaluated - typically to log the value of the
  expression.

  - :sc/dynamic-vars (Seq of symbols, defaults to [])
  The names of the dynamic Vars which bindings should be captured at evaluation
  time.

  You may also provide your own defaults for these options by defining
  your own version of the `spy` macro: to do that,
  use `sc.api/spy-emit`.

  You can completely disable the logging and recording behaviour of `spy`
  at a given Code Site by using `(cs.api/disable! cs-id)`.

  You can discard the recorded information about a given Execution Point
  (typically for freeing memory) by using `(sc.api/dispose! ep-id)`.
  "
  ([] (spy-emit nil nil &env &form))
  ([expr] (spy-emit nil expr &env &form))
  ([opts expr] (spy-emit opts expr &env &form)))

(defn brk-emit
  "Helper function for implementing (your own version of) the
  `brk` macro. See the source of `brk` for a usage example."
  [opts expr amp-env amp-form]
  (i/brk-emit opts expr amp-env amp-form))

(defmacro brk
  "Like `spy`, but will block the executing Thread until
  you release it from the REPL.

  There are 3 ways of resuming execution at an Execution Point
  where it was suspended by `brk`:

  - `(sc.api/loose ep-id)`: resumes execution by evaluating
  the wrapped expression.
  - `(sc.api/loose-with ep-id value)`: resumes execution by
  yielding `value` instead of evaluating the wrapped expression.
  - `(sc.api/loose-with-err ep-id err)`: resumes execution by
  throwing `err` instead of evaluating the wrapped expression.

  Similarly to `spy`, you can customize the behaviour of `brk`
  by passing a literal map of options; the accepted keys are:
  - :sc/brk-cs-logger-id
  - :sc/brk-ep-pre-eval-logger
  - :sc/brk-ep-post-eval-logger
  - :sc/dynamic-vars

  You can completely disable the logging, recording, and blocking behaviour of `spy`
  at a given Code Site by using `(cs.api/disable! cs-id)`.

  You can discard the recorded information about a given Execution Point
  (typically for freeing memory) by using `(sc.api/dispose! ep-id)`. Execution
  Points which are being blocked will see their execution resumed by
  throwing an Exception.

  Since you will likely need your REPL to un-block, you should never
  execute a `(brk ...)` block directly from your REPL thread!
  "
  ([] (brk-emit nil nil &env &form))
  ([expr] (brk-emit nil expr &env &form))
  ([opts expr] (brk-emit opts expr &env &form)))

(defn ep-info
  "Given an Execution Point Id,
  returns a map of information about that Execution Point,
  and the associated Code Site.

  Returns a map featuring the following keys:
  - :sc.ep/id (positive integer)
  The id of the Execution Point
  - :sc.ep/code-site
  A map of data about the associated Code Site, see `sc.api/cs-info`
  - :sc.ep/value
  The value the wrapped expression evaluated to (if any)
  - :sc.ep/error
  The exception thrown by the evaluation of the wrapped exception (if any)
  - :sc.ep/local-bindings
  A map which keys are the names of the locals in the scope of the wrapped expression,
  and which values are the values the locals were bound to at this Execution Point.
  - :sc.ep/dynamic-var-bindings
  A map which keys are the fully-qualified names of the dynamic Vars recorded
  for the Code Site, and which values are the values they were bound to at
  the Execution Point.
  "
  [ep-id]
  (i/ep-info ep-id))

(defn cs-info
  "Given a Code Site Id, retrieves a map of information about that Code Site,
  featuring the following keys:

  - :sc.cs/id (negative integer):
  the ID of the Code Site
  - :sc.cs/expr (code form):
  the wrapped code expression
  - :sc.cs/local-names (vector of symbols):
  the names of the locals that are in the lexical environment of the Code Site.
  - :sc.cs/dynamic-vars (vector of symbols);
  the names of the dynamic Vars which bindings will be recorded at that Code Site.
  - :sc.cs/file (String):
  the path of the source file of the wrapped expr, when available
  - :sc.cs/line (integer):
  the line number of the `spy` or `brk` call in the source file, when available
  - :sc.cs/column (integer):
  the column number of the `spy` or `brk` call in the source file, when available
  - :sc.cs/disabled (boolean):
  whether this Code Site is in a disabled state."
  [cs-id]
  (i/cs-info cs-id))

(defmacro letsc
  "Given an Execution Point Id `ep-id`, will expand to `let` and `binding`
  forms wrapping `body` which will reproduce the local and dynamic Var bindings
  that were recorded by the `spy` and `brk` macro at the Execution Point.

  In practice, this enables you to run `body` in the same local environment where
  the Execution Point was recorded.

  `ep-id` may be provided either as a positive integer literal (the Execution Point Id, e.g 8),
  or as a vector literal of one positive integer (the Execution Point Id) and one
  negative integer (the Code Site Id), e.g [8 -3]. Note that in Clojure environments
  where compilation and execution don't happen in the same address space
  (which is the case for JVM-compiled ClojureScript), only the second form is accepted,
  as there is no way of inferring the Code Site Id from the Execution Point Id
  at macro-expansion."
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
  "Given an Execution Point Id, expands to several `(def ...)` expressions,
  defining Vars which names match the names of the locals captured
  by the `spy` or `brk` macros at the Execution Point.

  In practice, this enables you to make some local, temporary bindings available
  globally and durably in a namespace (as Vars), which is convenient
  for evaluating forms using these bindings at the REPL.

  Note: `defsc` will not recreate the dynamic Var bindings at this Execution Point.

  You can un-define (via ns-unmap) the Vars `def`ined by `defsc`
  for a code site by using `sc.api/undefsc`.

  `ep-id` may be provided either as a positive integer literal (the Execution Point Id, e.g 8),
  or as a vector literal of one positive integer (the Execution Point Id) and one
  negative integer (the Code Site Id), e.g [8 -3]. Note that in Clojure environments
  where compilation and execution don't happen in the same address space
  (which is the case for JVM-compiled ClojureScript), only the second form is accepted,
  as there is no way of inferring the Code Site Id from the Execution Point Id
  at macro-expansion."
  [ep-id]
  (let [cs (i/resolve-code-site ep-id)
        ep-id (i/resolve-ep-id ep-id)]
    (into []
      (map (fn [ln]
             `(def ~ln (i/ep-binding ~ep-id (quote ~ln)))))
      (:sc.cs/local-names cs))))

(defmacro undefsc
  "Given an identifier `ep-or-cs-id` for a Code Site,
  undoes the effect of `defsc` by un`def`ing the Vars defined by `defsc`
  (via `ns-unmap`).

  `ep-or-cs-id` may be provided in one of the following forms:
  - a positive integer literal, identifying an Execution Point, e.g 8
  - a negative integer literal, identifying a Code Site, e.g -3
  - an [ep-id cs-id] vector literal, e.g [8 -3]"
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
  "Given an Execution Point Id `ep-id` for an Execution Point
  created by `brk` which is currently in a suspended state,
  resumes execution by evaluating the wrapped form."
  [ep-id]
  (i/brk-send-resume-cmd ep-id {:sc.brk/type :sc.brk.type/loose}))

(defn loose-with
  "Same as `sc.api/loose`, except that execution is resumed by
  yielding the provided value `v` instead of evaluating the wrapped
  expression."
  [ep-id v]
  (i/brk-send-resume-cmd ep-id {:sc.brk/type :sc.brk.type/loose-with
                          :sc.brk/loose-value v}))

(defn loose-with-err
  "Same as `sc.api/loose`, except that execution is resumed by
  throwing `err` instead of evaluating the wrapped
  expression."
  [ep-id err]
  (i/brk-send-resume-cmd ep-id {:sc.brk/type :sc.brk.type/loose-with-err
                          :sc.brk/loose-error err}))

(defn disable!
  "Disables all logging, recording, and blocking behaviour at a Code Site.
  No more Execution Points will be created at that Code Site, but the previously
  collected ones remain fully available. Idempotent. The Code Site can be re-enabled
  by calling `sc.api/enable!`"
  [cs-id]
  (i/disable! cs-id))

(defn enable!
  "Undoes the action of `sc.api/disable!`. Idempotent."
  [cs-id]
  (i/enable! cs-id))

(defn dispose!
  "Frees up resources held by the given Execution Point.
  Useful for freeing memory.

  If the Execution Point is in a suspened state (as by `brk`),
  will resume execution by throwing an Exception."
  [ep-id]
  (i/dispose! ep-id))

(defn dispose-all!
  "Disposes of all Execution Points (as per `sc.api/dispose!`)"
  []
  (i/dispose-all!))

(defn gen-ep-id
  "Generates a unique Execution Point Id, a positive integer."
  []
  (i/gen-ep-id))

(defn save-ep
  "Creates or updates an Execution Point. `ep-data` should follow
  the same schema as what is returned by ep-info; only the :sc.ep/local-bindings
  and :sc.ep/code-site keys are required, and only :sc.cs/id will be taken in account
  for the Code Site. Returns ep-id.

  Useful for artificially creating an Execution Point that is a slight variation
  of another one, e.g:

  (sc.api/save-ep (sc.api/gen-ep-id)
    (-> (sc.api/ep-info 17)
      (select-keys [:sc.ep/local-bindings :sc.ep/code-site])
      (assoc-in [:sc.ep/local-bindings 'x] 32)))
  "
  [ep-id ep-data]
  (i/save-ep ep-id ep-data))




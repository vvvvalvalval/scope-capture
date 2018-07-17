(ns sc.api.from)

(def ^:dynamic *caller*
  "A Dynamic Var which is compared to the :sc/called-from option (when specified),
  to restrict the recording of Execution Points to a specific calling context."
  nil)

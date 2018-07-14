# Tutorial

The first step in using scope-capture is making sure the `sc.api` namespace is loaded:

```clojure
(require 'sc.api)
```

## Recording and recreating scopes

Imagine you want to debug the following piece of code, in this case a hairy function 
 that computes the distance between 2 points based on their coordinates, using 
 the [Haversine formula](https://en.wikipedia.org/wiki/Haversine_formula):
 
```clojure
(ns sc.lab.tutorial)

(defn haversine
  [x]
  (let [s (Math/sin (/ (double x) 2.0))]
    (* s s)))

(def distance
  (let [earth-radius 6.371e6
        radians-per-degree (/ Math/PI 180.0)]
    (fn [p1 p2]
      (let [[lat1 lng1] p1
            [lat2 lng2] p1
            phi1 (* lat1 radians-per-degree)
            lambda1 (* lng1 radians-per-degree)
            phi2 (* lat2 radians-per-degree)
            lambda2 (* lng2 radians-per-degree)]
        (* 2 earth-radius
          (Math/asin 
            (Math/sqrt
              (+  
                (haversine (- phi2 phi1))
                (* 
                  (Math/cos phi1)
                  (Math/cos phi2)
                  (haversine (- lambda2 lambda1)))
                ))))
        ))))
```

There must be a bug in this function: whatever the inputs, the result is `0.0` meters!
 
```clojure 
(def Paris [48.8566 2.3522])
(def New-York [40.7134 -74.0055])
(def Athens [37.9838 23.7275])

(distance Paris New-York)
=> 0.0
(distance Paris Athens)
=> 0.0
(distance New-York Athens)
=> 0.0
```

To find the bug, you wrap the last expression with an `sc.api/spy` call:

```clojure
;; change the previously defined function to the following:
(def distance
  (let [earth-radius 6.371e6
        radians-per-degree (/ Math/PI 180.0)]
    (fn [p1 p2]
      (let [[lat1 lng1] p1
            [lat2 lng2] p1
            phi1 (* lat1 radians-per-degree)
            lambda1 (* lng1 radians-per-degree)
            phi2 (* lat2 radians-per-degree)
            lambda2 (* lng2 radians-per-degree)]
        (sc.api/spy ;; HERE
          (* 2 earth-radius
            (Math/asin
              (Math/sqrt
                (+
                  (haversine (- phi2 phi1))
                  (*
                    (Math/cos phi1)
                    (Math/cos phi2)
                    (haversine (- lambda2 lambda1)))
                  )))))
        ))))
```

When compiling the function (i.e when evaluating the `(def distance ...)` form in the REPL),
 you should see a message like the following being logged:
  
```
SPY <-1> /Users/val/projects/scope-capture/lab/sc/lab/tutorial.clj:19
  At Code Site -1, will save scope with locals [earth-radius radians-per-degree p2 phi2 phi1 lat1 lng2 lambda2 lat2 lambda1 p1 vec__15610 vec__15609 lng1]
```

`-1` is the id of the _Code Site_ at which we placed the `spy` call, which you can think of 
as a breakpoint in a debugger.
 
Now invoke the function with some inputs:

```clojure
(distance Paris Athens)
=> 0.0
```

You should see a message like the following being logged:

```
SPY [1 -1] /Users/val/projects/scope-capture/lab/sc/lab/tutorial.clj:19
  At Execution Point 1 of Code Site -1, saved scope with locals [earth-radius radians-per-degree p2 phi2 phi1 lat1 lng2 lambda2 lat2 lambda1 p1 vec__15610 vec__15609 lng1]
SPY [1 -1] /Users/val/projects/scope-capture/lab/sc/lab/tutorial.clj:19
(* 2 earth-radius (Math/asin (Math/sqrt (+ (haversine (- phi2 phi1)) (* (Math/cos phi1) (Math/cos phi2) (haversine (- lambda2 lambda1)))))))
=>
0.0
```

`1` is the id of the _Execution Point_ at which our `spy` call just ran.
 Now let's use the `sc.api/ep-info` function to get information about that Execution Point.
 You typically won't use `ep-info` for everyday development, but it's useful for understading what `spy` does:
 
```clojure
(sc.api/ep-info 1)
=>
{:sc.ep/id 1,
 :sc.ep/code-site {:sc.cs/id -1,
                   :sc.cs/expr (*
                                2
                                earth-radius
                                (Math/asin
                                 (Math/sqrt
                                  (+
                                   (haversine (- phi2 phi1))
                                   (* (Math/cos phi1) (Math/cos phi2) (haversine (- lambda2 lambda1))))))),
                   :sc.cs/local-names [earth-radius
                                       radians-per-degree
                                       p2
                                       phi2
                                       phi1
                                       lat1
                                       lng2
                                       lambda2
                                       lat2
                                       lambda1
                                       p1
                                       vec__15610
                                       vec__15609
                                       lng1],
                   :sc.cs/dynamic-var-names nil,
                   :sc.cs/file "/Users/val/projects/scope-capture/lab/sc/lab/tutorial.clj",
                   :sc.cs/line 19,
                   :sc.cs/column 9},
 :sc.ep/local-bindings {earth-radius 6371000.0,
                        radians-per-degree 0.017453292519943295,
                        p2 [40.7134 -74.0055],
                        phi2 0.8527085313298616,
                        phi1 0.8527085313298616,
                        lat1 48.8566,
                        lng2 2.3522,
                        lambda2 0.041053634665410614,
                        lat2 48.8566,
                        lambda1 0.041053634665410614,
                        p1 [48.8566 2.3522],
                        vec__15610 [48.8566 2.3522],
                        vec__15609 [48.8566 2.3522],
                        lng1 2.3522},
 :sc.ep/dynamic-var-bindings {},
 :sc.ep/value 0.0}
```
 
As you can see, when our code executed, the `spy` macro recorded a lot of 
runtime information about the Execution Point: the value of the wrapped expression
(`:sc.ep/value`), as well as the bindings of local names (`:sc.ep/local-bindings`).

We'll now automatically recreate the environment of our Execution Point using
 the `sc.api/defsc` macro. Evaluate this in the same namespace as our function:
 
```clojure 
(sc.api/defsc 1)
=>
[#'sc.lab.tutorial/earth-radius
 #'sc.lab.tutorial/radians-per-degree
 #'sc.lab.tutorial/p2
 #'sc.lab.tutorial/phi2
 #'sc.lab.tutorial/phi1
 #'sc.lab.tutorial/lat1
 #'sc.lab.tutorial/lng2
 #'sc.lab.tutorial/lambda2
 #'sc.lab.tutorial/lat2
 #'sc.lab.tutorial/lambda1
 #'sc.lab.tutorial/p1
 #'sc.lab.tutorial/vec__15610
 #'sc.lab.tutorial/vec__15609
 #'sc.lab.tutorial/lng1]
```

What just happened? `defsc` has just `def`ined global Vars that have the same name
 as the locals of our Execution Point, which means we can now evaluate sub-expressions of our function body.
 
```clojure 
        earth-radius
=> 6371000.0

         p1
=> [48.8566 2.3522]

            lambda1
=> 0.041053634665410614

                (haversine (- phi2 phi1))
=> 0.0

                  (Math/cos phi2)
=> 0.6579458609946129

                           (- phi2 phi1)
=> 0.0
                             (- lambda2 lambda1)
=> 0.0
```

We then quickly realize the cause of the bug: `lat2` and `lng2` are derived from `p1`
 instead of `p2`!
 
We can now fix our function. Don't forget to remove the `spy` call from your code:
 it shouldn't go to production, as it would cause a memory leak!

Speaking of memory leaks: we can free the memory used by our Execution Point using 
 `sc.api/dispose!`
 
```clojure
(sc.api/dispose! 1)
```

## Breakpoints

`sc.api/brk` is similar to `sc.api/spy`, except that it wil block the running thread
 (instead of immediately evaluating the wrapped expression and saving the result),
 until you choose to release it. For example:
 
```clojure 
(require '[clojure.string :as str])

(defn greet!
  [first-name]
  (let [msg (str "Hello, " (str/capitalize first-name) "!")]
    (println
      (sc.api/brk msg) ;; brk call HERE
      )))
;BRK <-2> /Users/val/projects/scope-capture/lab/sc/lab/tutorial.clj:183
;At Code Site -2, will save scope with locals [first-name msg]
=> #'sc.lab.tutorial/greet!
```

Now, invoke this function in another thread:
 
```clojure 
(def fut
  (future
    (greet! "jude")))
;BRK [3 -2] /Users/val/projects/scope-capture/lab/sc/lab/tutorial.clj:183
;saved scope with locals [first-name msg], use sc.api/loose(-...) to resume execution.
=> #'sc.lab.tutorial/fut
```

Like before with `spy`, this as created an Execution Point and recorded runtime information
 about it, which gives you the opportunity to recreate its environment using e.g `sc.api/defsc`.

However, the evaluation of our code is not saved; instead, it is suspended:
 
```clojure
fut
=> #object[clojure.core$future_call$reify__6962 0x6757f3f5 {:status :pending, :val nil}]
```

We now have 3 options to resume it:

```clojure 
;; continue execution normally
(sc.api/loose 3)
; Hello Jude!

;; continue execution by replacing `msg` with the provided value instead of evaluating it
(sc.api/loose-with 3 "Bonjour, Jude !")
; Bonjour, Jude !

;; continue execution by throwing the given Exception
;; (useful to prevent downstream side-effects or break out of a loop)
(sc.api/loose-with-err 3 (ex-info "Aaaaaarrrrgh" {}))
````


## Disabling code sites

You can disable the side-effects of the `spy` and `brk` macros at a given Code Site
 by calling `sc.api/disable!`
 
```clojure 
(sc.api/dispose! -2)
```

This is useful if you placed a `(brk ...)` call inside a loop, and want to suspend
 only one iteration.
 
## Usage from ClojureScript

In Clojure platforms where compilation and execution don't share their address space
 (such as JVM-compiled ClojureScript, which is currently the most popular way of programming in ClojureScript)
 there is no way to link compile-time information to an Execution Point Id.
 
Therefore, when using the `defsc` and `letsc` macros, you need to explicitly pass
 the Code Site Id in addition to the Execution Point Id, which is done by wrapping both
 in a vector literal:
 
```clojure
(sc.api/defsc [3 -2])

(sc.api/letsc [3 -2]
  ...)
```

## Dealing with large outputs: `spyqt` and `brkqt`

By default, `sc.api/spy` and `sc.api/brk` will print the recorded expression and values in their entirety; this can
 become cumbersome when the values are large (or infinite).

In such cases, you can use the more 'quiet' alternatives: `sc.api/spyqt` and `sc.api/brkqt`, which only print the type
 of the recorded values.

Their source code also provides a basic example of custom logging (see [below](#customization)).

## Customization

You can customize the behaviour of `spy` and `brk` by defining your own macros that call 
 `sc.api/spy-emit` and `sc.api/brk-emit`.
 
For instance, you can customize the log messages emitted by `spy` at compile-time and runtime.
 Here's an example that does both using the [Timbre](https://github.com/ptaoussanis/timbre) library:
 
```clojure 
(ns myapp.dev
  (:require [taoensso.timbre :as log]
            [sc.api]
            [sc.api.logging]))

;;;; defining custom loggers
(defn log-spy-cs-with-timbre
  [cs-data]
  (log/info "At Code Site" (:sc.cs/id cs-data)
    "will save scope with locals" (:sc.cs/local-names cs-data)
    (str "(" (:sc.cs/file cs-data) "." (:sc.cs/line cs-data) ":" (:sc.cs/column cs-data) ")")))

(sc.api.logging/register-cs-logger
  ::log-spy-cs-with-timbre
  #(log-spy-cs-with-timbre %))

(defn log-spy-ep-with-timbre
  [ep-data]
  (let [cs-data (:sc.ep/code-site ep-data)]
    (log/info "At Execution Point" 
      [(:sc.ep/id ep-data) (:sc.cs/id cs-data)]
      (:sc.cs/expr cs-data) "=>" (:sc.ep/value ep-data))))

;;;; defining our own spy macro
(def my-spy-opts 
  ;; mind the syntax-quote '`'
  `{:sc/spy-cs-logger-id ::log-spy-cs-with-timbre
    :sc/spy-ep-post-eval-logger log-spy-ep-with-timbre})

(defmacro my-spy
  ([] (sc.api/spy-emit my-spy-opts nil &env &form))
  ([expr] (sc.api/spy-emit my-spy-opts expr &env &form))
  ([opts expr] (sc.api/spy-emit (merge my-spy-opts opts) expr &env &form)))
```

You could also use these customization hooks to integrate `scope-capture` to other tools, e.g IDEs!

## Recreating the environment by launching a sub-REPL

In Clojure JVM, there is another way of recreating the environment of an Execution Point
 without creating new Vars like `sc.api/defsc` does.

It consists of launching a sub-REPL (in the sense of `clojure.main/repl`)
 which wraps each expression in a `(sc.api/letsc <ep-id> ...)` block before 
 evaluating it. This is exactly what `sc.repl/ep-repl` does:
 
```clojure
user=> x
;CompilerException java.lang.RuntimeException: Unable to resolve symbol: x in this context
user=> (sc.repl/ep-repl 1)
=> nil 
SC[1 -1]=> x
=> 13
SC[1 -1]=> :repl/quit
user=>
```

## Spying at a distance (advanced)

Sometimes, you want to spy at a given Code Site, but only in a very specific context.

For instance, imagine you want investigate an Exception thrown in a function `f`;
 when reproducing the bug, you may see that `f` gets called from many places, but
 the error only happens when it's called from function `g`. If you place a
 `(sc.api/spy ...)` call inside `f` to investigate, most of the recorded Execution Points
 will be useless noise to you, because you're only interested in what happens downstream
 of `g`.

For such cases, you can use the `sc.api/calling-from` macro and
 `:sc/only-from` option to only spy / brk downstream of a specific place in your code:

```clojure
(defn f
  "A fairly generic function that gets called from many places."
  [x]
  ;; ...
  (sc.api/spy `{:sc/only-from :foo}
    ...)
  ;; ...
  )

;; [...]

(defn g []
  ;; ...
  (sc.api/calling-from :foo
    (f ...))
  ;; ...
  )
```

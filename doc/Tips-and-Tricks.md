# Tips and Tricks

## You can `(sc.api/spy)` without a wrapped expression

The key value proposition of `scope-capture` is not to record the values of an expression, but to recreate its local bindings.
For this reason, you can write `(sc.api/spy)` without wrapping an expression, which can be less intrusive.

Example:

```clojure
(let [x (foo)
      y (bar x)]
  (sc.api/spy) ;; FIXME remove when done
  (something-something x y))
```


## To only spy when errors occur, you can `(sc.api/spy)` from inside a `catch` clause.

Example:

```clojure
(try
  (my-fallible-code)
  (catch Throwable err 
    (sc.api/spy err)
    (throw err))
```

You might want to write a custom macro `spy-on-err` doing that:

```clojure
(dev/spy-on-err
  (my-fallible-code)
  err)
```


## Restoring the last Execution Point at a given Code Site

You might want to create an editor shortcut for the following snippet

```clojure
(eval `(sc.api/defsc ~(sc.api/last-ep-id)))
```

There is no built-in macro for that in the library, because it cannot work in standard ClojureScript environments.


## You can combine several saved scopes

For example, you could use `defsc` twice to combine values saved from different sites:

```clojure
(defn my-fun-1 [a b]
  (sc.api/spy) ;; saved a and b in EP [-1 3]
  (something-about a b))

(defn my-fun-2 [x y]
  (sc.api/spy) ;; saved x and y in EP [-2 4]
  (something-about x y))

;; combining both scopes
(sc.api/defsc [-1 3])
(sc.api/defsc [-2 4])
(play-around-with a b x y)
```



## Writing your custom `spy` and `brk` macros is allowed and encouraged

People have vastly different tastes and opportunities regarding their programming experience, what and how notifications should be displayed, etc.
This is why `scope-capture` is designed to be [customizable](./Tutorial.md#customization) rather than opinionated on these aspects.


## Adding a reader macro shorthand

For simple cases where you just want to wrap a form with `sc.api/spy`, instead of writing

```clojure
(require 'sc.api)
(sc.api/spy
  (my-very interesting expression))
```

You can add a reader macro, and write it like this:

```clojure
#sc/spy (my-very interesting expression)
```

To add such a reader macro defined in your own `dev` namespace, you would add

```clojure
{sc/spy dev/read-sc-spy}
```

To `data_readers.clj` at a root of the classpath, as described in [the `clojure.core/*data-readers*` docstring](https://clojuredocs.org/clojure.core/*data-readers*) (you could use `dev/data_readers.clj` if `dev` is a source-path for your development environment), and define the reader macro in your `dev` namespace:

```clojure
(defn read-sc-spy [form]
  (require 'sc.api)
  `(sc.api/spy ~form))
```

Since the reader macro itself requires the namespace, you can now simply add `#sc/spy` before the form you wish to capture in any namespace, evaluate it, and execute it.

NOTE: this tip will probably not work for ClojureScript.

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

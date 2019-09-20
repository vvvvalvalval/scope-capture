# scope-capture

`[vvvvalvalval/scope-capture "0.3.2"]`

[![Clojars Project](https://img.shields.io/clojars/v/vvvvalvalval/scope-capture.svg)](https://clojars.org/vvvvalvalval/scope-capture)

This library eases REPL-based development, by providing macros which help you save and restore the local environment of a piece of code with minimal effort.

Project status: beta quality. On the other hand, you typically will only use it in your development environment, so there's little risk in adoption.

**[Demo video:](https://vimeo.com/237220354)**

[![Demo preview](https://i.vimeocdn.com/video/659566887.webp?mw=1000&mh=561)](https://vimeo.com/237220354)

**[Talk at Clojure Days 2018:](https://www.youtube.com/watch?v=dCInpNWlZ4k)**

[![Talk preview](https://img.youtube.com/vi/dCInpNWlZ4k/0.jpg)](http://www.youtube.com/watch?v=dCInpNWlZ4k)

## Similar libraries

* [miracle.save](https://github.com/Saikyun/miracle.save) - capture arguments and results of function(s) in a namespace

## Rationale

This library is designed to support the programming / debugging methodology advocated by Stuart Halloway in this blog post: [_REPL Debugging: No Stacktrace Required_](http://blog.cognitect.com/blog/2017/6/5/repl-debugging-no-stacktrace-required), which consists of:

1. recreating the environment in which a piece of code runs (using `def`s) 
2. evaluating forms in the code buffer to narrow down the cause of the problem

What the blog post does not mention is that oftentimes, this first step (recreating the local environment) can get very tedious and error-prone; especially when the values of the environment are difficult to fabricate (HTTP requests, database connections, etc.), which can be the case for online programs such as web servers, or if you don't have a keen knowledge of the project.

scope-capture alleviates this pain by:

* providing macros that let you **_save snapshots of the local environment on the fly_**: `sc.api/spy` (which additionally saves the evaluated wrapped expression - or the resulting error, which you can inspect using `sc.api/saved-value`) and `sc.api/brk` (which acts as a breakpoint, blocking the flow of the program until you choose to release it from the REPL using `sc.api/loose!`, possibly with a value which supersedes the evaluation of the wrapped expression using `sc.api/loose-with!` or `sc.api/loose-with-ex!`)
* providing macros that let you **_restore these snapshots from the REPL_**: `sc.api/defsc` (recreates the environment with global vars, i.e by `def`-ing the local names) and `sc.api/letsc` (recreates the environment with locals, i.e by `let`-ing the local names)

### Benefits

As a consequence, **_to reproduce the runtime context of a code expression, you only need to get it to execute once_** (not necessarily from the REPL). This makes for:

1. **Easier debugging**, as you can immediately focus on searching for the cause of the bug
2. **Easier project onboarding, especially for beginners**. For someone new to project, and even more so someone new to Clojure, manually fabricating the context of a piece of code at the REPL can be a daunting task, as it requires a relatively comprehensive knowledge of the flow of values through the program. This library lets you do that in a completely mechanical and uninformed way. 
3. **Easier exploration.** Because it lowers the barrier to experimentation, this library can be also useful for other tasks than debugging and development, such as running one-off queries as variants of existing ones, or just understading how a project works.

## Features

* Recording / logging the runtime scope and evaluation of an expression: `sc.api/spy`
* Re-creating a recorded scope: with let-bound locals: `sc.api/letsc` / with global Vars: `sc.api/defsc`/ with a sub-REPL: `sc.repl/ep-repl`
* Accessing recorded information: `sc.api/ep-info`, `sc.api/cs-info`
* Suspending and resuming execution (similar to breakpoints): `sc.api/brk`, `sc.api/loose`, `sc.api/loose-with`, `sc.api/loose-with-err`
* Cleaning up after yourself: `sc.api/undefsc`, `sc.api/dispose!`, `sc.api/disable!`
* Customizing: `sc.api/spy-emit`, `sc.api/brk-emit`, `sc.api.logging/register-cs-logger`
* Artificially creating scopes: `sc.api/save-ep`

For nREPL integration, see also the [scope-capture-nrepl](https://github.com/vvvvalvalval/scope-capture-nrepl) companion library.

## Installation

With both Leiningen and Boot, it's better not to include `scope-capture` in your project dependencies but rather in your local environment. Here's how to do it :

### Leiningen

Add the following to the `:user` profile in `~/.lein/profiles.clj`:

``` clojure
:dependencies [[vvvvalvalval/scope-capture "0.3.2"]]
:injections [(require 'sc.api)]
```

### Boot

Using a `$BOOT_HOME/profile.boot` (usually `~/.boot/profile.boot`) file:

``` clojure
(set-env! :dependencies #(conj % '[vvvvalvalval/scope-capture "0.3.2"]))
(require 'sc.api)
```

## Docs and API docs

[![cljdoc badge](https://cljdoc.org/badge/vvvvalvalval/scope-capture)](https://cljdoc.org/d/vvvvalvalval/scope-capture/CURRENT)

## Usage

(See also the **[detailed tutorial](doc/Tutorial.md)**)

```clojure
(require 'sc.api)
```

Assume you need to debug a function with a bunch of locals:

```clojure
(def my-fn 
  (let [a 23 
        b (+ a 3)]
    (fn [x y z]
      (let [u (inc x)
            v (+ y z u)]
        (* (+ x u a)
          ;; Insert a `spy` call in the scope of these locals
          (sc.api/spy
            (- v b)))
        ))))
=> #'sc.lab.example/my-fn
```

When compiling the function, you will see a log like the following:

```
SPY <-3> /Users/val/projects/scope-capture/lab/sc/lab/example.cljc:52 
  At Code Site -3, will save scope with locals [a b x y z u v]
```

Now call the function:

```clojure
(my-fn 3 4 5)
=> -390
```

You will see a log like the following:

```
SPY [7 -3] /Users/val/projects/scope-capture/lab/sc/lab/example.cljc:52 
  At Execution Point 7 of Code Site -3, saved scope with locals [a b x y z u v]
SPY [7 -3] /Users/val/projects/scope-capture/lab/sc/lab/example.cljc:52 
(- v b)
=>
-13
```

You can now use the `letsc` macro to recreate the scope of your `spy` call at the previous execution: 

```clojure
(sc.api/letsc 7
  [a b u v x y z])
=> [23 26 4 13 3 4 5]

(sc.api/letsc 7
  (+ x u a))
=> 30  
```

You can also use `defsc` to recreate the scope by def-ing Vars, which is more convenient if you're using the 'evaluate form in REPL' command of your editor:

```clojure
(sc.api/defsc 7)
=> [#'sc.lab.example/a #'sc.lab.example/b #'sc.lab.example/x #'sc.lab.example/y #'sc.lab.example/z #'sc.lab.example/u #'sc.lab.example/v]

a 
=> 23
 
x 
=> 3

(+ x z u)
=> 12 
```

If your REPL supports it, you can also achive the same effect by launching a sub-REPL
(won't work with nREPL)

```clojure
(sc.repl/ep-repl 7)

;;;; a, b, u, v etc. will always be in scope from now on
```

## Project goals

* Providing practical ways of recreating the runtime environment of a piece of code at the REPL
* Targeting a wide range of Clojure execution platforms
* Providing a good foundation for additional tooling, e.g editor integrations
* Being well documented, and friendly to beginners
* Being customizable.

## Caveats

Using scope-capture with ClojureScript is supported, but can get tricky: when in trouble, see the [Pitfalls with ClojureScript REPLs](https://github.com/vvvvalvalval/scope-capture/wiki/Pitfalls-with-(browser-connected)-ClojureScript-REPLs) Wiki Page.

Dynamic Vars:

* In order for `spy`/`brk` to record dynamic Var bindings, the list of dynamic Vars to observe must be explicitly declared as an option, e.g 

```clojure
(sc.api/spy 
  {:sc/dynamic-vars [*out* my.app/*foo*]}
  expr)
```

* `sc.api/letsc` will rebind the dynamic Vars that were captured (using `(binding [...] ...)`), `sc.api/defsc` will not. 

Local/global name collisions:

* `sc.api/defsc` will overwrite the global Vars that have the same name as the locals it has captured 
* In particular, if a global Var has been `def`ed via `defonce`, `sc.api/defsc` won't work for a local of the same name.

For these reasons, using `sc.api/letsc` or a sub-REPL is generally more error-proof than using `defsc`, although in many cases it is less practical.

## License

Copyright © 2017 Valentin Waeselynck and contributors.

Distributed under the MIT license.

(ns sc.lab.example
  (:require [sc.api]))

(def foo
  (let [a 1
        b (inc a)]
    (fn [x y]
      (let [z (+ x (* a y))]
        (* 3
          (sc.api/spy
            (+ b z)))))))

(def foo2
  (let [a 1
        b (inc a)]
    (fn [x y]
      (let [z (+ x (* a y))]
        (* 3
          (sc.api/brk
            (+ b z)))))))

(comment
  (sc.repl/ep-repl 1)

  (foo 23 1)

  (future
    (foo2 1 2))

  (def f (future
           (dotimes [i 100]
             (foo2 i (inc i)))))

  (sc.api/ep-info 8)

  (sc.api/loose 210)
  (sc.impl/disable -7)

  (sc.api/loose-with-err 7 (ex-info "aajfkdlsfjslkfjs" {}))

  *2)


(def my-fn
  (let [a 23
        b (+ a 3)]
    (fn [x y z]
      (let [u (inc x)
            v (+ y z u)]
        (* (+ x u a)
          ;; insert a `spy` call in the scope of these locals
          (sc.api/spy
            (- v b)))
        ))))

(my-fn 3 4 5)

(sc.api/letsc 2
  (+ x u a))

(sc.api/letsc 2
  [a b u v x y z])

(sc.api/defsc 2)

(+ x z u)

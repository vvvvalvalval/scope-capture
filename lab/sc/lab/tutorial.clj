(ns sc.lab.tutorial
  (:require [clojure.string :as str]))

(defn haversine
  [x]
  (let [s (Math/sin (/ (double x) 2))]
    (* s s)))

(def distance
  (let [earth-radius 6.371e6
        radians-per-degree (/ 180.0 Math/PI)]
    (fn [p1 p2]
      (let [[lat1 lng1] p1
            [lat2 lng2] p2
            phi1 (* lat1 radians-per-degree)
            lambda1 (* lng1 radians-per-degree)
            phi2 (* lat2 radians-per-degree)
            lambda2 (* lng2 radians-per-degree)]
        (sc.api/spy
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

(def Paris [48.8566 2.3522])
(def New-York [40.7134 -74.0055])
(def Athens [37.9838 23.7275])

(distance Paris New-York)
=> 0.0
(distance Paris Athens)
=> 0.0
(distance New-York Athens)
=> 0.0


(sc.api/ep-info 1)

(sc.api/defsc 1)


(defn say-hello
  [first-name]
  (dotimes [i 10]
    (println "Hello" (sc.api/brk first-name) i)))

(future
  (say-hello "Pauline"))


(sc.api/loose 10)

(sc.api/loose-with 11 "Valentin")

(sc.api/loose-with-err  8 (ex-info "Aaaaaarg" {}))


















(def distance
  (let [earth-radius 6.371e6
        radians-per-degree (/ 180.0 Math/PI)]
    (fn [p1 p2]
      (let [[lat1 lng1] p1
            [lat2 lng2] p1
            phi1 (* lat1 radians-per-degree)
            lambda1 (* lng1 radians-per-degree)
            phi2 (* lat2 radians-per-degree)
            lambda2 (* lng2 radians-per-degree)]
        (sc.api/spy
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
                                       vec__15673
                                       radians-per-degree
                                       p2
                                       vec__15672
                                       phi2
                                       phi1
                                       lat1
                                       lng2
                                       lambda2
                                       lat2
                                       lambda1
                                       p1
                                       lng1],
                   :sc.cs/dynamic-var-names nil,
                   :sc.cs/file "/Users/val/projects/scope-capture/lab/sc/lab/tutorial.clj",
                   :sc.cs/line 52,
                   :sc.cs/column 9},
 :sc.ep/local-bindings {earth-radius 6371000.0,
                        vec__15673 [48.8566 2.3522],
                        radians-per-degree 57.29577951308232,
                        p2 [37.9838 23.7275],
                        vec__15672 [48.8566 2.3522],
                        phi2 2799.276981358858,
                        phi1 2799.276981358858,
                        lat1 48.8566,
                        lng2 2.3522,
                        lambda2 134.77113257067222,
                        lat2 48.8566,
                        lambda1 134.77113257067222,
                        p1 [48.8566 2.3522],
                        lng1 2.3522},
 :sc.ep/dynamic-var-bindings {},
 :sc.ep/value 0.0}


(sc.api/defsc 1)

(def distance
  (let [earth-radius 6.371e6
        radians-per-degree (/ 180.0 Math/PI)]
    (fn [p1 p2]
      (let [[lat1 lng1] p1
            [lat2 lng2] p2
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


(require '[clojure.string :as str])

(defn greet!
  [first-name]
  (let [msg (str "Hello, " (str/capitalize first-name) "!")]
    (println
      (sc.api/brk msg) ;; BRK call here
      )))
;BRK <-2> /Users/val/projects/scope-capture/lab/sc/lab/tutorial.clj:183
;At Code Site -2, will save scope with locals [first-name msg]
=> #'sc.lab.tutorial/greet!

(def fut
  (future
    (greet! "Jude")))
;BRK [3 -2] /Users/val/projects/scope-capture/lab/sc/lab/tutorial.clj:183
;saved scope with locals [first-name msg], use sc.api/loose(-...) to resume execution.
=> #'sc.lab.tutorial/fut

fut

;; continue execution normally
(sc.api/loose 3)

;; continue execution by replacing `msg` with the provided value instead of evaluating it
(sc.api/loose-with 3 "Bonjour, Jude !")

;; continue execution by throwing the given Exception
;; (useful to prevent downstream side-effects or break out of a loop)
(sc.api/loose-with-err 3 (ex-info "Aaaaaarrrrgh" {}))

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
  ;; mind the syntax-quote
  `{:sc/spy-cs-logger-id ::log-spy-cs-with-timbre
    :sc/spy-ep-post-eval-logger log-spy-cs-with-timbre})

(defmacro my-spy
  ([] (sc.api/spy-emit my-spy-opts nil &env &form))
  ([expr] (sc.api/spy-emit my-spy-opts expr &env &form))
  ([opts expr] (sc.api/spy-emit (merge my-spy-opts opts) expr &env &form)))



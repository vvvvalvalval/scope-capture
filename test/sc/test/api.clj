(ns sc.test.api
  (:require [clojure.set]
            [clojure.test :as test :refer :all]
            [sc.api]
            [sc.api.logging])
  (:import (java.util.concurrent ExecutionException)))

(def ^:dynamic cs-logger
  (fn [cs-data]))

(def ^:dynamic pre-eval-logger
  (fn [ep-data]))

(def ^:dynamic post-eval-logger
  (fn [ep-data]))

(sc.api.logging/register-cs-logger ::test-logger
  (fn [cs-data]
    (cs-logger cs-data)))

(def ^:dynamic *my-var* 0)

(deftest spy-example-test
  (let [code `(let [~'a 1
                    ~'b 2]
                (fn my-fun [~'x ~'y ~'m]
                  (let [{:keys [~'u ~'v]} ~'m
                        ~'c (+ 2 ~'x)
                        ~'d *my-var*]
                    (* ~'c ~'b
                      (if (number? ~'x)
                        (sc.api/spy
                          {:sc/spy-cs-logger-id ::test-logger
                           :sc/spy-ep-pre-eval-logger pre-eval-logger
                           :sc/spy-ep-post-eval-logger post-eval-logger
                           :sc/dynamic-vars [*my-var*]}
                          (+ ~'x ~'a
                            (if (number? ~'y)
                              ~'d
                              (throw (ex-info "" {:message "aaaaargh"})))))
                        nil)))))
        a_cs (atom nil)
        f (binding [cs-logger #(reset! a_cs %)]
            (eval code))
        cs-data @a_cs]
    (testing "logged the appropriate compile-time data"
      (is (some? cs-data))
      (is (= (:sc.cs/expr cs-data) '(clojure.core/+ x a (if (clojure.core/number? y) d (throw (clojure.core/ex-info "" {:message "aaaaargh"}))))))
      (is (clojure.set/subset?
            '#{a b c d m my-fun u v x y}
            (set (:sc.cs/local-names cs-data))))
      (is (= (:sc.cs/dynamic-var-names cs-data) '[sc.test.api/*my-var*]))
      (let [csid (:sc.cs/id cs-data)]
        (is (and (integer? csid) (neg? csid)))))

    (testing "regular case"
      (let [a_pre (atom nil)
            a_post (atom nil)
            v (binding [pre-eval-logger #(reset! a_pre %)
                        post-eval-logger #(reset! a_post %)
                        *my-var* 42]
                (f 1 2 {:u 3 :v 4}))
            ep-pre @a_pre
            ep-id (:sc.ep/id ep-pre)
            ep-post @a_post]
        (testing "sc.api/last-ep-id returns the recorded Execution Point id"
          (let [csid (:sc.cs/id cs-data)]
            (is (=
                  (sc.api/last-ep-id)
                  [ep-id csid]))))
        (testing "logged the appropriate runtime pre-eval data"
          (is (and (integer? ep-id) (pos? ep-id)))
          (is (= (:sc.ep/code-site ep-pre) cs-data))
          (is (= (select-keys (:sc.ep/local-bindings ep-pre)
                   '[a b c d m u v x y])
                '{x 1,
                  a 1,
                  u 3,
                  y 2,
                  v 4,
                  m {:u 3, :v 4},
                  c 3,
                  b 2,
                  d 42}))
          (is (= (:sc.ep/dynamic-var-bindings ep-pre)
                '{sc.test.api/*my-var* 42})))
        (testing "logged the appropriate runtime post-eval data"
          (is (=
                (select-keys ep-post
                  [:sc.ep/id :sc.ep/code-site :sc.ep/local-bindings :sc.ep/dynamic-var-bindings])
                (select-keys ep-pre
                  [:sc.ep/id :sc.ep/code-site :sc.ep/local-bindings :sc.ep/dynamic-var-bindings])))
          (is (= (:sc.ep/value ep-post) 44)))
        (testing "the information is later available via ep-info"
          (is (=
                (sc.api/ep-info ep-id)
                ep-post))
          (is (=
                (sc.api/ep-info [ep-id (:sc.cs/id cs-data)])
                ep-post)))
        (testing "letsc recreates the context by binding locals and dynamic vars"
          (is (= (zipmap
                   '[a b c d m u v x y *my-var*]
                   (eval `(sc.api/letsc ~ep-id
                            ~'[a b c d m u v x y sc.test.api/*my-var*])))
                '{a 1, b 2, c 3, d 42, m {:u 3, :v 4}, u 3, v 4, x 1, y 2, *my-var* 42})))
        ))
    (testing "error case"
      (let [a_pre (atom nil)
            a_post (atom nil)
            err (try (binding [pre-eval-logger #(reset! a_pre %)
                               post-eval-logger #(reset! a_post %)
                               *my-var* 48]
                       (f 1 "not a number" {:u 3 :v 4}))
                     (catch Throwable err
                       err))
            ep-pre @a_pre
            ep-id (:sc.ep/id ep-pre)
            ep-post @a_post]
        (testing "logged the appropriate runtime pre-eval data"
          (is (= (:sc.ep/code-site ep-pre) cs-data))
          (is (= (select-keys (:sc.ep/local-bindings ep-pre)
                   '[a b c d m u v x y])
                '{x 1,
                  a 1,
                  u 3,
                  y "not a number",
                  v 4,
                  m {:u 3, :v 4},
                  c 3,
                  b 2,
                  d 48}))
          (is (= (:sc.ep/dynamic-var-bindings ep-pre)
                '{sc.test.api/*my-var* 48})))
        (testing "logged the appropriate runtime post-eval data"
          (is (=
                (select-keys ep-post
                  [:sc.ep/id :sc.ep/code-site :sc.ep/local-bindings :sc.ep/dynamic-var-bindings])
                (select-keys ep-pre
                  [:sc.ep/id :sc.ep/code-site :sc.ep/local-bindings :sc.ep/dynamic-var-bindings])))
          (is (= (:sc.ep/error ep-post) err))
          (is (= (ex-data err) {:message "aaaaargh"})))
        (testing "the information is later available via ep-info"
          (is (=
                (sc.api/ep-info ep-id)
                ep-post)))))
    ))

(deftest brk-example
  (let [code `(fn [~'x ~'thrw]
                (let [~'z (dec ~'x)]
                  (* 10
                    (sc.api/brk
                      {:sc/brk-cs-logger-id ::test-logger
                       :sc/brk-ep-pre-eval-logger pre-eval-logger
                       :sc/brk-ep-post-eval-logger post-eval-logger}
                      (if ~'thrw
                        (throw (ex-info "euaark" {:throw ~'thrw}))
                        (inc ~'z))))))
        a_cs (atom nil)
        f (binding [cs-logger #(reset! a_cs %)]
            (eval code))
        cs-data @a_cs]
    (testing "logged the appropriate compile-time data"
      (is (some? cs-data))
      (is (=
            (:sc.cs/expr cs-data)
            '(if thrw
               (throw (clojure.core/ex-info "euaark" {:throw thrw}))
               (clojure.core/inc z))
            ))
      (is (clojure.set/subset?
            '#{x thrw z}
            (set (:sc.cs/local-names cs-data))))
      (let [csid (:sc.cs/id cs-data)]
        (is (and (integer? csid) (neg? csid)))))
    (testing "loose"
      (testing "regular case"
        (let [p_pre (promise)
              a_post (atom nil)
              fut (future
                    (binding [pre-eval-logger #(deliver p_pre %)
                              post-eval-logger #(reset! a_post %)]
                      (f 1 false)))
              ep-pre (deref p_pre 100 nil)
              ep-id (:sc.ep/id ep-pre)]
          (testing "sc.api/last-ep-id returns the recorded Execution Point id"
            (let [csid (:sc.cs/id cs-data)]
              (is (=
                    (sc.api/last-ep-id)
                    [ep-id csid]))))
          (testing "brk suspended execution"
            (is (not (realized? fut))))
          (testing "logged the appropriate runtime pre-eval data"
            (is (= (:sc.ep/code-site ep-pre) cs-data))
            (is (= (select-keys (:sc.ep/local-bindings ep-pre)
                     '[x thrw z])
                  '{x 1,
                    thrw false,
                    z 0})))
          (testing "sc.api/loose resumes execution normally"
            (sc.api/loose ep-id)
            (let [v (deref fut 100 :timed-out)
                  ep-post @a_post]
              (is (= v 10))
              (testing "... and logged the appropriate runtime post-eval data"
                (is (=
                      (select-keys ep-post
                        [:sc.ep/id :sc.ep/code-site :sc.ep/local-bindings :sc.ep/dynamic-var-bindings])
                      (select-keys ep-pre
                        [:sc.ep/id :sc.ep/code-site :sc.ep/local-bindings :sc.ep/dynamic-var-bindings])))
                (is (= (:sc.ep/value ep-post) 1)))))
          ))
      (testing "error case"
        (let [p_pre (promise)
              a_post (atom nil)
              fut (future
                    (binding [pre-eval-logger #(deliver p_pre %)
                              post-eval-logger #(reset! a_post %)]
                      (f 1 true)))
              ep-pre (deref p_pre 100 nil)
              ep-id (:sc.ep/id ep-pre)]
          (testing "brk suspended execution"
            (is (not (realized? fut))))
          (testing "logged the appropriate runtime pre-eval data"
            (is (= (:sc.ep/code-site ep-pre) cs-data))
            (is (= (select-keys (:sc.ep/local-bindings ep-pre)
                     '[x thrw z])
                  '{x 1,
                    thrw true,
                    z 0})))
          (testing "sc.api/loose resumes execution normally"
            (sc.api/loose ep-id)
            (let [err (try
                        (deref fut 100 :timed-out)
                        (catch ExecutionException ee
                          (.getCause ee)))
                  ep-post @a_post]
              (is (= (ex-data err) {:throw true}))
              (testing "... and logged the appropriate runtime post-eval data"
                (is (=
                      (select-keys ep-post
                        [:sc.ep/id :sc.ep/code-site :sc.ep/local-bindings :sc.ep/dynamic-var-bindings])
                      (select-keys ep-pre
                        [:sc.ep/id :sc.ep/code-site :sc.ep/local-bindings :sc.ep/dynamic-var-bindings])))
                (is (= (:sc.ep/error ep-post) err)))))
          )))
    (testing "loose-with"
      (doseq [thrw #{true false}]
        (let [p_pre (promise)
              a_post (atom nil)
              fut (future
                    (binding [pre-eval-logger #(deliver p_pre %)
                              post-eval-logger #(reset! a_post %)]
                      (f 1 thrw)))
              ep-pre (deref p_pre 100 nil)
              ep-id (:sc.ep/id ep-pre)]
          (testing "sc.api/loose-with supersedes the evaluation of the wrapped expression"
            (sc.api/loose-with ep-id 100)
            (let [v (deref fut 100 :timed-out)
                  ep-post @a_post]
              (is (= v 1000))
              (testing "... and logged the appropriate runtime post-eval data"
                (is (=
                      (select-keys ep-post
                        [:sc.ep/id :sc.ep/code-site :sc.ep/local-bindings :sc.ep/dynamic-var-bindings])
                      (select-keys ep-pre
                        [:sc.ep/id :sc.ep/code-site :sc.ep/local-bindings :sc.ep/dynamic-var-bindings])))
                (is (= (:sc.ep/value ep-post) 100)))))
          )))
    (testing "loose-with-err"
      (doseq [thrw #{true false}]
        (let [p_pre (promise)
              a_post (atom nil)
              fut (future
                    (binding [pre-eval-logger #(deliver p_pre %)
                              post-eval-logger #(reset! a_post %)]
                      (f 1 thrw)))
              ep-pre (deref p_pre 100 nil)
              ep-id (:sc.ep/id ep-pre)
              err (ex-info "" {:message "Abort! abort!"})]
          (testing "sc.api/loose-with-err resumes execution by throwing an error"
            (sc.api/loose-with-err ep-id err)
            (let [e (try
                      (deref fut 100 :timed-out)
                      :no-error
                      (catch ExecutionException e
                        (.getCause e)))]
              (is (identical? err e)))
            (testing "... and logged the appropriate runtime post-eval data"
              (let [ep-post @a_post]
                (is (=
                      (select-keys ep-post
                        [:sc.ep/id :sc.ep/code-site :sc.ep/local-bindings :sc.ep/dynamic-var-bindings])
                      (select-keys ep-pre
                        [:sc.ep/id :sc.ep/code-site :sc.ep/local-bindings :sc.ep/dynamic-var-bindings])))
                (is (= (:sc.ep/error ep-post) err)))))
          ))))
  )

(deftest calling-from--example
  (testing "with spy"
    (let [code
          `(fn f []
             (sc.api/spy
               {:sc/called-from 42

                :sc/spy-cs-logger-id ::test-logger
                :sc/spy-ep-pre-eval-logger pre-eval-logger
                :sc/spy-ep-post-eval-logger post-eval-logger}
               nil))
          f (eval code)
          logged-when-f-called?
          (fn []
            (let [a (atom false)]
              (binding [pre-eval-logger
                        (fn [_] (reset! a true))]
                (f))
              @a))]
      (testing "without calling-from"
        (is (false? (logged-when-f-called?))))
      (testing "calling-from with the right value"
        (is (true?
              (sc.api/calling-from 42
                (logged-when-f-called?))))
        (is (true?
              (sc.api/calling-from (* 2 21)
                (logged-when-f-called?)))))
      (testing "calling-from with the wrong value"
        (is (false?
              (sc.api/calling-from :wrong
                (logged-when-f-called?))))
        (is (false?
              (sc.api/calling-from nil
                (logged-when-f-called?))))))
    (testing "When :sc/called-from is nil, ignored"
      (let [code
            `(fn f []
               (sc.api/spy
                 {:sc/called-from nil

                  :sc/spy-cs-logger-id ::test-logger
                  :sc/spy-ep-pre-eval-logger pre-eval-logger
                  :sc/spy-ep-post-eval-logger post-eval-logger}
                 nil))
            f (eval code)
            logged-when-f-called?
            (fn []
              (let [a (atom false)]
                (binding [pre-eval-logger
                          (fn [_] (reset! a true))]
                  (f))
                @a))]
        (testing "without calling-from"
          (is
            (true?
              (logged-when-f-called?))))
        (testing "with calling-from"
          (are [from]
            (true?
              (logged-when-f-called?))
            nil 42 :hello "foo" false)))))
  (testing "with brk"
    (let [code
          `(fn f []
             (sc.api/brk
               {:sc/called-from 42

                :sc/brk-cs-logger-id ::test-logger
                :sc/brk-ep-pre-eval-logger pre-eval-logger
                :sc/brk-ep-post-eval-logger post-eval-logger}
               nil))
          f (eval code)
          logged-when-f-called?
          (fn []
            (let [p (promise)]
              (binding [pre-eval-logger
                        (fn [_] (deliver p true))]
                (future
                  (f)))
              (deref p 10 false)))]
      (testing "without calling-from"
        (is (false? (logged-when-f-called?))))
      (testing "calling-from with the right value"
        (is (true?
              (sc.api/calling-from 42
                (logged-when-f-called?))))
        (is (true?
              (sc.api/calling-from (* 2 21)
                (logged-when-f-called?)))))
      (testing "calling-from with the wrong value"
        (is (false?
              (sc.api/calling-from :wrong
                (logged-when-f-called?))))
        (is (false?
              (sc.api/calling-from nil
                (logged-when-f-called?))))))
    (testing "When :sc/called-from is nil, ignored"
      (let [code
            `(fn f []
               (sc.api/brk
                 {:sc/called-from nil

                  :sc/brk-cs-logger-id ::test-logger
                  :sc/brk-ep-pre-eval-logger pre-eval-logger
                  :sc/brk-ep-post-eval-logger post-eval-logger}
                 nil))
            f (eval code)
            logged-when-f-called?
            (fn []
              (let [p (promise)]
                (binding [pre-eval-logger
                          (fn [_] (deliver p true))]
                  (future (f)))
                (deref p 10 false)))]
        (testing "without calling-from"
          (is
            (true?
              (logged-when-f-called?))))
        (testing "with calling-from"
          (are [from]
            (true?
              (logged-when-f-called?))
            nil 42 :hello "foo" false))))
    ))


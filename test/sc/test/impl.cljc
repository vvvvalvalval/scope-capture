(ns sc.test.impl
  (:require [clojure.test :as test :refer :all]
            [sc.impl]))

(deftest test-last-ep-id*
  (testing "When no EP saved, throws an Exception"
    (is
      (=
        :sc.api.error-types/no-ep-saved-yet
        (try
          (sc.impl/last-ep-id* {:execution-points {}})
          (catch Throwable err
            (-> err ex-data :sc.api.error/error-type)))))))


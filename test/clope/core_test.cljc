(ns clope.core-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as g #?@(:cljs [:include-macros true])]
            [clojure.test.check.properties :as p #?@(:cljs [:include-macros true])]
            [clope.core :as c]))

(defn bytes->array [bytes]
  #?(:clj (reduce-kv (fn [a i b] (doto ^bytes a (aset i (byte b)))) (byte-array (count bytes)) bytes)
     :cljs (.-buffer (reduce-kv (fn [a i b] (doto a (.setInt8 i b))) (-> (count bytes) (js/ArrayBuffer.) (js/DataView.)) bytes))))

(def ropes
  (->> (g/choose -128 127)
       (g/vector)
       (g/fmap (comp c/wrap bytes->array))
       (g/vector)
       (g/fmap (partial apply c/join))))

(def subr-join-equality
  (p/for-all [[r s] (g/let [r ropes
                            i (g/choose 0 (c/size r))]
                      [r (c/join (c/subr r 0 i) (c/subr r i))])]
    (and (= (c/size r) (c/size s))
         (= (hash r) (hash s))
         (= r s))))

(deftest main
  (is (:result (tc/quick-check 1000 subr-join-equality))))
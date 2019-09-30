(ns clope.core
  (:require [clope.impl :as i]))

(defn wrap
  "Wraps given byte array into a rope."
  [bytes] (when (some? bytes) (i/wrap bytes)))

(defn join
  "Returns a concatenation of given ropes."
  ([])
  ([r] r)
  ([l r] (if (nil? l) r (if (nil? r) l (i/join l r))))
  ([l r & s] (reduce join (join l r) s)))

(defn size
  "Returns the size of given `rope`, in bytes."
  ^long [rope] (if (nil? rope) 0 (i/size rope)))

(defn subr
  "Returns a subrope of given `rope` with bytes from `start` (inclusive, defaults to 0) to `end` (exclusive, defaults to `(size rope)`)."
  ([])
  ([rope] rope)
  ([rope ^long start] (subr rope start (size rope)))
  ([rope ^long start ^long end]
   (assert (<= 0 start end (size rope)) "subrope out of bounds")
   (when-not (== start end) (i/subr rope start end))))
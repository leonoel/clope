(ns ^:no-doc clope.impl
  (:import (clope.impl Rope) (java.io Writer)))

(defmethod print-method Rope [^Rope r ^Writer w]
  (.write w "#rope")
  (print-method {:hash (.hashCode r) :size (.size r)} w))

(defn wrap [^bytes bytes]
  (when (pos? (alength bytes)) (Rope/wrap bytes)))

(defn join [^Rope l ^Rope r]
  (Rope/join l r))

(defn size [^Rope r]
  (.size r))

(defn subr [^Rope r ^long s ^long e]
  (.subr r s e))
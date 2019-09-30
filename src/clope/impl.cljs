(ns ^:no-doc clope.impl)

(declare join)

(defn hash-dv ^number [^number hash dv]
  (loop [h hash
         i (.-byteOffset dv)]
    (if (< i (.-byteLength dv))
      (recur (-> h (imul 31) (+ (.getInt8 dv i))) (inc i)) h)))

(defprotocol Rope
  (-size [_])
  (-subr [_ ^number start ^number end])
  (-append [_ ^Rope rope])
  (-prepend [_ ^Rope rope])
  (-populate [_ ^array arrays ^number index]))

(defn print-rope [rope writer opts]
  (-write writer "#rope")
  (pr-writer {:hash (-hash rope) :size (-size rope)} writer opts))

(deftype Join [^number size ^Rope left ^Rope right ^:mutable hash]
  Rope
  (-size [_] size)
  (-subr [_ ^number start ^number end]
    (if (== (- end start) size)
      _ (let [startk (- start (-size left))
              endk (- end (-size left))]
          (if (neg? startk)
            (if (pos? endk)
              (join (-subr left start (-size left))
                    (-subr right 0 endk))
              (-subr left start end))
            (-subr right startk endk)))))
  (-append [_ ^Rope rope]
    (if (< (-size rope) (-size left))
      (join left (join right rope))
      (Join. (+ size (-size rope)) _ rope nil)))
  (-prepend [_ ^Rope rope]
    (if (< (-size rope) (-size right))
      (join (join rope left) right)
      (Join. (+ (-size rope) size) rope _ nil)))
  (-populate [_ ^array arrays ^number index]
    (->> index
         (-populate left arrays)
         (-populate right arrays)))
  IHash
  (-hash [_]
    (if-some [h hash]
      h (let [it (iter right)]
          (loop [h (-hash left)]
            (if (.hasNext it)
              (recur (hash-dv h (js/DataView. (.next it))))
              (set! hash h))))))
  IEquiv
  (-equiv [_ o]
    (and (some? o)
         (satisfies? Rope o)
         (== size (-size o))
         (= left (-subr o 0 (-size left)))
         (= right (-subr o (-size left) size))))
  ICounted
  (-count [_] (+ (-count left) (-count right)))
  ISeqable
  (-seq [_]
    (let [arrays (object-array (-count _))]
      (-populate _ arrays 0)
      (->IndexedSeq arrays 0 nil)))
  IIterable
  (-iterator [_]
    (let [arrays (object-array (-count _))]
      (-populate _ arrays 0)
      (->IndexedSeqIterator arrays 0)))
  IReduce
  (-reduce [_ f]
    (let [arrays (object-array (-count _))]
      (-populate _ arrays 0)
      (array-reduce arrays f)))
  (-reduce [_ f i]
    (let [arrays (object-array (-count _))]
      (-populate _ arrays 0)
      (array-reduce arrays f i)))
  IPrintWithWriter
  (-pr-writer [_ writer opts]
    (print-rope _ writer opts)))

(deftype Wrap [ab ^:mutable hash]
  Rope
  (-size [_] (.-byteLength ab))
  (-subr [_ ^number start ^number end]
    (if (== (- end start) (.-byteLength ab))
      _ (Wrap. (.slice ab start end) nil)))
  (-append [_ ^Rope rope]
    (->Join (+ (-size _) (-size rope)) _ rope nil))
  (-prepend [_ ^Rope rope]
    (->Join (+ (-size rope) (-size _)) rope _ nil))
  (-populate [_ ^array arrays ^number index]
    (aset arrays index ab)
    (inc index))
  IHash
  (-hash [_]
    (if-some [h hash]
      h (set! hash (hash-dv 0 (js/DataView. ab)))))
  IEquiv
  (-equiv [_ o]
    (and (some? o)
         (satisfies? Rope o)
         (== (.-byteLength ab) (-size o))
         (let [it (iter o)
               dv (js/DataView. ab)]
           (loop [i 0]
             (if (< i (.-byteLength ab))
               (let [oab (.next it)
                     odv (js/DataView. oab)]
                 (if (loop [j 0]
                       (if (< j (.-byteLength oab))
                         (if (== (.getInt8 odv j) (.getInt8 dv (+ i j)))
                           (recur (inc j)) false) true))
                   (recur (+ i (.-byteLength oab))) false)) true)))))
  ICounted
  (-count [_] 1)
  ISeqable
  (-seq [_] (list ab))
  IIterable
  (-iterator [_] (iter (list ab)))
  IReduce
  (-reduce [_ _] ab)
  (-reduce [_ f i]
    (let [r (f i ab)]
      (if (reduced? r) @r r)))
  IPrintWithWriter
  (-pr-writer [_ writer opts]
    (print-rope _ writer opts)))

(defn wrap [ab]
  (when (pos? (.-byteLength ab)) (->Wrap ab nil)))

(defn join [^Rope l ^Rope r]
  (if (> (-size l) (-size r))
    (-append l r) (-prepend r l)))

(def size -size)

(def subr -subr)
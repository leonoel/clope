# clope

Byte ropes for clojure and clojurescript.

[![clojars](https://img.shields.io/clojars/v/clope.svg)](https://clojars.org/clope)
[![cljdoc](https://cljdoc.org/badge/clope/clope)](https://cljdoc.org/d/clope/clope/CURRENT)
[![build](https://travis-ci.org/leonoel/clope.svg?branch=master)](https://travis-ci.org/leonoel/clope)
[![license](https://img.shields.io/github/license/leonoel/clope.svg)](LICENSE)


## Maturity

Stable.


## Rationale

Ropes are immutable data structures holding sequences of bytes, represented as binary trees instead of contiguous memory arrays. This provides sub-linear algorithmic complexity for concatenation and slicing, and relaxes allocation-related constraints.


## Documentation

[`clope.core`](https://cljdoc.org/d/clope/clope/CURRENT/api/clope.core)


### Overview

```clojure
(require '[clope.core :as c])
```

`wrap` turns a byte array into a rope.
```clojure
(c/wrap (.getBytes "clojure"))
#_=> #rope{:hash 866284260, :size 7}
```

`size` returns the number of bytes in a rope.
```clojure
(def rope (c/wrap (.getBytes "clojure")))
(c/size rope)
#_=> 7
```

`join` returns the concatenation of an arbitrary number of ropes.
```clojure
(c/join (c/wrap (.getBytes "Hello "))
        (c/wrap (.getBytes "World !")))
#_=> #rope{:hash 22678917, :size 13}
```

`subr` returns a subrope of an arbitrary rope, with bytes in the given range.
```clojure
(c/subr (c/wrap (.getBytes "clojure")) 1 4)
#_=> #rope{:hash 107335, :size 3}
```

Ropes are collections of their underlying byte arrays, they are counted, iterable, seqable and reducible.
```clojure
(def rope (c/join (c/wrap (.getBytes "Hello "))
                  (c/wrap (.getBytes "World !"))))

(count rope)
#_=> 2

(map alength rope)
#_=> (6 7)

(import java.nio.ByteBuffer)
(import java.nio.charset.Charset)
(defn bb-put [^ByteBuffer buffer ^bytes array] (.put buffer array))
(->> rope
     (reduce bb-put (ByteBuffer/allocate (c/size rope)))
     (.flip)
     (.decode (Charset/defaultCharset))
     (.toString))
#_=> "Hello World !"
```

`nil` is the empty rope, it's safe to pass it where a rope is expected. non-`nil` implies non-empty.
```clojure
(c/size nil)
#_=> 0
```

Ropes implement proper hashing and equality semantics, based on actual byte content.
```clojure
(= (c/wrap (.getBytes "clojure"))
   (c/join (c/wrap (.getBytes "clo"))
           (c/wrap (.getBytes "jure"))))
#_=> true
```


### Caveats

For performance reasons, the rope implementation assumes to take full ownership of the arrays it wraps and doesn't perform any defensive copies. The immutability contract of ropes holds by the following conditions :
* don't write to an array after it's been `wrap`ped in a rope.
* treat arrays exposed by rope traversal as read-only.
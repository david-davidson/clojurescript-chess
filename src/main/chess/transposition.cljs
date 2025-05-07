(ns chess.transposition
    (:require [chess.board :refer [lookup]]
              [chess.pieces :refer [vacant]]))

(def entry-types
  {:exact "EXACT"
   :upper "UPPER"
   :lower "LOWER"})

(def zobrist-keys
  (let [piece-types ["pawn" "knight" "bishop" "rook" "queen" "king"]
        colors ["white" "black"]
        squares (for [row (range 8)
                     col (range 8)]
                 [row col])
        next-num (let [a (atom 1)]
                  #(swap! a (fn [x] (mod (* x 48271) 2147483647))))]
    (into {}
          (for [piece piece-types
                color colors
                square squares]
            {[piece color square] (next-num)}))))

(defn zobrist-hash [board]
  (reduce
    (fn [hash [row col]]
      (let [piece (lookup board [row col])]
        (if (= piece vacant)
          hash
          (bit-xor hash
                   (get zobrist-keys
                        [(:type piece)
                         (:color piece)
                         [row col]]
                        0)))))
    0
    (for [row (range 8)
          col (range 8)]
      [row col])))

(defn ensure-vector-move [move]
  (when (and (sequential? move)
             (= 2 (count move))
             (first move)
             (second move))
    [(vec (first move))
     (vec (second move))]))

(defn make-tt-entry [score depth type best-move]
  {:score score
   :depth depth
   :type type
   :best-move (ensure-vector-move best-move)})

(def tt-size 1048576)

(defn create-transposition-table []
  {})

(defn tt-store [table hash entry]
  (if (< (count table) tt-size)
    (assoc table hash entry)
    table))

(defn tt-lookup [table hash]
  (when-let [{:keys [best-move] :as entry} (get table hash)]
    (cond-> entry
      best-move (assoc :best-move (ensure-vector-move best-move)))))

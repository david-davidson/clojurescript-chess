(ns chess.board
    (:require [clojure.string :as string]
              [chess.utils :refer [reduce-indexed]]
              [chess.pieces :as pieces :refer [bishop
                                               pawn
                                               b
                                               w
                                               vacant
                                               is-vacant?
                                               pieces-by-type
                                               get-position-weighting]]))

(defn get-initial-board [] [
    [(b "rook") (b "knight") (b "bishop") (b "queen") (b "king") (b "bishop") (b "knight") (b "rook")]
    [(b "pawn") (b "pawn") (b "pawn") (b "pawn") (b "pawn") (b "pawn") (b "pawn") (b "pawn")]
    [(vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant)]
    [(vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant)]
    [(vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant)]
    [(vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant)]
    [(w "pawn") (w "pawn") (w "pawn") (w "pawn") (w "pawn") (w "pawn") (w "pawn") (w "pawn")]
    [(w "rook") (w "knight") (w "bishop") (w "queen") (w "king") (w "bishop") (w "knight") (w "rook")]
])

(defn lookup [board position] (get-in board position))

(defn move-piece [board from to]
    (let [piece (lookup board from)]
        (-> board
            (assoc-in from (vacant))
            (assoc-in to (assoc piece :move-count (inc (get piece :move-count)))))))

(defn evaluate-board [board]
    (reduce-indexed
        (fn [row-idx total row]
            (reduce-indexed
                (fn [col-idx total piece]
                    (let [color (get piece :color)
                          type (get piece :type)
                          position-weighting (if (is-vacant? piece)
                                                 0
                                                 (get-position-weighting color type [row-idx col-idx]))]
                    (+ total (get piece :weight 0) position-weighting)))
                total
                row))
        0
        board))

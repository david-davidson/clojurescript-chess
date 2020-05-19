(ns chess.board
    (:require [clojure.string :as string]
              [chess.pieces :as pieces :refer [bishop pawn b w vacant is-vacant? pieces-by-type]]))

(defn get-initial-board [] [
    [(b :rook) (b :knight) (b :bishop) (b :queen) (b :king) (b :bishop) (b :knight) (b :rook)]
    [(b :pawn) (b :pawn) (b :pawn) (b :pawn) (b :pawn) (b :pawn) (b :pawn) (b :pawn)]
    [(vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant)]
    [(vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant)]
    [(vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant)]
    [(vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant) (vacant)]
    [(w :pawn) (w :pawn) (w :pawn) (w :pawn) (w :pawn) (w :pawn) (w :pawn) (w :pawn)]
    [(w :rook) (w :knight) (w :bishop) (w :queen) (w :king) (w :bishop) (w :knight) (w :rook)]
])

(def get-file-offset (partial string/index-of "abcdefgh"))

(def get-rank-offset (comp #(- 8 %) int))

(defn to-coordinates [[file rank]]
    [(get-rank-offset rank) (get-file-offset file)])

(defn lookup-coords
    "Takes position in matrix format: [0, 0], etc"
    [board position]
        (get-in board position))

(defn lookup
    "Takes position in rank-and-file format: a1, etc"
    [board position]
        (lookup-coords board (to-coordinates position)))

(defn move-piece [board from to]
    (let [piece (lookup-coords board from)]
        (-> board
            (assoc-in from (vacant))
            (assoc-in to (update-in piece [:move-count] inc)))))

(defn score-material [board]
    (reduce
        (fn [total row]
            (reduce
                (fn [total piece]
                    (+ total (get piece :weight 0)))
                total
                row))
        0
        board))

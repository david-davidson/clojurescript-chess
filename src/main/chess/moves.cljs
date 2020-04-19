(ns chess.moves
    (:require [chess.board :refer [lookup-coords]]
              [chess.utils :refer [flatten-once]]
              [chess.pieces :refer [is-vacant? pieces-by-type]]))

(defn is-coord-element-valid? [coord] (and (>= coord 0) (< coord 8)))
(defn is-coord-valid? [coord] (every? is-coord-element-valid? coord))

(defn get-moves-seq
    "Builds a sequence of moves"
    [transform-position initial-position limit]
        (let [moves-seq (iterate transform-position (transform-position initial-position))]
            (if limit
                (take limit moves-seq)
                moves-seq)))

(defn get-moves-seq-from-position
    "Builds a given piece's sequence of moves"
    [piece initial-position]
        (let [moves (get piece :moves)
              limit (get piece :limit)]
            (map #(get-moves-seq % initial-position limit) moves)))

(defn get-moves-in-seq
    "Follows a sequence of moves until it ends, whether by running out, exiting the board, or encountering other pieces.
    Returns all possible moves through that point."
    ([board color moves-seq] (get-moves-in-seq board color moves-seq []))
    ([board color moves-seq visited-moves]
        (let [next-move (first moves-seq)]
            (if-not (and next-move (is-coord-valid? next-move))
                visited-moves
                (let [next-piece (lookup-coords board next-move)]
                    (cond (= color (get next-piece :color)) visited-moves
                          (is-vacant? next-piece) (get-moves-in-seq board color (rest moves-seq) (conj visited-moves next-move))
                          :else (conj visited-moves next-move)))))))

(defn get-moves-from-position
    "Returns all moves available from a given position on the board"
    [board position]
        (let [piece (lookup-coords board position)
              piece-data (get pieces-by-type (get piece :type))
              move-seqs (get-moves-seq-from-position piece-data position)]
            (->> move-seqs
                 (map #(get-moves-in-seq board (get piece :color) %))
                 (filter not-empty)
                 (flatten-once))))

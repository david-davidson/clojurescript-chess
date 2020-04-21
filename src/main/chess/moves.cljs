(ns chess.moves
    (:require [chess.board :refer [lookup-coords]]
              [chess.utils :refer [flatten-once reverse-color]]
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
    "Builds a given piece's sequence of possible moves, without filtering/constraints"
    [transformations color initial-position limit]
        (map #(get-moves-seq (partial % color) initial-position limit) transformations))

(defn get-valid-moves [board position piece move-config]
    "Builds a list of valid moves for a given piece"
    (let [color (get piece :color)
          get-limit (get move-config :get-limit)
          limit (when get-limit (get-limit piece))
          all-move-seqs (get-moves-seq-from-position (get move-config :transformations) color position limit)]
        (defn traverse-moves-seq
            ([moves-to-visit] (traverse-moves-seq moves-to-visit []))
            ([moves-to-visit visited-moves]
                (let [next-move (first moves-to-visit)
                      next-piece (lookup-coords board next-move)
                      is-next-piece-opposite-color (= (reverse-color color) (get next-piece :color))]
                    (cond (not (is-coord-valid? next-move))
                            visited-moves
                          (and (get move-config :can-capture true) is-next-piece-opposite-color)
                            (conj visited-moves next-move)
                          (and (get move-config :can-advance true) (is-vacant? next-piece))
                            (traverse-moves-seq (rest moves-to-visit) (conj visited-moves next-move))
                          :else
                            visited-moves))))
        (->> all-move-seqs
            (map traverse-moves-seq)
            (flatten-once))))


(defn get-moves-from-position
    "Returns all moves available from a given position on the board"
    [board position]
        (let [piece (lookup-coords board position)
              piece-data (get pieces-by-type (get piece :type))
              moves (get piece-data :moves)]
            (->> moves
                (map #(get-valid-moves board position piece %))
                (flatten-once))))

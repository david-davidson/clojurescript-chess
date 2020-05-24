(ns chess.moves
    (:require [chess.board :refer [lookup-coords move-piece]]
              [chess.utils :refer [flatten-once reverse-color]]
              [chess.pieces :refer [is-vacant? pieces-by-type]]))

(declare is-check?)

(defn is-coord-element-valid? [coord] (and (>= coord 0) (< coord 8)))
(defn is-coord-valid? [coord] (every? is-coord-element-valid? coord))

(defn get-coords-for-color [board color]
    (->> (for [x (range 8) y (range 8)] [x y])
         (filter (fn [position]
            (let [piece (lookup-coords board position)]
                (= color (get piece :color)))))))

(declare get-moves-from-position) ; We need to declare this above `get-all-moves-for-color`, but can't _define_ it yet

(defn get-all-moves-for-color [board color]
    (->> (get-coords-for-color board color)
         (map #(get-moves-from-position board % false))
         (flatten-once)))

(defn get-moves-seq
    "Builds a sequence of moves, without filtering/constraints"
    [transform-position initial-position limit]
        (let [moves-seq (iterate transform-position (transform-position initial-position))]
            (if limit
                (take limit moves-seq)
                moves-seq)))

(defn get-moves-seq-from-position
    "Builds a given piece's sequence of possible moves, without filtering/constraints"
    [transformations color initial-position limit]
        (map #(get-moves-seq (partial % color) initial-position limit) transformations))

(defn is-move-unsafe [board color from-position to-position]
    (let [next-board (move-piece board from-position to-position)]
        (not (is-check? next-board color))))

(defn filter-unsafe-moves [board color from-position should-filter moves]
    "Filters `moves` such that no move enters 'danger': a position where it could be taken as a result of the move.
    Mostly just for preventing kings moving into check."
    (if should-filter
        (filter (partial is-move-unsafe board color from-position) moves)
        moves))

(defn get-valid-moves [board from-position piece move-config]
    "Builds a seq of valid moves for a given piece"
    (let [color (get piece :color)
          get-limit (get move-config :get-limit)
          limit (when get-limit (get-limit piece))
          all-move-seqs (get-moves-seq-from-position (get move-config :transformations) color from-position limit)]
        (loop [move-seqs all-move-seqs
               moves-to-visit (first move-seqs)
               visited-moves (transient [])]
            (if-let [moves-to-visit (if (nil? moves-to-visit) (first move-seqs) moves-to-visit)]
                (if-let [next-move (first moves-to-visit)]
                    (let [piece (lookup-coords board from-position)
                        next-piece (lookup-coords board next-move)
                        is-next-piece-opposite-color (= (reverse-color (get piece :color)) (get next-piece :color))]
                        (cond (not (is-coord-valid? next-move))
                                (recur (rest move-seqs) nil visited-moves)
                            (and (get move-config :can-capture true) is-next-piece-opposite-color)
                                (recur (rest move-seqs) nil (conj! visited-moves next-move))
                            (and (get move-config :can-advance true) (is-vacant? next-piece))
                                (recur move-seqs (rest moves-to-visit) (conj! visited-moves next-move))
                            :else
                                (recur (rest move-seqs) nil visited-moves)))
                    (recur (rest move-seqs) nil visited-moves))
                (persistent! visited-moves)))))

(defn get-moves-from-position
    "Returns all moves available from a given position on the board"
    ([board position] (get-moves-from-position board position true))
    ([board position filter-unsafe]
        (let [piece (lookup-coords board position)
              piece-data (get pieces-by-type (get piece :type))]
            (->> (get piece-data :moves)
                 (map (partial get-valid-moves board position piece))
                 (flatten-once)
                 ((partial filter-unsafe-moves board (get piece :color) position filter-unsafe))))))

(defn get-moves-for-color [board color filter-unsafe]
    (->> (get-coords-for-color board color)
         (map (fn [from-coords]
            (->> (get-moves-from-position board from-coords filter-unsafe)
                 (map (fn [to-coords] [from-coords to-coords])))))
         flatten-once))

(defn permits-king-capture? [board color to-coords]
    (let [piece (lookup-coords board to-coords)]
        (and (= "king" (get piece :type)) (= color (get piece :color)))))

(defn is-check? [board color]
    (->> (get-all-moves-for-color board (reverse-color color))
         (some (partial permits-king-capture? board color))))

(defn is-checkmate? [board color]
    (and (is-check? board color)
         (->> (get-moves-for-color board color true)
              (count)
              (= 0))))

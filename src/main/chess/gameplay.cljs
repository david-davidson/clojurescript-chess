(ns chess.gameplay
    (:require [chess.utils :refer [reverse-color reduce-with-early-exit]]
              [chess.moves :refer [get-moves-for-color]]
              [chess.board :refer [move-piece score-board]]))

(declare evaluate-board)

(defn get-comparator [color] (if (= color :white) > <))

(defn get-initial-score [color] (if (= color :white) -1000 1000))

(def root-depth 1)
(defn is-root [depth] (= depth root-depth))

(defn get-best-score-reducer [board color target-depth depth]
    (fn [{:keys [score move tiebreaker alpha beta] :as total}
         next-move
         next-step]
        (if (> alpha beta)
            total
            (let [[from-coords to-coords] next-move
                  next-board (move-piece board from-coords to-coords)
                  next-score (if (>= depth target-depth)
                                 (score-board next-board)
                                 (evaluate-board next-board
                                                 (reverse-color color)
                                                 target-depth
                                                 (inc depth)
                                                 alpha
                                                 beta))
                  next-tiebreaker (when (is-root depth) (count (get-moves-for-color next-board color)))
                  should-update-score (if (= next-score score)
                                          (> next-tiebreaker tiebreaker)
                                          ((get-comparator color) next-score score))
                  best-score (if should-update-score next-score score)]
                (next-step {:score best-score
                            :move (if should-update-score next-move move)
                            :tiebreaker (if should-update-score next-tiebreaker tiebreaker)
                            :alpha (if (= color :white) (max alpha best-score) alpha)
                            :beta (if (= color :black) (min beta best-score) beta)})))))

(defn evaluate-board
    ([board color target-depth] (evaluate-board board color target-depth root-depth -1000 1000))
    ([board color target-depth depth alpha beta]
    (->> (get-moves-for-color board color)
         (reduce-with-early-exit
            (get-best-score-reducer board color target-depth depth)
            {:score (get-initial-score color)
             :move nil
             :tiebreaker nil
             :alpha alpha
             :beta beta})
         ((if (is-root depth) :move :score)))))

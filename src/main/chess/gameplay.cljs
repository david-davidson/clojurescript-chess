(ns chess.gameplay
    (:require [chess.utils :refer [reverse-color index-of]]
              [chess.moves :refer [get-moves-for-color]]
              [chess.board :refer [move-piece score-board]]))

(defn get-comparator [color] (if (= color :white) max min))

(defn pick-best-score [color list]
    (-> (get-comparator color)
        (apply list)))

(defn pick-move-for-best-score [board color list]
    (as-> (pick-best-score color list) $
          (index-of $ list)
          (nth (get-moves-for-color board color) $)))

(defn get-next-move
    ([board color depth] (get-next-move board color depth 1))
    ([board color depth counter]
        (->> (get-moves-for-color board color)
             (map (fn [[from-coords to-coords]]
                (if (>= counter depth)
                    (score-board (move-piece board from-coords to-coords))
                    (get-next-move (move-piece board from-coords to-coords)
                                   (reverse-color color)
                                   depth
                                   (inc counter)))))
             ((if (= counter 1)
                (partial pick-move-for-best-score board color)
                (partial pick-best-score color))))))

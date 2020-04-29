(ns chess.gameplay
    (:require [chess.utils :refer [reverse-color log]]
              [chess.moves :refer [get-moves-for-color]]
              [chess.board :refer [move-piece score-board]]))

(defn get-asc-or-desc [color]
    (fn [first second]
        (if (= color :black)
            (compare first second) ; For black, low (negative) scores ar "best": asc
            (compare second first))))

(defn get-best-comparator [color] (if (= color :black) < >))

(defn reduce-subtree-to-score [color tree]
    (->> tree
         (map (fn [[key val]]
            (if (number? val)
                val
                (reduce-subtree-to-score (reverse-color color) val))))
         (reduce (fn [total, current]
            (if ((get-best-comparator color) current total) current total)))))

(defn get-best-move [color tree]
    "For 'best', we implement the minimax algorithm: walk the game tree, picking the optimal move for
    the current player at every level in the tree"
    (->> tree
        (map (fn [[key val]] (assoc {} key (reduce-subtree-to-score (reverse-color color) val))))
        (sort-by #(->> % vals first) (get-asc-or-desc color))
        log
        first ; Pick best of the worst-case scenarios
        keys
        first))

(defn build-game-tree
    ([board color depth] (build-game-tree board color depth 1))
    ([board color depth counter]
    (->> (get-moves-for-color board color)
         (reduce (fn [total [from-coords all-to-coords]]
            (reduce
                (fn [total to-coords]
                    (assoc total [from-coords to-coords]
                        (if (>= counter depth)
                            (score-board (move-piece board from-coords to-coords))
                            (build-game-tree (move-piece board from-coords to-coords)
                                             (reverse-color color)
                                             depth
                                             (inc counter)))))
                total
                all-to-coords)
            ) {}))))

(defn get-next-move [board color depth]
    (->> (build-game-tree board color depth)
         (get-best-move color)))

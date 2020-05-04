(ns chess.gameplay
    (:require [chess.utils :refer [reverse-color index-of]]
              [chess.moves :refer [get-moves-for-color]]
              [chess.board :refer [move-piece score-board]]))

(defn get-comparator [color] (if (= color :white) max min))

(defn get-initial-score [color] (if (= color :white) -1000 1000))

(defn get-best-score
    "This helper implements the minimax algorithm with alpha-beta pruning:
        * Minimax walks the game tree, selecting the optimal move depending on the level in the tree (i.e., if
            evaluating moves available to white, we'll pick high scores; if black, low scores)
        * Alpha-beta pruning tracks the highest (alpha) and lowest (beta) scores seen in a given subtree, allowing us to
            skip evaluation of entire subtrees altogether. Say the first move's subtree evaluates to -10 (advantage 10
            black); then, say that while evaluating the next subtree, among white's available options, we find -9. Since
            white picks the highest score available, we know the second subtree can't go lower than -9 -- which means
            the first subtree will always be preferable (for black), and we can skip the rest of the second."
    ([board color target-depth] (get-best-score board color target-depth 1 -1000 1000))
    ([board color target-depth depth initial-alpha initial-beta]
        (loop [child-moves (get-moves-for-color board color)
               best-score-so-far (get-initial-score color)
               idx 0
               idx-best-score-so-far 0
               alpha initial-alpha
               beta initial-beta]
            (let [next-move (first child-moves)]
                (if (or (> alpha beta) (nil? next-move))
                    {:score best-score-so-far
                     :move-idx idx-best-score-so-far}
                    (let [[from-coords to-coords] next-move
                           next-score (if (>= depth target-depth)
                                          (score-board (move-piece board from-coords to-coords))
                                          (-> (get-best-score (move-piece board from-coords to-coords)
                                                              (reverse-color color)
                                                              target-depth
                                                              (inc depth)
                                                              alpha
                                                              beta)
                                              :score
                                              (/ depth)))
                           best-score ((get-comparator color) next-score best-score-so-far)]
                        (recur (rest child-moves)
                               best-score
                               (inc idx)
                               (if (= next-score best-score) idx idx-best-score-so-far)
                               (if (= color :white) (max alpha best-score) alpha)
                               (if (= color :black) (min beta best-score) beta))))))))

(defn get-next-move [board color depth]
    (->> (get-best-score board color depth)
         :move-idx
         (nth (get-moves-for-color board color))))

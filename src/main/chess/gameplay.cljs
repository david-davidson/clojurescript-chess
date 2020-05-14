(ns chess.gameplay
    (:require [chess.utils :refer [reverse-color reduce-with-early-exit indexes-by-item]]
              [chess.moves :refer [get-moves-for-color get-all-moves-for-color]]
              [chess.board :refer [move-piece evaluate-board]]))

(declare evaluate-game-tree)

(defn get-comparator [color] (if (= color "white") > <))

(defn get-default-score-if-no-subtree [color]
    "Handles cases when a board is not yet at terminal depth, but has no moves available. For now,
    we need this to seek checkmate."
    (if (= color "white") js/Infinity (- js/Infinity)))

(defn select-cache [item] (get item :cache))

(defn select-sorted-moves [color item]
    (->> (get item :visited-moves)
         (map :move)))

(defn select-best-move [color item]
    (-> (get item :visited-moves)
        first
        :move))

(defn select-best-subtree-score [color item]
    (-> (get item :visited-moves)
        first
        (get :score (get-default-score-if-no-subtree color))))

(defn sort-visited-moves [color item]
    (let [sorted-moves (->> (get item :visited-moves)
                            (sort-by :score (get-comparator color)))]
        (assoc item :visited-moves sorted-moves)))

(defn update-cache [board color cache moves]
    (let [subcache (get cache color)
          next-subcache (assoc subcache board moves)]
        (assoc cache color next-subcache)))

(defn cache-visited-moves [board color results]
    (let [next-cache (update-cache board
                                   color
                                   (select-cache results)
                                   (select-sorted-moves color results))]
        (assoc results :cache next-cache)))

(defn get-available-moves [board color cache]
    "Gets all moves available from a given board. If we've already visited the board, we check the
    cache for the best order to visit children in. Note that we can't return the cache hit as-is
    (and instead have to use it as a sort helper) because it's possible the previous iteration
    alpha/beta pruned certain children that shouldn't be pruned in the current iteration."
    (let [subcache (get cache color)
          child-moves (get-moves-for-color board color)
          cached-moves (get subcache board)]
        (if cached-moves
            (if (= (count cached-moves) (count child-moves))
                cached-moves ; If counts match, we know no children have been pruned, so can return cache as-is
                (let [indexes-by-move (indexes-by-item cached-moves)]
                    (sort-by #(get indexes-by-move % js/Infinity) child-moves)))
            child-moves)))

(defn get-minimax-reducer [board color depth]
    "Minimax helper with an alpha/beta condition for early exit: `next-step` to continue iteration,
    bare return value to bail early."
    (fn [{:keys [alpha beta visited-moves cache] :as accum}
         next-move
         next-step]
        (if (> alpha beta)
            accum ; Bail early
            (let [[from-coords to-coords] next-move
                  next-board (move-piece board from-coords to-coords)
                  next-game-tree (when (> depth 1)
                                       (evaluate-game-tree next-board
                                                           (reverse-color color)
                                                           cache
                                                           (dec depth)
                                                           alpha
                                                           beta))
                  next-score (if (= depth 1)
                                 (evaluate-board next-board)
                                 (select-best-subtree-score color next-game-tree))]
                (next-step {:visited-moves (conj visited-moves {:move next-move :score next-score})
                            :cache (get next-game-tree :cache cache)
                            :alpha (if (= color "white") (max alpha next-score) alpha)
                            :beta (if (= color "black") (min beta next-score) beta)})))))

(defn evaluate-game-tree
    "Evaluation implements the minimax algorithm, which selects moves under the assumption that both
    players will make optimal choices. This assumption enables alpha/beta pruning, where if a score
    is visited that means the current subtree will never be chosen, we can skip evaluating all other
    boards in that subtreee. `alpha` and `beta` track the highest/lowest scores in a given subtree."
    ([board color cache depth] (evaluate-game-tree board color cache depth (- js/Infinity) js/Infinity))
    ([board color cache depth alpha beta]
        (->> (get-available-moves board color cache)
             (reduce-with-early-exit
                (get-minimax-reducer board color depth)
                {:visited-moves []
                 :cache cache
                 :alpha alpha
                 :beta beta})
             (sort-visited-moves color)
             (cache-visited-moves board color))))

(defn get-next-move [board color depth]
    "We implement an iterative-deepening approach: for 3-ply search, we first search to depth 1,
    then 2, then 3. As we go, for every move with _child_ moves, we store those moves in order of
    subtree score (in `cache`). On the final search--which consumes almost all the search time--
    we'll be more likely to check optimal moves first, and thus more likely to hit an alpha/beta
    early exit."
    (loop [cache {"white" {} "black" {}}
           iterative-depth 1]
        (let [game-tree (evaluate-game-tree board color cache iterative-depth)]
            (if (= iterative-depth depth)
                (select-best-move color game-tree)
                (recur (select-cache game-tree)
                       (inc iterative-depth))))))

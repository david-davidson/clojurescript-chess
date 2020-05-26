(ns chess.gameplay
    (:require [chess.utils :refer [reverse-color reduce-with-early-exit indexes-by-item]]
              [chess.moves :refer [is-check? is-checkmate? captures-king? get-moves-for-color]]
              [chess.board :refer [move-piece evaluate-board]]))

(declare evaluate-game-tree)

(defn get-comparator [color] (if (= color "white") > <))

(defn get-score-for-checkmate [color] (if (= color "white") js/Infinity (- js/Infinity)))

(defn select-cache [results] (get results :cache))

(defn select-sorted-moves [color results]
    (->> (get results :visited-moves)
         (map :move)))

(def select-best-move (comp first select-sorted-moves))

(defn select-best-subtree-score [color results board]
    "If no best subtree exists (no child moves are available), looks for checkmate--if no checkmate,
    scores board as stalemate on the basis of material alone."
    (let [best-child (first (get results :visited-moves))]
        (if best-child
            (get best-child :score)
            (if (is-checkmate? board (reverse-color color))
                (get-score-for-checkmate color)
                (evaluate-board board)))))

(defn sort-visited-moves [color results]
    (let [sorted-moves (->> (get results :visited-moves)
                            (filter #(not (get % :invalid)))
                            (sort-by :score (get-comparator color)))]
        (assoc results :visited-moves sorted-moves)))

(defn cache-visited-moves [color parent-moves results]
    (let [cache (select-cache results)
          next-cache (assoc cache parent-moves (select-sorted-moves color results))]
        (assoc results :cache next-cache)))

(defn get-available-moves [board color cache parent-moves]
    "Gets all moves available from a given board. If we've already visited the board, we check the
    cache for the best order to visit children in. Note that we can't return the cache hit as-is
    (and instead have to use it as a sort helper) because it's possible the previous iteration
    alpha/beta pruned certain children that shouldn't be pruned in the current iteration.

    Because it's expensive to prevalidate that no moves lead to check (and are therefore illegal), we
    don't peform that validation in a separate step. Instead, we look as we go for moves that take
    kings, and use them to signal (via `:has-invalid-child`) that the _parent_ move, which failed to
    get out of check, was invalid."
    (let [child-moves (get-moves-for-color board color false)
          cached-moves (get cache parent-moves)]
        (if cached-moves
            (if (= (count cached-moves) (count child-moves))
                cached-moves ; If counts match, we know no children have been pruned, so can return cache as-is
                (let [indexes-by-move (indexes-by-item cached-moves)]
                    (sort-by #(get indexes-by-move % js/Infinity) child-moves)))
            child-moves)))

(defn get-minimax-reducer [board color depth parent-moves]
    "Minimax helper with an alpha/beta condition for early exit: `next-step` to continue iteration,
    bare return value to bail early."
    (fn [{:keys [alpha beta visited-moves has-invalid-child cache] :as accum}
         next-move
         next-step]
        (if (>= alpha beta)
            accum ; Bail early!
            (let [[from-coords to-coords] next-move
                  is-leaf-node (= depth 1)
                  captures-king (captures-king? board (reverse-color color) to-coords)
                  next-board (move-piece board from-coords to-coords)
                  next-game-tree (when (not is-leaf-node)
                                       (evaluate-game-tree next-board
                                                           (reverse-color color)
                                                           cache
                                                           (dec depth)
                                                           (conj parent-moves next-move)
                                                           alpha
                                                           beta))
                  next-score (if is-leaf-node
                                 (evaluate-board next-board)
                                 (select-best-subtree-score color next-game-tree next-board))]
                (next-step {:visited-moves (conj visited-moves {:move next-move
                                                                :score next-score
                                                                :invalid (get next-game-tree :has-invalid-child false)})
                            :cache (get next-game-tree :cache cache)
                            :has-invalid-child (or has-invalid-child captures-king)
                            :alpha (if (= color "white") (max alpha next-score) alpha)
                            :beta (if (= color "black") (min beta next-score) beta)})))))

(defn evaluate-game-tree
    "Evaluation implements the minimax algorithm, which selects moves under the assumption that both
    players will make optimal choices. This assumption enables alpha/beta pruning, where if a score
    is visited that means the current subtree will never be chosen, we can skip evaluating all other
    boards in that subtreee. `alpha` and `beta` track the highest/lowest scores in a given subtree."
    ([board color cache depth] (evaluate-game-tree board color cache depth [] (- js/Infinity) js/Infinity))
    ([board color cache depth parent-moves alpha beta]
        (->> (get-available-moves board color cache parent-moves)
             (reduce-with-early-exit
                (get-minimax-reducer board color depth parent-moves)
                {:visited-moves []
                 :cache cache
                 :has-invalid-child false
                 :alpha alpha
                 :beta beta})
             (sort-visited-moves color)
             (cache-visited-moves color parent-moves))))

(defn get-next-move [board color depth]
    "We implement an iterative-deepening approach: for 3-ply search, we first search to depth 1,
    then 2, then 3. As we go, for every move with _child_ moves, we store those moves in order of
    subtree score (in `cache`). On the final search--which consumes almost all the search time--
    we'll be more likely to check optimal moves first, and thus more likely to hit an alpha/beta
    early exit."
    (loop [cache {}
           iterative-depth 1]
        (let [game-tree (evaluate-game-tree board color cache iterative-depth)]
            (if (= iterative-depth depth)
                (select-best-move color game-tree)
                (recur (select-cache game-tree)
                       (inc iterative-depth))))))

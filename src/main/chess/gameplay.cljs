(ns chess.gameplay
    (:require [chess.utils :refer [reverse-color reduce-with-early-exit indexes-by-item]]
              [chess.moves :refer [is-check? is-checkmate? captures-king? get-moves-for-color]]
              [chess.board :refer [move-piece evaluate-board]]))

(declare evaluate-game-tree)
(declare sort-moves)

(defn get-comparator [color] (if (= color "white") > <))
(defn get-alpha-beta-comparator [color] (if (= color "white") max min))

; These values need to be less than the alpha/beta defaults (+/- infinity): otherwise, as soon as
; the algorithm finds a single checkmate, alpha/beta pruning will bail on all further subtrees. This
; isn't _quite_ desirable: if one subtree has a checkmate in 5 plies, and another in 1, we should
; find and favor the immediate checkmate.
(defn get-score-for-checkmate [color] (if (= color "white") 99999 (- 99999)))

(defn select-cache [results] (get results :cache))

(defn select-visited-moves [results] (get results :visited-moves))

(def select-best-child (comp first select-visited-moves))

(def select-child-moves (comp (partial map :move) select-visited-moves))

(def select-best-child-move (comp :move select-best-child))

(defn select-best-subtree-score [color results board]
    "If no best subtree exists (no child moves are available), looks for checkmate--if no checkmate,
    scores board as stalemate on the basis of material alone."
    (let [best-child (select-best-child results)]
        (if best-child
            (get best-child :score)
            (if (is-checkmate? board (reverse-color color))
                (get-score-for-checkmate color)
                (evaluate-board board)))))

(defn sort-moves [color moves]
    (->> moves
         (sort-by :at-depth >)
         (sort-by :score (get-comparator color))))

(defn sort-filter-visited-moves [color results]
    (assoc results :visited-moves (->> (select-visited-moves results)
                                       (filter #(not (get % :invalid)))
                                       (sort-moves color))))

(defn cache-visited-moves [color parent-moves results]
    (let [cache (select-cache results)
          next-cache (assoc cache parent-moves (select-child-moves results))]
        (assoc results :cache next-cache)))

(defn get-available-moves [board color cache moves-to-visit parent-moves]
    "Gets all moves available from a given board. If we've already visited the board, we check the
    cache for the best order to visit children in. Note that we can't return the cache hit as-is
    (and instead have to use it as a sort helper) because it's possible the previous iteration
    alpha/beta pruned certain children that shouldn't be pruned in the current iteration.

    Because it's expensive to prevalidate that no moves lead to check (and are therefore illegal), we
    don't peform that validation in a separate step. Instead, we look as we go for moves that take
    kings, and use them to signal (via `:has-invalid-child`) that the _parent_ move, which failed to
    get out of check, was invalid."
    (let [available-moves (if moves-to-visit moves-to-visit (get-moves-for-color board color false))
          cached-moves (get cache parent-moves)]
        (if cached-moves
            (if (= (count cached-moves) (count available-moves))
                cached-moves ; If counts match, we know no children have been pruned, so can return cache as-is
                (let [indexes-by-move (indexes-by-item cached-moves)]
                    (sort-by #(get indexes-by-move % js/Infinity) available-moves)))
            available-moves)))

(defn get-alpha-beta-fn [target-color]
    "Helper for building alpha-beta choosers"
    (let [comparator (get-alpha-beta-comparator target-color)]
        (fn [invalid current-color prev-val next-val]
            (cond invalid prev-val
                (= current-color target-color) (comparator prev-val next-val)
                :else prev-val))))

(def get-alpha (get-alpha-beta-fn "white"))
(def get-beta (get-alpha-beta-fn "black"))

(defn select-child-depth [game-tree current-depth]
    (let [best-child (select-best-child game-tree)]
        (if best-child (get best-child :at-depth) current-depth)))

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
                                                           nil
                                                           alpha
                                                           beta
                                                           (conj parent-moves next-move)))
                  invalid (get next-game-tree :has-invalid-child false)
                  next-score (if is-leaf-node
                                 (evaluate-board next-board)
                                 (select-best-subtree-score color next-game-tree next-board))
                  invalid (get next-game-tree :has-invalid-child false)]
                (next-step {:visited-moves (conj visited-moves {:move next-move
                                                                :score next-score
                                                                :at-depth (select-child-depth next-game-tree depth)
                                                                :invalid invalid})
                            :cache (get next-game-tree :cache cache)
                            :has-invalid-child (or has-invalid-child captures-king)
                            :alpha (get-alpha invalid color alpha next-score)
                            :beta (get-beta invalid color beta next-score)})))))

(defn evaluate-game-tree
    "Evaluation implements the minimax algorithm, which selects moves under the assumption that both
    players will make optimal choices. This assumption enables alpha/beta pruning, where if a score
    is visited that means the current subtree will never be chosen, we can skip evaluating all other
    boards in that subtree. `alpha` and `beta` track the highest/lowest scores in a given subtree."
    ([board color cache depth moves-to-visit alpha beta] (evaluate-game-tree board color cache depth moves-to-visit alpha beta []))
    ([board color cache depth moves-to-visit alpha beta parent-moves]
        (->> (get-available-moves board color cache moves-to-visit parent-moves)
             (reduce-with-early-exit
                (get-minimax-reducer board color depth parent-moves)
                {:visited-moves []
                 :cache cache
                 :has-invalid-child false
                 :alpha alpha
                 :beta beta})
             (sort-filter-visited-moves color)
             (cache-visited-moves color parent-moves))))

(defn get-next-move [board color moves-to-visit depth alpha beta next]
    "We implement an iterative-deepening approach: for 3-ply search, we first search to depth 1,
    then 2, then 3. As we go, for every move with _child_ moves, we store those moves in order of
    subtree score (in `cache`). On the final search--which consumes almost all the search time--
    we'll be more likely to check optimal moves first, and thus more likely to hit an alpha/beta
    early exit."
    (let [alpha (or alpha (- js/Infinity))
          beta (or beta js/Infinity)]
        (loop [cache {}
               iterative-depth 1]
            (let [game-tree (evaluate-game-tree board color cache iterative-depth moves-to-visit alpha beta)]
                (if (= iterative-depth depth)
                    (next game-tree)
                    (recur (select-cache game-tree)
                           (inc iterative-depth)))))))

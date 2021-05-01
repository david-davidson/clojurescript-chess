(ns chess.worker_utils
    (:require [cljs.core.async :refer [chan put! take! <! go-loop]]
              [chess.gameplay :refer [sort-moves get-alpha-beta-comparator]]
              [chess.utils :refer [flatten-once]]
              [chess.constants :refer [series parallel worker-count]]
              [chess.moves :refer [get-moves-for-color]]))

(defn get-worker []
    "Side effects: initializes web worker and sets up messaging"
    (let [web-worker (js/Worker. "/compiled/worker.js")
          from-worker (chan)
          to-worker (chan)]
        (.. web-worker (addEventListener "message" (fn [evt]
            (let [move-data (js->clj (.. evt -data) :keywordize-keys true)]
                (put! from-worker move-data)))))
        (go-loop []
            (let [val (<! to-worker)]
                (.. web-worker (postMessage (clj->js val))))
            (recur))
        [from-worker to-worker]))

(defonce worker-pool (vec (repeatedly worker-count get-worker)))

(defn message-worker [from-worker to-worker type payload]
    (js/Promise. (fn [resolve]
        (put! to-worker {:type type
                         :payload payload})
        (take! from-worker resolve))))

(defn get-alpha-beta-fn [target-color]
    "See similar helper in `gameplay.cljs` -- both do similar work, but with different data."
    (let [comparator (get-alpha-beta-comparator target-color)]
        (fn [color results]
            (when (= color target-color)
                  (->> results
                       (map :score)
                       (apply comparator))))))

(def get-alpha (get-alpha-beta-fn "white"))
(def get-beta (get-alpha-beta-fn "black"))

(defn select-best-move [color moves]
    (->> (sort-moves color moves)
         first
         :move))

(defn explore-moves-parallel [board active-color search-depth]
    "Divides up search work across the worker pool. Rather than partioning a slice of moves to each
    worker and searching each partition in parallel, we allocate work 'on demand', with each worker
    exploring a _single_ move as it becomes available. The intent is that, if a worker gets bogged
    down in its portion of the game tree, other workers won't have to wait on it and can continue
    cycling through moves.

    We lose some alpha/beta optimization _within subtrees_ by searching such small spaces in
    isolation, but gain the ability to track _top-level_ alpha/beta as we go and expose it to
    moves explored later in the process.

    We track two separate atoms, `results` and `async-work`, to decouple the shape of the results
    from the 'is work done?' criteria. e.g., once in a while a worker promise will return _no_ valid
    results ([]), which means we can't simply push into the results vector and watch _its_ length."
    (js/Promise. (fn [resolve]
        (let [child-moves (get-moves-for-color board active-color false)
              results (atom [])
              async-work (atom [])
              moves (chan)
              workers (chan)]
            (run! (partial put! workers) worker-pool)
            (run! (partial put! moves) child-moves)
            (go-loop []
                (let [move (<! moves)
                      worker (<! workers)
                      [from-worker to-worker] worker
                      promise (-> (message-worker from-worker
                                                  to-worker
                                                  parallel
                                                  {:board board
                                                   :color active-color
                                                   :search-depth search-depth
                                                   :moves-to-visit [move]
                                                   :alpha (get-alpha active-color @results)
                                                   :beta (get-beta active-color @results)})
                                  (.then (fn [latest-results]
                                    (put! workers worker)
                                    (swap! results #(concat % latest-results)))))]
                    (swap! async-work #(conj % promise))
                    (when (= (count child-moves) (count @async-work))
                          (-> (js/Promise.all @async-work)
                              (.then #(resolve @results)))))
                    (recur))))))

(defn search-series [board active-color search-depth]
    (let [[from-worker to-worker] (first worker-pool)]
        (message-worker from-worker
                        to-worker
                        series
                        {:board board
                         :color active-color
                         :search-depth search-depth})))

(defn search-parallel [board active-color search-depth]
    (-> (explore-moves-parallel board active-color search-depth)
        (.then (fn [moves]
            (js/console.log (clj->js (sort-moves active-color moves)))
            (select-best-move active-color moves)))))

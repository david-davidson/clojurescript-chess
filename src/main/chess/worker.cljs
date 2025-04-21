(ns chess.worker
    (:require [chess.gameplay :refer [get-next-move select-best-child-move select-visited-moves]]
              [chess.constants :refer [series parallel]]))

; Minimal cache of visited game states
(def cache-table (atom {}))
(defn get-cache-key [board color depth]
  (str (pr-str board) color depth))

(defn load! []
    (js/self.addEventListener "message"
        (fn [^js evt]
            (let [data (js->clj (.. evt -data) :keywordize-keys true)
                  {:keys [type payload]} data
                  {:keys [board color moves-to-visit search-depth alpha beta]} payload
                  cache-key (get-cache-key board color search-depth)]
              (if-let [cached-result (get @cache-table cache-key)]
                (js/postMessage (clj->js cached-result))
                (let [select-results (cond (= type series) select-best-child-move
                                           (= type parallel) select-visited-moves
                                           :else (throw (js/Error. (str "Unknown search strategy: " type))))
                      result (get-next-move board
                                            color
                                            moves-to-visit
                                            search-depth
                                            alpha
                                            beta
                                            select-results)]
                  (swap! cache-table assoc cache-key result)
                  (js/postMessage (clj->js result))))))))

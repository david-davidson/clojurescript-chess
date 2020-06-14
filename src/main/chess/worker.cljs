(ns chess.worker
    (:require [chess.gameplay :refer [get-next-move select-best-child-move select-visited-moves]]
              [chess.constants :refer [series parallel]]))

(defn load! []
    (js/self.addEventListener "message"
        (fn [^js evt]
            (let [data (js->clj (.. evt -data) :keywordize-keys true)
                  {:keys [type payload]} data
                  {:keys [board color moves-to-visit search-depth alpha beta]} payload
                  select-results (cond (= type series) select-best-child-move
                                       (= type parallel) select-visited-moves
                                       :else (throw (js/Error. (str "Unknown search strategy: " type))))]
                (-> (get-next-move board
                                   color
                                   moves-to-visit
                                   search-depth
                                   alpha
                                   beta
                                   select-results)
                    clj->js
                    js/postMessage)))))

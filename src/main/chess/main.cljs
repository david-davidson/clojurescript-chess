(ns chess.main
    (:require [reagent.core :as reagent]
              [chess.moves :refer [get-moves-from-position]]
              [chess.board :refer [get-initial-board to-coordinates]]))

; For testing from the REPL: (get-moves-repl "b8") => ([2 2] [2 0])
(def get-moves-repl (comp
    (partial get-moves-from-position (get-initial-board))
    to-coordinates))

(defn app []
    [:div
        [:h1 "Hello, world!"]])

; --------------------------------------------------------------------------------------------------

(defn mount! []
    (reagent/render [app] (.getElementById js/document "app")))

(defn load! []
    (mount!))

(defn reload! []
    (mount!))

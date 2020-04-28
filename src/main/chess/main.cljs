(ns chess.main
    (:require [reagent.core :as reagent]
              [chess.components :refer [app]]
              [chess.utils :refer [reverse-color]]
              [chess.board :refer [get-initial-board move-piece]]))

(defonce board (reagent/atom (get-initial-board)))
(defn set-board [new-board] (reset! board new-board))

(defonce hovered-coords (reagent/atom nil))
(defn set-hovered-coords [coords] (reset! hovered-coords coords))

(defonce active-color (reagent/atom :white))
(defn set-active-player [player] (reset! active-color player))

(defn set-piece [from-color from to]
    (let [new-board (move-piece @board from to)
          new-player (reverse-color from-color)]
        (set-active-player new-player)
        (set-board new-board)))

; --------------------------------------------------------------------------------------------------

(defn mount! []
    (reagent/render [app board hovered-coords active-color {:set-hovered-coords set-hovered-coords :set-piece set-piece}]
                    (.getElementById js/document "app")))

(defn load! []
    (mount!))

(defn reload! []
    ; We need to explicitly unmount the old app to correctly tear down `react-dnd` event listeners:
    (reagent/unmount-component-at-node (.getElementById js/document "app"))
    (mount!))

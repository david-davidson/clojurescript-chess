(ns chess.main
    (:require [reagent.core :as reagent]
              [cljs.core.async :refer [chan put! take! <! go-loop]]
              [chess.components :refer [app]]
              [chess.utils :refer [reverse-color]]
              [chess.moves :refer [is-check? is-checkmate?]]
              [chess.board :refer [get-initial-board move-piece]]))

(declare set-piece)

; Side effects: initializes worker and sets up messaging
(defn get-worker-channels []
    (let [web-worker (js/Worker. "/compiled/worker.js")
          from-worker (chan)
          to-worker (chan)]

        (.. web-worker (addEventListener "message" (fn [evt]
            (let [move-data (js->clj (.. evt -data))]
                (put! from-worker move-data)))))

        (go-loop []
            (let [val (<! to-worker)]
                (when val (.. web-worker (postMessage (clj->js val)))))
            (recur))

        [from-worker to-worker]))

(defonce worker-channels (get-worker-channels))

(defn message-worker [board color search-depth]
    (let [[from-worker to-worker] worker-channels]
        (js/Promise. (fn [resolve]
            (put! to-worker {:board board
                             :color color
                             :search-depth search-depth})
            (take! from-worker resolve)))))

(defonce search-depth (reagent/atom 4))
(defn set-search-depth [depth] (reset! search-depth depth))

(defonce board (reagent/atom (get-initial-board)))
(defn set-board [new-board] (reset! board new-board))

(defonce hovered-coords (reagent/atom nil))
(defn set-hovered-coords [coords] (reset! hovered-coords coords))

(defonce active-color (reagent/atom "white"))
(defn set-active-color [color] (reset! active-color color))

(defn init-next-move []
    (when (= @active-color "black")
          (-> (message-worker @board @active-color @search-depth)
              (.then (fn [[from to]]
                (set-piece @active-color from to))))))

(defn alert-check-checkmate [new-board from-color]
    (js/setTimeout (fn []
        (when (is-check? new-board (reverse-color from-color))
              (if (is-checkmate? new-board (reverse-color from-color))
                  (js/alert "Checkmate")
                  (js/alert "Check")))) 20)) ; 20ms to give time for board to render latest move

(defn set-piece [from-color from to]
    (let [new-board (move-piece @board from to)
          new-color (reverse-color from-color)]
        (set-active-color new-color)
        (set-board new-board)
        (alert-check-checkmate new-board from-color)
        (init-next-move)))

; --------------------------------------------------------------------------------------------------

(defn mount! []
    (reagent/render [app
                     board
                     hovered-coords
                     active-color
                     search-depth
                     {:set-hovered-coords set-hovered-coords
                      :set-piece set-piece
                      :set-search-depth set-search-depth}]
                    (.getElementById js/document "app")))

(defn load! []
    (mount!))

(defn reload! []
    ; We need to explicitly unmount the old app to correctly tear down `react-dnd` event listeners:
    (reagent/unmount-component-at-node (.getElementById js/document "app"))
    (mount!))

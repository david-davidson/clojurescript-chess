(ns chess.components
    (:require [chess.moves :refer [get-moves-from-position]]
              [chess.utils :refer [reverse-color to-safe-props from-safe-props get-js-children]]
              [reagent.core :as reagent]
              ["react-dnd" :as react-dnd :refer [DndProvider useDrag useDrop]]
              ["react-dnd-html5-backend" :as react-html5-backend]
              [chess.pieces :refer [piece-symbols is-vacant?]]))

(def square-size 75)
(def piece-size 60)

(defn get-square-background-color [row-idx col-idx available-moves]
    (if (some #(= % [row-idx col-idx]) available-moves)
        "springgreen"
        (if (= (even? row-idx) (even? col-idx)) "white" "gray")))

(defn square-ui [js-props]
    (let [props (from-safe-props js-props)
          children (get-js-children js-props)
          {:keys [row-idx col-idx available-moves set-hovered-coords set-piece]} props
          [_ ref] (useDrop (clj->js {
              :accept :piece
              :drop (fn [item]
                (let [from-coords (get (js->clj item) "from-coords")]
                    (set-piece from-coords [row-idx col-idx])))
              :canDrop (fn [item]
                (let [available-moves (get (js->clj item) "available-moves")]
                    (some #(= % [row-idx col-idx]) available-moves)))
          }))]
        ^{:key (str row-idx col-idx)}
        (reagent/as-element [:div {
            :style {
                :height square-size
                :width square-size
                :align-items "center"
                :justify-content "center"
                :background-color (get-square-background-color row-idx col-idx available-moves)
                :display "flex"}
            :ref ref}
                children])))

(defn piece-ui [js-props]
        (let [props (from-safe-props js-props)
              {:keys [piece coords available-moves set-hovered-coords]} props
              [dnd-props ref] (useDrag (clj->js {
                :item {
                    :type :piece
                    :from-coords coords
                    :available-moves available-moves
                }
                :collect (fn [monitor] {
                    :is-dragging (.isDragging monitor)
                })}))]
            (when (not (is-vacant? piece))
                (reagent/as-element [:div {:style {
                    :background-color (get piece :color)
                    :color (reverse-color (get piece :color))
                    :height piece-size
                    :width piece-size
                    :border-radius piece-size
                    :font-size "2em"
                    :display "flex"
                    :align-items "center"
                    :justify-content "center"
                    :border "1px solid"
                    :cursor "pointer"
                    :opacity (if (get dnd-props :is-dragging) 0.1 1)}
                    :ref ref
                    :on-mouse-enter #(apply set-hovered-coords [coords])
                    :on-mouse-leave #(apply set-hovered-coords [nil])}
                        [:div {:style {:transform "translateY(-3px)"}}
                            (piece-symbols (get piece :type))]]))))

(defn row-label [idx]
    [:div {:style {:align-self "center" :padding-right 10}} (- 8 idx)])

(defn col-labels [char]
    [:div {:style {:display "flex" :margin-left 18 :margin-top 10}}
        (map (fn [char]
                ^{:key char}
                [:div {:style {
                        :width square-size
                        :height square-size}}
                    char])
             ["A" "B" "C" "D" "E" "F" "G" "H"])])

(defn board-ui [board hovered-coords {:keys [set-hovered-coords set-piece]}]
    [:> DndProvider {:backend react-html5-backend/default}
        [:div {:style {:display "inline-block"}}
            (let [available-moves (get-moves-from-position board hovered-coords)]
                (map-indexed (fn [row-idx row]
                                ^{:key row-idx}
                                [:div {:style {:display "flex"}}
                                    [row-label row-idx]
                                    (map-indexed (fn [col-idx piece]
                                                    ^{:key (str row-idx col-idx)}
                                                    [:> square-ui (to-safe-props {
                                                        :row-idx row-idx
                                                        :col-idx col-idx
                                                        :available-moves available-moves
                                                        :set-piece set-piece})
                                                        [:> piece-ui (to-safe-props {
                                                            :piece piece
                                                            :coords [row-idx col-idx]
                                                            :available-moves available-moves
                                                            :set-hovered-coords set-hovered-coords
                                                        })]])
                                                 row)])
                             board))
                [col-labels]]])

(defn app [board hovered-coords handlers]
    [:div {:style { :text-align "center" :margin-top 50 }}
        [board-ui @board @hovered-coords handlers]])

(ns chess.components
    (:require [chess.moves :refer [get-moves-from-position]]
              [chess.utils :refer [reverse-color to-safe-props from-safe-props get-js-children]]
              [reagent.core :as reagent]
              [chess.board :refer [lookup]]
              ["react-dnd" :as react-dnd :refer [DndProvider useDrag useDrop]]
              ["react-dnd-html5-backend" :as react-html5-backend]
              [chess.pieces :refer [piece-symbols is-vacant?]]))

(def square-size 75)
(def piece-size 60)

(defn get-square-background-color [hovered-color row-idx col-idx available-moves]
    (if (contains? available-moves [row-idx col-idx])
        (if (= hovered-color "white") "springgreen" "lightblue")
        (if (= (even? row-idx) (even? col-idx)) "white" "gray")))

(defn square-ui [js-props]
    (let [props (from-safe-props js-props)
          children (get-js-children js-props)
          {:keys [row-idx col-idx available-moves set-hovered-coords set-piece hovered-color]} props
          [_ ref] (useDrop (clj->js {
              :accept :piece
              :drop (fn [js-piece]
                (let [piece-data (from-safe-props js-piece)
                      from-coords (get piece-data :from-coords)
                      from-color (get piece-data :color)]
                    (set-piece from-color from-coords [row-idx col-idx])))
              :canDrop (fn [js-piece]
                (let [piece-data (from-safe-props js-piece)
                      available-moves (get piece-data :available-moves)]
                    (contains? available-moves [row-idx col-idx])))
          }))]
        ^{:key (str row-idx col-idx)}
        (reagent/as-element [:div {
            :style {
                :height square-size
                :width square-size
                :align-items "center"
                :justify-content "center"
                :background-color (get-square-background-color hovered-color row-idx col-idx available-moves)
                :display "flex"}
            :ref ref}
                children])))

(defn piece-ui [js-props]
        (let [props (from-safe-props js-props)
              {:keys [piece coords available-moves active-color set-hovered-coords]} props
              belongs-to-active-color (and (= active-color "white") (= active-color (get piece :color)))
              [dnd-props ref] (useDrag (clj->js {
                :item (merge {:type :piece}
                              (to-safe-props {
                                :color (get piece :color)
                                :from-coords coords
                                :available-moves available-moves
                              }))
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
                    :user-select "none"
                    :justify-content "center"
                    :border "1px solid"
                    :cursor (if belongs-to-active-color "pointer" "default")
                    :opacity (if (get dnd-props :is-dragging) 0.1 1)}
                    :ref (if belongs-to-active-color ref nil)
                    :on-mouse-enter #(set-hovered-coords coords)
                    :on-mouse-leave #(set-hovered-coords nil)}
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

(def max-search-depth 6)
(defn depth-ui [search-depth set-search-depth]
    [:div {:style {:position "absolute" :top 40 :left 18}} "Search depth: "
    [:select
        {:value search-depth
         :on-change #(set-search-depth (->> % .-target .-value int))}
        (map (fn [idx]
            ^{:key idx}
            [:option { :value idx } idx]) (range 1 max-search-depth))]])

(defn loading-ui [active-color]
    [:div {:style { :width 80 :height 80 :margin "0 auto 20px"}}
        (when (= active-color "black")
            [:img { :src "spinner.gif" :height "100%" :width "100%" }])])

(defn board-ui [board hovered-coords active-color {:keys [set-hovered-coords set-piece]}]
    [:> DndProvider {:backend react-html5-backend/default}
        [:div {:style {:display "inline-block"}}
            (let [available-moves (set (get-moves-from-position board hovered-coords))
                  hovered-color (:color (lookup board hovered-coords))]
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
                                                        :hovered-color hovered-color
                                                        :set-piece set-piece})
                                                        [:> piece-ui (to-safe-props {
                                                            :piece piece
                                                            :coords [row-idx col-idx]
                                                            :active-color active-color
                                                            :available-moves available-moves
                                                            :set-hovered-coords set-hovered-coords
                                                        })]])
                                                 row)])
                             board))
                [col-labels]]])

(defn header-ui [search-depth active-color set-search-depth]
[:div {:style {:display "flex" :position "relative"}}
    [depth-ui search-depth set-search-depth]
    [loading-ui active-color]
])

(defn app [board hovered-coords active-color search-depth handlers]
    [:div {:style { :text-align "center" }}
        [:div {:style { :display "inline-block" }}
            [header-ui @search-depth @active-color (get handlers :set-search-depth)]
            [board-ui @board @hovered-coords @active-color handlers]]])

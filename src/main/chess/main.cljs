(ns chess.main
    (:require [reagent.core :as reagent]))

(defn app []
    [:div
        [:h1 "Hello, world!"]])

; --------------------------------------------------------------------------------------------------

(defn mount! []
    (println (.getElementById js/document "app"))
    (reagent/render [app] (.getElementById js/document "app")))

(defn load! []
    (mount!))

(defn reload! []
    (mount!))

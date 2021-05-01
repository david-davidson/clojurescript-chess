(ns chess.utils
    (:require [reagent.core :as reagent]))

(defn key-by
    ([coll] (key-by identity coll))
    ([key-fn coll]
        (reduce (fn [total current] (assoc total (key-fn current) current))
                {}
                coll)))

(def flatten-once (partial mapcat identity)) ; Only flattens one level deep

(defn reverse-color [color] (if (= color "black") "white" "black"))

(defn const [x] (fn [] x))

(defn reduce-indexed [reducer total coll]
    (loop [coll coll
           total total
           idx 0]
        (let [current (first coll)]
            (if (nil? current)
                total
                (recur (rest coll)
                       (reducer idx total current)
                       (inc idx))))))

(defn indexes-by-item [coll]
    (reduce-indexed (fn [idx total current] (assoc total current idx))
                    {}
                    coll))

; Per https://github.com/reagent-project/reagent/issues/389, interop between React components and
; Reagent components automatically coerces primitives between CLJS and JS, in ways we don't usually
; want (snake case to camel case, etc). To leave CLJS values untouched when we pass them in to
; pure-React components, we wrap them in an atom, which Reagent won't try to convert. On the other
; side, we unpack the atom to access untouched CLJS values.
; Surely there's a better way than this?...
(defn to-safe-props [props] {:props (reagent/atom props)})
(defn from-safe-props [js-props] @(get (js->clj js-props) "props"))
(defn get-js-children [js-props]
    (. js-props -children))

(ns chess.utils)

(defn key-by
    ([xs] (key-by identity xs))
    ([key-fn xs]
        (reduce #(assoc %1 (key-fn %2) %2)
                {}
                xs)))

(def flatten-once (partial mapcat identity)) ; Only flattens one level deep

(defn reverse-color [color] (if (= color :black) :white :black))

(defn const [val] (fn [] val))

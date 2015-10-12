(ns centaur.styles
  (:require [garden.core :refer [css]]
            [garden.units :as u :refer [px percent em]]
            [garden.color :as color :refer [hsl rgb]]
            [goog.style]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Inserting / removing styles in a reloadable way

(declare app-styles)

(defonce styles (atom {}))

(defn remove-styles* [s]
  (when-let [el (:styles s)]
    (.removeChild js/document.head el)))

(defn insert-styles* [s css]
  (remove-styles* s)
  (assoc s :styles (goog.style/installStyles css)))

(defn insert-styles! [css]
  (swap! styles insert-styles* css))

(defn insert-app-styles! []
  (insert-styles! app-styles))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Garden styles

(def app-styles
  (css
   [:body {:background (rgb 250 250 240)}]
   [:.centered {:margin "0 auto"
                :max-width (px 750)
                :width (percent 60)}]))

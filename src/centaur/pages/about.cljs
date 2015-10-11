(ns centaur.pages.about
  (:require [centaur.common :as c :refer [md action!]]
            [rum.core :as rum]
            [devcards.util.edn-renderer :refer [html-edn]]
            [devcards.util.markdown :refer [parse-out-blocks]]
            [cljsjs.highlight]
            [cljsjs.highlight.langs.clojure]))

(rum/defc about < rum/static [state inputs]
  [:div
   (md
    "An about page.")])

(defmethod c/page [:page {:name "about"}] [_ state inputs]
  (about state inputs))

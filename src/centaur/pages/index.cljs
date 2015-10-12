(ns centaur.pages.index
  (:require [centaur.common :as c :refer [md clj-code action!]]
            [rum.core :as rum]
            [devcards.util.edn-renderer :refer [html-edn]]
            [devcards.util.markdown :refer [parse-out-blocks]])
  (:require-macros [devcards.system :refer [inline-resouce-file]]))

(rum/defc index < rum/static [state inputs]
  [:div.centered
   [:header#header ]
   [:article
    (md
     "# An index page!"
     "and some codee:"
     "> A quote aa
     >
     > Cool "
     (clj-code
      "(defn foo [a b]
         (+ a e))"))]])

(defmethod c/page [:index] [_ state inputs]
  (index state inputs))




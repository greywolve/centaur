(ns centaur.common
  (:require [cljs.core.async :refer [put! chan close! timeout]]
            [cljsjs.marked]
            [devcards.util.markdown :refer [parse-out-blocks]]
            [rum.core :as rum])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Common multimethods

(defmulti perform-action (fn [state action]
                           (first action)))
(defmulti effect (fn [state inputs e]
                   (first e)))

(defmulti page (fn [view state inputs]
                 (let [[handler args] view]
                      (if (empty? args)
                        [handler]
                        view))))

(defmethod page :default [view _ _]
  "Not found")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Actions and effect helpers

(defn action! [inputs action]
  (fn [e]
    (put! (:actions-chan inputs) action)
    nil))

(defn add-effect [state effect]
  (let [effects (-> (concat (:effects state) [effect])
                    distinct
                    (into []))]
    (assoc state :effects effects)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Markdown and Code Highlighting

(def highlight-mixin
  {:did-mount (fn [state]
                (let [owner (.getDOMNode (:rum/react-component state))]
                  (doall (some->> (.querySelectorAll owner "pre code")
                                  array-seq
                                  (map #(.highlightBlock js/hljs %))))))})

(rum/defc markdown < rum/static [md-str]
  [:div {:dangerouslySetInnerHTML {:__html
                                   (js/marked md-str)}}])

(rum/defc code-block < rum/static highlight-mixin [code-str lang]
  [:pre
   [:code {:lang lang}
    code-str]])

(defmulti markdown-block->react :type)

(defmethod markdown-block->react :default [{:keys [content]}]
  (markdown content))

(defmethod markdown-block->react :code-block [{:keys [content lang]}]
  (code-block content lang))

(rum/defc md [& strs]
  [:div
   (if (every? string? strs)
     (let [blocks (mapcat parse-out-blocks strs)]
       [:div
        (for [b blocks]
          (-> b
              markdown-block->react
              (rum/with-key (hash (:content b)))))])
     [:div "Error, all arguments for markdown must be strings."])])

(defn clj-code [code-str]
  (str "```clojure\n" code-str "\n```\n"))

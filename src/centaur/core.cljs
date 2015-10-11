(ns ^:figwheel-always centaur.core
  (:require [centaur.common :as c :refer [md action!]]
            [centaur.pages.index]
            [centaur.pages.about]
            [cljs.core.async :refer [put! chan close! timeout]]
            [devcards.util.edn-renderer :refer [html-edn]]
            [bidi.bidi :as bidi]
            [goog.dom :as dom]
            [goog.events :as events]
            [goog.events.EventType :as EventType]
            [goog.history.EventType :as HistEventType]
            [rum.core :as rum])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:import [goog History]
           [goog.dom query]
           [goog.dom ViewportSizeMonitor]))

(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; App state, and inputs

(defonce app-state (atom {:routes ["/" [[""               :index]
                                        [["pages/" :name] :page]
                                        [["posts/" :name] :post]
                                        [true             :not-found]]] }))
(defn update-state [state action]
  (c/perform-action
   ;; reset the effects queue
   (assoc state :effects [])
   action))

(defonce actions-chan (chan))

(defonce inputs {:actions-chan actions-chan})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Main event loop

(defn event-loop!
  [app-state actions-chan action-handler-fn]
  (go (loop [action (<! actions-chan)]
        (when action
          (swap! app-state action-handler-fn action)
          (recur (<! actions-chan))))))

(defonce event-loop (event-loop! app-state actions-chan update-state))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Routing / History

(defonce history
  (let [h (History.)]
    (events/listen h HistEventType/NAVIGATE #(put! actions-chan [:set-view-from-url (-> % .-token)]))
    (.setEnabled h true)
    h))

(defn history-token []
  (.getToken history))

(defn set-history-token! [token]
  (when-not (= token (history-token))
    (.setToken history token)))

;; token is everything after the # (anchor) in the url
;; view is a variant of the form [route-identifier-keyword route-params-map] e.g [:cogs {:name "blah"}]

(defn token->view [routes token]
  (let [{:keys [handler route-params] :as bidi-match} (bidi/match-route routes token)]
    [handler (or route-params {})]))

(defn view->token [routes [view-key args-map]]
  (apply bidi/path-for routes view-key (-> args-map seq flatten)))


(defmethod c/perform-action :set-view [state [_ view]]
  (let [route-exists? (-> view first (not= :not-found))]
    (-> state
        (assoc :current-view view)
        (cond->
            route-exists?
          (c/add-effect [:set-history-token
                       (view->token (:routes state) view)])))))

(defmethod c/perform-action :set-view-from-url [state [_ token]]
  (let [view          (if (empty? token)
                        (or (:start-view state) [:index {}])
                        (token->view (:routes state) token))]
    (c/perform-action state [:set-view view])))

(defmethod c/effect :set-history-token [state _ [_ token]]
  (set-history-token! token))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Root component

(rum/defc app < rum/static [state inputs]
  [:div
   (c/page (:current-view state) state inputs)])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Rendering and effects loop

(defonce render-and-effects-loop
    (add-watch app-state :render
                    (fn [_ _ _ state]
                      (rum/mount (app @app-state inputs) js/document.body)
                      (doseq [e (:effects state)]
                        (c/effect state inputs e)))))

;; get the ball rolling
(swap! app-state update-in [:__figwheel_counter] inc)


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)


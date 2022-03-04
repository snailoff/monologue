(ns knot.frontend.actions
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [knot.frontend.state :refer [app-state]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))


(defn get-pieces []
      (go (let [response (<! (http/get "/api/piece-recent-list"
                                       {:with-credentials? false}))]
               (swap! app-state assoc :pieces ((response :body) :pieces)))))

(defn get-piece-recent-one []
      (go (let [response (<! (http/get "/api/piece-recent-one"
                                       {:with-credentials? false}))]
               (swap! app-state assoc :piece ((response :body) :piece)))))

(defn get-piece [piece-id]
      (go (let [response (<! (http/get (str "/api/piece/" piece-id)
                                       {:with-credentials? false}))]
               (swap! app-state assoc :piece ((response :body) :piece)))))




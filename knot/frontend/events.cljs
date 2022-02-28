(ns knot.frontend.events
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [knot.frontend.state :refer [app-state]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(defn increment
      [event]
      (.preventDefault event)
      (swap! app-state update-in [:count] inc)
      (go (let [response (<! (http/get "/api/piece/89"
                                       {:with-credentials? false}))]
               (prn ((response :body) :content))
               (swap! app-state assoc :page ((response :body) :content)))))


(defn decrement
      [event]
      (.preventDefault event)
      (swap! app-state update-in [:count] dec))
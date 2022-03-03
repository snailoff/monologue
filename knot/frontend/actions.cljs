(ns knot.frontend.actions
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [knot.frontend.state :refer [app-state]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))


(defn get-pieces []
      (go (let [response (<! (http/get "/api/pieces"
                                       {:with-credentials? false}))]
               (swap! app-state assoc :pieces ((response :body) :pieces)))))





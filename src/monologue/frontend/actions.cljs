(ns monologue.frontend.actions
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [monologue.frontend.state :as state :refer [s-piece s-pieces]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(def backend "https://b.monologue.me")

(defn get-pieces []
      (go (let [response (<! (http/get (str backend "/api/piece-recent-list")
                                       {:with-credentials? false}))]

               (state/set-pieces (-> response :body :pieces))
               )))

(defn get-piece-recent-one []
      (go (let [response (<! (http/get (str backend "/api/piece-recent-one")
                                       {:with-credentials? false}))]
               (state/set-piece (-> response :body :piece)))))


(defn get-piece [piece-id]
      (go (let [response (<! (http/get (str backend "/api/piece/" piece-id)
                                       {:with-credentials? false}))]
               (state/set-piece (-> response :body :piece)))))







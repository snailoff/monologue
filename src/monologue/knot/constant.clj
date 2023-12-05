(ns monologue.knot.constant
  (:require [environ.core :refer [env]]))

(def db-config {:dbtype      "postgresql"
                :host        (env :monologue-db-host)
                :port        (env :monologue-db-port)
                :dbname      (env :monologue-db-name)
                :user        (env :monologue-db-user)
                :password    (env :monologue-db-password)
                :auto-commit true})

(def git-config {:login (env :monologue-git-user)
                 :pw    (env :monologue-git-token)
                 :repo  (env :monologue-git-repository)})

(def knot-config {:workspace (env :monologue-knot-workspace)
                  :resource  (env :monologue-knot-resource)
                  :start-page "@index"
                  :default-page "@tags"
                  :404-page "@404"
                  :template-file "@knot.html"})
(def memo (atom {:git-commit-id-save? true}))
(defn memo-set [key val] (swap! memo assoc-in [key] val))

(def META-GIT-COMMIT-ID "GIT-COMMIT-ID")

(comment
  db-config
  knot-config)
(ns monologue.backend.core
  (:require [monologue.backend.gitter :as gitter]
            [monologue.backend.router :as router])

  (:gen-class))

(defn -main
  [& _]
  (gitter/reload-schedule)
  (router/start))



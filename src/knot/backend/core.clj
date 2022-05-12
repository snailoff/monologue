(ns knot.backend.core
  (:require [knot.backend.gitter :as gitter]
            [knot.backend.router :as router])

  (:gen-class))

(defn -main
  [& _]
  (gitter/reload-schedule)
  (router/start))



(ns knot.backend.main
  (:require
            [knot.backend.gitter :as gitter]
            [knot.backend.router :as router]))

(defn -main
  [& _]
  (gitter/reload-schedule)
  (router/start))



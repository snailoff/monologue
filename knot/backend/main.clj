(ns knot.backend.main
  (:require [immutant.scheduling :as cron]
            [knot.backend.gitter :as gitter]
            [knot.backend.web :as web]
            [ring.adapter.jetty :as jetty]))

(defn start []
  (jetty/run-jetty #'web/app {:port 1234, :join? false})
  (println "server running in port 1234"))

(defn reload-schedule []
  (cron/schedule #(gitter/reload-md) (cron/cron "0 */1 * ? * *")))

(defn -main
  [& _]
  (reload-schedule)
  (start))



(ns knot.backend.main
  (:require [immutant.scheduling :as cron]
            [knot.backend.gitter :as gitter]
            [knot.backend.web :as web]
            [ring.adapter.jetty :as jetty]
            [taoensso.timbre :as b]))

(defn start []
  (jetty/run-jetty #'web/app {:port 1234, :join? false})
  (b/debug "start!")
  (b/spy :info (* 1 2 3)))


(defn reload-schedule []
  (cron/schedule #(gitter/reload-md) (cron/cron "0 */1 * ? * *")))

(defn -main
  [& _]
  (reload-schedule)
  (start))



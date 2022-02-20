(ns knot-md.frontend.app
  (:require [cljs.nodejs :as node]))

(def express (node/require "express"))

(def app (new express))

(defn -main
      []
      (.listen app
               5678
               (fn [] (js/console.log "App Started at http://localhost:5678"))))

(set! *main-cli-fn* -main)

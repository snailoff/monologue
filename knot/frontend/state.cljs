(ns knot.frontend.state
  (:require [reagent.core :refer [atom]]))

(defonce s-piece (atom {}))
(defonce s-pieces (atom []))

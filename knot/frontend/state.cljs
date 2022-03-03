(ns knot.frontend.state
  (:require [reagent.core :refer [atom]]))

(defonce app-state (atom {:pieces []
                          :tags []
                          :piece {}}))

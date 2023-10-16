(ns monologue.backend.service
  (:require [monologue.backend.mapper :as mmap]
            [monologue.backend.generator :as mgen]
            [monologue.backend.constant :refer [db-config]]))






(defn load-page-one [page-name]
  (let [piece (mmap/select-piece-by-subject db-config page-name)
        content (mgen/html-content-page piece)]
    (mgen/html-wrap content)))

(defn load-pages []
  (mgen/html-wrap
    (mgen/html-content-pages
      (mmap/pieces-recent-many 10)
      (mmap/pieces-years))))

(defn load-pages-year [year]
  (mgen/html-wrap
    (mgen/html-content-pages-in-year
      (mmap/pieces-recent-many 1000 year)
      year)))


(mgen/html-content-pages
  (mmap/pieces-recent-many 10)
  (mmap/pieces-years))



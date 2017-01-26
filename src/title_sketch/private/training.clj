(ns title-sketch.private.training
  (:require [clucy.core :as clucy]
            [title-sketch.private.core :as core]
            [title-sketch.private.data :as data]
            [util
             [util :as util]
             [io :as io]
             [types :as types]]))


(def ^:dynamic *bespoke-titles* "bespoke_titles.csv")
(def ^:dynamic *job-title-jd* "job_title_jd.csv")


(def ^:private init-data
  (memoize
   (fn []
     (assoc {}
            :bespoke-title (data/load-csv-header *bespoke-titles*)
            :jd-title (data/load-csv-header *job-title-jd*)))))


(defn index
  []
  (let [index         (clucy/memory-index)
        training-data (init-data)]
    (map (fn [x] (clucy/add index x)) (:bespoke-title training-data))
    (map (fn [y] (try
                   (assoc y
                        :clean-title
                        (-> (clucy/search index (:title y) 1)
                            first
                            :job_title))
                   (catch Exception e (prn "error"))
                   (finally nil)))
         (:jd-title training-data))
    ))

(ns title-sketch.private.training
  (:require [clucy.core :as clucy]
            [title-sketch.private.core :as core]
            [util
             [util :as util]
             [io :as io]
             [types :as types]]))


(def ^:dynamic *bespoke-title-path* "resources/data/bespoke_titles.csv")
(def ^:dynamic *jd-title-path* "resources/data/job_title_jd.csv")

(defn- convert-data
  "Uses the first row as column labels and returns a collection of hash maps"
  [data]
  (let [labels (map util/to-kw (first data))]
    (map (partial zipmap labels) (rest data))))

(defn- load-csv
  "Load csv file into memory"
  [file]
  (convert-data
    (io/read-csv file
    :separators #{\,}
                 :enclosing {\" \"})))

(def ^:private init-data
  (memoize
   (fn []
     (assoc {}
            :bespoke-title (load-csv *bespoke-title-path*)
            :jd-title (load-csv *jd-title-path*)))))


(defn index
  []
  (let [index (clucy/memory-index)
        training-data (init-data)]
    (map (fn [x] (clucy/add index x)) (:bespoke-title training-data))
    (map (fn [y] (assoc y
                        :clean-title
                        (-> (clucy/search index (:title y) 1)
                            first
                            :job_title)))
         (take 100 (:jd-title training-data)))
    ))

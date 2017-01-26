(ns title-sketch.private.training
  (:require [clucy.core :as clucy]
            [title-sketch.private.core :as core]
            [title-sketch.private.elastic :as elastic]
            [title-sketch.private.data :as data]
            [util
             [util :as util]
             [io :as io]
             [types :as types]]))

(def ^:dynamic *elastic-mapping*
  {:mapping
                     {:_all {:enabled false}
                      :properties
                            {:job-title-id {:type "string" :index "not_analyzed"}
                             :job-title {:type "string" :analyzer "job_analyzer"}
                             :job-function-id {:type "string" :index "not_analyzed"}
                             :job-function {:type "string" :index "not_analyzed"}}}
   :settings
                     {:analysis
                      {:filter
                       {:english_stop {:type "stop" :stopwords "_english_"}
                        :english_stemmer {:type "stemmer" :language "english"}
                        :english_possessive_stemmer
                        {:type "stemmer" :language "possessive_english"}}
                       :analyzer
                       {:job_analyzer
                        {:tokenizer "standard"
                         :char_filter ["html_strip"]
                         :filter
                         ["english_possessive_stemmer"
                          "asciifolding"
                          "lowercase"
                          "english_stop"
                          "english_stemmer"]}}}}
   :index-name "job-title"
   :last-update-time 1484769443})

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

(defn index-elastic
  []
  (if (elastic/index-exists? (:index-name *elastic-mapping*))
    (elastic/delete-index (:index-name *elastic-mapping*)))
  (elastic/create-index "job-title" *elastic-mapping*))

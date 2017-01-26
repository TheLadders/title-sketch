(ns title-sketch.private.training
  (:require [clucy.core :as clucy]
            [title-sketch.private.core :as core]
            [title-sketch.private.elastic :as elastic]
            [util
             [util :as util]
             [io :as io]
             [types :as types]]))


(def ^:dynamic *bespoke-title-path* "resources/data/bespoke_titles.csv")
(def ^:dynamic *jd-title-path* "resources/data/job_title_jd.csv")
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

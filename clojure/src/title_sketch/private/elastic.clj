(ns title-sketch.private.elastic
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [clj-http.client :as http]))

(def local-elasticsearch "http://localhost:9200")

(def ^:dynamic *elasticsearch-uri* local-elasticsearch)


(defn create-index
  "Creates elastic index, with name and options map with possible keys:
    * :mappings  map with elastic mappings.
    * :settings  map of settings, custom analyzers, etc.
    * :shards    number of shards.
    * :replicas  number of replicas."
  [name opts]
  (let [{:keys [mappings settings shards replicas]} opts
        index-args (cond-> {}
                           mappings (assoc :mappings mappings)
                           settings (assoc :settings settings)
                           replicas (assoc-in [:settings :number_of_replicas] replicas)
                           shards (assoc-in [:settings :number_of_shards] shards))]
    (http/put (str *elasticsearch-uri* "/" name "/")
              {:content-type :json :form-params index-args
               :insecure? true})))

(defn index-info
  "Returns a map of information about an index, or nil if it doesn't exist."
  [name]
  (try
    (-> (http/get (str *elasticsearch-uri* "/" name) {:insecure? true})
        :body
        (json/read-str :key-fn keyword)
        (get (keyword name)))
    (catch Exception e
      (if (= 404 (:status (ex-data e)))
        nil
        (throw e)))))

(defn delete-index
  "Deletes index."
  [name]
  (http/delete (str *elasticsearch-uri* "/" name "/")
               {:insecure? true}))

(defn add-document
  "Adds document of type with specified id to index."
  [index type id document]
  (http/put (str *elasticsearch-uri* "/" index "/" type "/" id)
            {:content-type :json
             :form-params document
             :insecure? true}))

(defn bulk-add-documents
  "Adds a collection of documents of given type to index. Documents
   is either a map of id to document, or a collection of [id document]
   tuples."
  [index type documents]
  (let [data (->> documents
                  (mapcat (fn [[id document]]
                            [{:index {:_id id}}
                             document]))
                  (map http/json-encode))
        data-str (-> data
                     (interleave (repeat "\n"))
                     (str/join))]
    (http/post (str *elasticsearch-uri* "/" index "/" type "/_bulk")
               {:content-type :json
                :body data-str
                :insecure? true})))

(defn index-exists?
  [index]
  (try
    (-> (http/get (str *elasticsearch-uri* "/_cat/indices/" index))
      :body
      )
    true
    (catch Exception e false)))

(defn search
  "Performs elastic query against index. Returns results converted to
   Clojure map."
  [index query]
  (-> (http/post (str *elasticsearch-uri* "/" index "/_search")
                 {:content-type :json
                  :form-params query
                  :insecure? true})
      :body
      (json/read-str :key-fn keyword)))

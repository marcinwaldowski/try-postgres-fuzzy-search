(ns try-postgres-fuzzy-search.core
  (:require [clojure
             [edn :as edn]
             [string :as str]]
            [clojure.java
             [io :as io]
             [jdbc :as jdbc]]
            [migratus.core :as migratus]
            [mount.core :as mount :refer [defstate]])
  (:import com.github.javafaker.Faker))

(defn- read-db-cfg
  "Reads config for database connection."
  []
  (-> "config.edn" io/resource slurp edn/read-string))

(defn- read-mig-cfg
  "Reads config for database migration."
  []
  {:store :database
   :migration-dir "migrations/"
   :db (read-db-cfg)})

(defn create-schema!
  "Creates database schema."
  []
  (migratus/migrate (read-mig-cfg)))

(defn drop-schema!
  "Removes database schema."
  []
  (migratus/rollback (read-mig-cfg)))

(defstate ^:private faker
  :start (Faker.))

(defstate ^:private con
  :start {:connection (jdbc/get-connection (read-db-cfg))}
  :stop (.close (:connection con)))

(defn connect!
  "Connect to database."
  []
  (mount/start))

(defn disconnect!
  "Disconnect from database."
  []
  (mount/stop))

(def ^:private find-similar-words-sql
  "SELECT word
   FROM word
   WHERE word % ?")

(defn- find-similar-words
  "Finds similar words in database for given word.
  Example:
  (find-similar-words \"Mario\") -> (dario maggio) "
  [word]
  (jdbc/query con
              [find-similar-words-sql word]
              {:row-fn :word}))

(def ^:private search-person-sql
  "SELECT id,
          first_name,
          last_name,
          similarity(last_name, ?) +
          similarity(first_name, ?) +
          coalesce(similarity(last_name, ?), 0) +
          coalesce(similarity(first_name, ?), 0)  as rank
   FROM person
   WHERE tsv @@ to_tsquery('simple', ?)
   ORDER BY rank DESC
   LIMIT 100")

(defn- make-ts-query
  "Makes text search query from coll of words.
  Example: (make-ts-query [[\"one\" \"two\"][\"three\" \"four\"]]) ->
           (one:* | two:*) & (three:* | four:*)"
  [words-colls]
  (str/join
   " & "
   (map (fn [words]
          (str "("
               (->> words
                    (map #(str % ":*"))
                    (str/join " | "))
               ")"))
        words-colls)))

(defn- with-similar-words
  "Create set of given word plus words similar to it."
  [word]
  (-> word
      find-similar-words
      set
      (conj (str/lower-case word))))

(defn search-person
  "Searches for person with one or two search strings which could be
   first name and last name with any order. Arguments can be given
   with typos. Using only the beginning of first and last name is
   supported too."
  [word1 & [word2]]
  (let [words (keep identity [word1 word2])
        simws (map with-similar-words words)]
    (jdbc/query con [search-person-sql
                     word1
                     word1
                     word2
                     word2
                     (make-ts-query simws)])))

(def ^:private get-max-person-id-sql
  "SELECT max(id) as max FROM person")

(defn- get-max-person-id
  "Gets maximum id of person in database."
  []
  (first (jdbc/query con
                     [get-max-person-id-sql]
                     {:row-fn :max})))

(def ^:private get-persons-sql
  "SELECT id, first_name, last_name
   FROM person
   WHERE id > ?")

(defn get-random-persons
  "Gets given amount of persons from database."
  [amount]
  (if-let [max-person-id (get-max-person-id)]
    (jdbc/query con
                [get-persons-sql (rand-int max-person-id)]
                {:max-rows amount})
    '()))

(defn- gen-name
  "Generates vector with two elements first name and last name."
  []
  [(-> faker .name .firstName)
   (-> faker .name .lastName)])

(defn- names-seq
  "Returns lazy sequence of fist name last name vectors."
  ([] (lazy-seq (cons (gen-name) (names-seq))))
  ([n] (take n (names-seq))))

(defn- rows-num-seq
  "Returns lazy sequence of [rows-to-insert rows-already-inserted] vectors
   for given max amount of rows to insert in batch."
  ([max] (rows-num-seq 0 max))
  ([already-inserted max]
   (if (>= already-inserted max)
     []
     (let [to-insert        (min 1000 (- max already-inserted))
           already-inserted (+ already-inserted to-insert)]
       (lazy-seq (cons [to-insert already-inserted]
                       (rows-num-seq already-inserted max)))))))

(defn insert-persons!
  "Inserts given amount of generated persons to database."
  [amount]
  (doseq [[to-insert already-inserted] (rows-num-seq amount)]
    (jdbc/with-db-transaction [con con]
      (jdbc/insert-multi! con :person
                          [:first_name :last_name]
                          (names-seq to-insert)))
    (println already-inserted "rows inserted")))

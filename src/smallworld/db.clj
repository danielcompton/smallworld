(ns smallworld.db (:require [clojure.java.jdbc :as sql]
                            [clojure.pprint :as pp]
                            [smallworld.clj-postgresql.types] ; this enables the :json type
                            [smallworld.util :as util]))

(def debug? false)
(def url (util/get-env-var "DATABASE_URL"))
(def memoized-data-schema [[:id          :integer "primary key" "generated always as identity"]
                           [:request_key :text    "not null" "unique"]
                           [:data        :json]
                           [:created_at  :timestamp "default current_timestamp"]
                           [:updated_at  :timestamp "default current_timestamp"]
                           ; TODO: get "on update current_timestamp" working for :updated_at
                           ; I thought the following would work, but the db throws an error:
                           ;; [:updated_at  :timestamp "default current_timestamp" "on update current_timestamp"]
                           ])
(def tables {:coordinates :coordinates
             :users       :users})

(defn table-exists? [table-name]
  (not= 0
        (count (sql/query url (str "SELECT table_name FROM information_schema.tables where table_name = '"
                                   (name table-name) "'")))))

(table-exists? :coordinates)
(table-exists? :users)

(defn create-table [table-name]
  (if (table-exists? table-name)
    (println "table" table-name "already exists")
    (do
      (println "creating table"  table-name)
      (sql/db-do-commands url (sql/create-table-ddl (name table-name) memoized-data-schema)))))

(defn recreate-table [table-name]
  (sql/db-do-commands url (str " drop table if exists " (name table-name)))
  (create-table table-name)
  (when debug? (println "done dropping table named " table-name " (if it existed)"))
  (when debug? (println "done creating table named " table-name)))

(defn show-all [table-name]
  (println)
  (let [results (if (= table-name :users)
                  (sql/query url (str "select request_key from users"))
                  (sql/query url (str "select * from " (name table-name))))]
    (pp/pprint results)
    (when (= table-name :users) (println "not printing {:data {:friends}} because it's too long"))
    (println "count: " (count results)))
  (println))

(defn select-by-request-key [table-name request-key]
  (println "select-by-request-key  ->  request-key: " request-key "from " table-name)
  (clojure.walk/keywordize-keys
   (sql/query url [(str "select * from " (name table-name)
                        " where request_key = '" request-key "'")])))

(defn insert! [-table-name data]
  (println "-table-name:" -table-name)
  (println "       data:" data)
  (sql/insert! url -table-name data))

(defn update! [table-name request-key new-json]
  (sql/update! url table-name new-json ["request_key = ?" request-key]))

(comment
  (recreate-table :users)
  (show-all :users)

  (show-all :access_tokens)
  (select-by-request-key :access_tokens "devonzuegel")

  (recreate-table :coordinates)
  (show-all :coordinates)
  (pp/pprint (select-by-request-key :coordinates "Miami Beach"))
  (update! :coordinates "Miami Beach" {:data {:lat 25.792236328125 :lng -80.13484954833984}})
  (select-by-request-key :coordinates "spain"))

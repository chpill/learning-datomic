(ns datomic-basics.core
  (:gen-class)
  (:require [datomic.api :as datomic]))

;; This namespace contains the code used by Rich Hickey to demonstrate datomic
;; basics in this video https://www.youtube.com/watch?v=4iaIwiemqfo

;; TODO what is this `db.part` thing here?
;; part is partition
(defn tempid [] (datomic/tempid :db.part/user))

;; TODO port must be 4334?
;; what about the route at the end? is it the name of the db?
;; CFV (comment from video): change this everytime!!!!
;; NB: changed `datomic:dev` into `datomic:mem` here
(def uri "datomic:mem://plop")

(def wut-is-this (datomic/create-database uri))

(def conn (datomic/connect uri))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Define schema as data
;; Again, we encounter this `db.part` thingie...
;; NB, apparently, it's really no good to use `:email` here. We should prefer a
;; namespaced keyword, as attributes are kind of globals to the db
;; TODO use the shorthand literal for namespaced keys
;; NB to create a schema entity, we use the :db.part/db
(def schema {:db/id (datomic/tempid :db.part/db)
             :db/ident :email
             :db/valueType :db.type/string
             :db/cardinality :db.cardinality/one
             :db/unique :db.unique/identity
             :db.install/_attribute :db.part/db})


;;;;;;;;;;;;;;;;;;;;;
;; Install the schema
(def schema-ret-raw (datomic/transact conn [schema]))
(def schema-ret @schema-ret-raw)

(def new-db (:db-after schema-ret))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query that fetches all emails
;; Note that the query is *quoted*
(def query '[:find ?e ?email
             :where [?e :email ?email]])

;;;;;;;;;;;;;;;;;;;
;; Let's add a user
;; NB to create an entity, we use the db.part/user
(def fred-tx [{:db/id (datomic/tempid :db.part/user)
               :email "fred@example.com"}])

(def fred-ret @(datomic/transact conn fred-tx))

(def fred-db (:db-after fred-ret))
(def fred-id (-> fred-ret :tempids first val))
(identity fred-id)

;;;;;;;;;;;;;;;;;;;;;;
;; Let's try our query

(datomic/q query new-db)
;; => #{}
;; Yay immutability
(datomic/q query fred-db)
;; => #{[64 "fred@example.com"]}

;;;;;;;;;;;;;;;;;;;;;;;;;
;; Let's add another user
(def ethel-tx [{:db/id (datomic/tempid :db.part/user)
                :email "ethel@example.com"}])
(def ethel-ret @(datomic/transact conn ethel-tx))
(def ethel-db (:db-after ethel-ret))
(def ethel-id (-> ethel-ret :tempids first val))
(identity ethel-id)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Let's try our query again

(datomic/q query ethel-db)
(datomic/q query (datomic/history (datomic/db conn)))

(def tx-query '[:find ?e ?email ?tx ?added
                :where [?e :email ?email ?tx ?added]])

(datomic/q tx-query (datomic/history (datomic/db conn)))

;; Allright I dun goofed, to create a new entity, one must use `(datomic/tempid
;; :db.part/user)`, not `(datomic/tempid :db.part/db)`.
;; It seems that it returns the same id (always? TODO check it out), so what
;; really happened here is that, when we thought we were creating an entity with
;; email ethel, we were really retracting the email of the fred entity and
;; setting its email to ethel@example.com...


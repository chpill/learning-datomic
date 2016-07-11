(ns learning-datomic.tdd
  (:require [clojure.spec :as spec]
            [datomic.api :as datomic]))

;; This code is taken from an example of TDD with datomic by mtnygard
;; https://www.youtube.com/watch?v=JaZ1Tm6ixCY

(def schema [{:db/id                 #db/id[db.part/db]
              :db/ident              :todo/title
              :db/valueType          :db.type/string
              :db/cardinality        :db.cardinality/one
              :db/doc                "Title attached to a todo."
              :db.install/_attribute :db.part/db}
             {:db/id                 #db/id[db.part/db]
              :db/ident              :todo/completed?
              :db/valueType          :db.type/boolean
              :db/cardinality        :db.cardinality/one
              :db/doc                "True is the task has been completed."
              :db.install/_attribute :db.part/db}
             {:db/id                 #db/id[db.part/db]
              :db/ident              :todo/tag
              :db/valueType          :db.type/ref
              :db/cardinality        :db.cardinality/many
              :db/doc                "tag to help categorize todos"
              :db.install/_attribute :db.part/db}])

(def initial-tags [[:db/add #db/id[:db.part/user] :db/ident :todo.tags/chore]
                   [:db/add #db/id[:db.part/user] :db/ident :todo.tags/feat]
                   [:db/add #db/id[:db.part/user] :db/ident :todo.tags/bug]
                   [:db/add #db/id[:db.part/user] :db/ident :todo.tags/refactor]])

(defn add-todo
  "Create a new todo using the given title. Every new todo is not completed"
  [conn title]
  (datomic/transact conn [{:db/id #db/id[:db.part/user]
                           :todo/title title
                           :todo/completed? false}]))

(defn todo-by-title
  "Given a database **value**, finds todos that have that same title"
  [db title]
  (datomic/q '[:find ?e
               :in $ ?title
               :where [?e :todo/title ?title]]
            db
             title))

(defn find-all-tags
  "Given a database **value**, find all existing tags in the db"
  [db]
  (datomic/q '[:find ?id ?ident
               :where
               [?id :db/ident ?ident]
               [(namespace ?ident) ?ns]
               [(= ?ns "todo.tags")]]
             db))

(defn add-tag
  "Add a tag to the database. Validates that the tag conforms to the namespace
  scheme of tags"
  [conn tag-name]
  (assert (= "todo.tags" (namespace tag-name))
          "A tag for todos must belong to the todo.tags namespace.")
  (datomic/transact conn
                    [[:db/add #db/id[:db.part/user]
                      :db/ident tag-name]]))

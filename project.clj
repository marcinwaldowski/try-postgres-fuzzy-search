(defproject try-postgres-fuzzy-search "0.1.0-SNAPSHOT"
  :description "Trying postgres fuzzy search."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cprop "0.1.9"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [org.postgresql/postgresql "9.4.1212"]
                 [com.github.javafaker/javafaker "0.12"]
                 [mount "0.1.11"]
                 [migratus "0.8.32"]
                 [com.fzakaria/slf4j-timbre "0.3.2"]
                 [com.taoensso/timbre "4.8.0"]]
  :repl-options {:init-ns try-postgres-fuzzy-search.core
                 :init (alter-var-root #'clojure.pprint/*print-right-margin* (constantly 110))})

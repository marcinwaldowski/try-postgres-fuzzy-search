Proof of concept mini project which shows combined [PostgreSQL 9.6](https://www.postgresql.org) features ([full text search](https://www.postgresql.org/docs/9.6/static/textsearch.html) and [trigrams](https://www.postgresql.org/docs/9.6/static/pgtrgm.html)) to mimic some Google-like search capabilities typically achievable with [Elasticsearch](https://www.elastic.co/products/elasticsearch).

Implemented as a bunch of [Clojure](https://clojure.org/) functions which allow to:
- connect to PostgreSQL,
- create database [schema](resources/migrations/20161229122220-init.down.sql) (tables and triggers),
- populate database with random data,
- perform search.

## Usage

1. Install Java 8.

2. Install [Leiningen](http://leiningen.org/#install).

3. Create PostgreSQL database and user which is owner of it. The database needs to have pg_trgm extension.

  ```sql
  CREATE EXTENSION pg_trgm;
  ```

4. Clone this repository.

5. Edit file [`/resources/config.edn`](/resources/config.edn) to set:
  - database name,
  - database host,
  - database port,
  - database user,
  - database password.


6. `cd` to cloned repository and execute:

  ```sh
  lein repl
  ```

  The output should be:

  ```sh
  nREPL server started on port 35213 on host 127.0.0.1 - nrepl://127.0.0.1:35213
  REPL-y 0.3.7, nREPL 0.2.12
  Clojure 1.8.0
  Java HotSpot(TM) 64-Bit Server VM 1.8.0_111-b14
      Docs: (doc function-name-here)
            (find-doc "part-of-name-here")
    Source: (source function-name-here)
   Javadoc: (javadoc java-object-or-class-here)
      Exit: Control+D or (exit) or (quit)
   Results: Stored in vars *1, *2, *3, an exception in *e

   try-postgres-fuzzy-search.core=>
   ```

7. Create database schema with:

  ```clojure
  (create-schema!)
  ```

8. Connect to database with:

  ```clojure
  (connect!)
  ```

9. Populate database with one million persons with:

  ```clojure
  (insert-persons! 1000000)
  ```
10. Get random data from database.

  ```clojure
  (pprint (get-random-persons 5))
  ```

  The example output:

  ```sh
  ({:id 17820, :first_name "Barney", :last_name "Gislason"}
   {:id 17821, :first_name "Destiny", :last_name "Rogahn"}
   {:id 17822, :first_name "Allison", :last_name "Altenwerth"}
   {:id 17823, :first_name "Willie", :last_name "Rippin"}
   {:id 17824, :first_name "Pink", :last_name "Crona"})
  nil
  ```

11. Perform search:

  - search without typos:

      ```clojure
      (pprint (search-person "Barney" "Gislason"))
      ```

  - search with typos and with beginning of word:

      ```clojure
      (pprint (search-person "Berney" "Gisl"))
      ```
  - search by only one word (with typo):

      ```clojure
      (pprint (search-person "Gisleson"))
      ```
12. Disconnect from database.

  ```clojure
  (disconnect!)
  ```

13. (Optionally) Drop database schema.

  ```clojure
  (drop-schema!)
  ```

14. Exit REPL.

  ```sh
  exit
  ```

## License

Eclipse Public License version 1.0.

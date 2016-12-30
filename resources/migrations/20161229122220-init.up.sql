CREATE TABLE person (
  id SERIAL PRIMARY KEY,
  first_name TEXT NOT NULL,
  last_name TEXT NOT NULL,
  tsv tsvector
);
--;;

CREATE INDEX idx_person_tsv ON person USING GIN (tsv);
--;;

CREATE TABLE word (
   word TEXT PRIMARY KEY
);
--;;

CREATE INDEX word_idx ON word USING GIN (word gin_trgm_ops);
--;;

CREATE TRIGGER tr_person_update_tsv BEFORE INSERT OR UPDATE
ON person FOR EACH ROW EXECUTE PROCEDURE
tsvector_update_trigger(tsv, 'pg_catalog.simple', first_name, last_name);
--;;

CREATE FUNCTION tr_word_update() RETURNS TRIGGER AS $$
  BEGIN
    INSERT INTO word (word)
    SELECT unnest(tsvector_to_array(NEW.tsv))
    ON CONFLICT DO NOTHING;
    RETURN NEW;
  END;
$$ LANGUAGE plpgsql;
--;;

CREATE TRIGGER tr_word_update BEFORE INSERT OR UPDATE
ON person FOR EACH ROW EXECUTE PROCEDURE
tr_word_update();

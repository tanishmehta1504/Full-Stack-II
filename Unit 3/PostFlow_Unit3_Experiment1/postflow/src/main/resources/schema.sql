CREATE TABLE IF NOT EXISTS events (
 id VARCHAR(36) PRIMARY KEY, title VARCHAR(120) NOT NULL, content VARCHAR(2000) NOT NULL,
 channel VARCHAR(20) NOT NULL, failure_mode VARCHAR(20) NOT NULL, generation INT NOT NULL DEFAULT 0,
 status VARCHAR(20) NOT NULL, attempts INT NOT NULL DEFAULT 0, duplicates INT NOT NULL DEFAULT 0,
 scheduled_at BIGINT NOT NULL, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL,
 last_error VARCHAR(500)
);
CREATE TABLE IF NOT EXISTS outbox (
 id VARCHAR(36) PRIMARY KEY, event_id VARCHAR(36) NOT NULL REFERENCES events(id),
 payload VARCHAR(10000) NOT NULL, due_at BIGINT NOT NULL, sent_at BIGINT, attempts INT NOT NULL DEFAULT 0,
 next_try BIGINT NOT NULL DEFAULT 0, last_error VARCHAR(500)
);
CREATE TABLE IF NOT EXISTS processed_events (event_id VARCHAR(36) PRIMARY KEY, processed_at BIGINT NOT NULL);
CREATE TABLE IF NOT EXISTS publications (
 event_id VARCHAR(36) PRIMARY KEY REFERENCES events(id), title VARCHAR(120) NOT NULL,
 content VARCHAR(2000) NOT NULL, channel VARCHAR(20) NOT NULL, published_at BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS activity (
 id VARCHAR(36) PRIMARY KEY, event_id VARCHAR(36), kind VARCHAR(30) NOT NULL,
 message VARCHAR(500) NOT NULL, created_at BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS dead_letters (
 id VARCHAR(36) PRIMARY KEY, event_id VARCHAR(36), generation INT, payload VARCHAR(20000) NOT NULL,
 reason VARCHAR(1000) NOT NULL, source_topic VARCHAR(200) NOT NULL, source_partition INT NOT NULL,
 source_offset BIGINT NOT NULL, created_at BIGINT NOT NULL, replayed BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS outbox_pending ON outbox(sent_at,due_at,next_try);
CREATE INDEX IF NOT EXISTS activity_event ON activity(event_id,created_at);

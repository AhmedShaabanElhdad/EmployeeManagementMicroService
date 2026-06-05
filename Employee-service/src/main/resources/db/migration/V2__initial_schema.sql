-- Add missing indexes to outbox table
CREATE INDEX idx_outbox_processed
ON outbox(processed);

CREATE INDEX idx_outbox_processed_created
ON outbox(processed, created_at);

CREATE TABLE processed_customer_sync_events (
    event_id UUID PRIMARY KEY,
    source_customer_id VARCHAR(18) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

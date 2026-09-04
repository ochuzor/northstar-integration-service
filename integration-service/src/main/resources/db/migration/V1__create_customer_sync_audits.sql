CREATE TABLE customer_sync_audits (
    correlation_id UUID PRIMARY KEY,
    event_id UUID,
    source_customer_id VARCHAR(18) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    failure_category VARCHAR(64),

    CONSTRAINT uk_customer_sync_audits_event_id UNIQUE (event_id),
    CONSTRAINT ck_customer_sync_audits_status
        CHECK (status IN ('INITIATED', 'PUBLISHED', 'PUBLICATION_FAILED'))
);

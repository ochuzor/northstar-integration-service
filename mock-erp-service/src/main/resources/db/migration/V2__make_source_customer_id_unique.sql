ALTER TABLE erp_customers
    ADD CONSTRAINT uk_erp_customers_source_customer_id UNIQUE (source_customer_id);

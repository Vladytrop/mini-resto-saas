CREATE TABLE IF NOT EXISTS payments (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    provider_code VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255),
    idempotency_key VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_payments_order_tenant
    ON payments(order_id, tenant_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON payments TO resto_app;
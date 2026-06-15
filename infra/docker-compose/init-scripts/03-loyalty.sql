CREATE TABLE IF NOT EXISTS loyalty_accounts (
                                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    points INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

CREATE UNIQUE INDEX IF NOT EXISTS idx_loyalty_account_tenant_customer
    ON loyalty_accounts(tenant_id, customer_name);

ALTER TABLE loyalty_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE loyalty_accounts FORCE ROW LEVEL SECURITY;

CREATE POLICY loyalty_accounts_policy ON loyalty_accounts
    FOR ALL
    USING (tenant_id::text = current_setting('app.current_tenant', true)::text)
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true)::text);

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO resto_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO resto_app;
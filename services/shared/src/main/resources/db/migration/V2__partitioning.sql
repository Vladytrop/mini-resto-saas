-- create order_partitioned table
CREATE TABLE IF NOT EXISTS orders_partitioned (
                                                  id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    customer_name VARCHAR(255),
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
    ) PARTITION BY RANGE (created_at);

-- create partitions (current + 3 months ahead)
CREATE TABLE IF NOT EXISTS orders_2026_06 PARTITION OF orders_partitioned
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE IF NOT EXISTS orders_2026_07 PARTITION OF orders_partitioned
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE IF NOT EXISTS orders_2026_08 PARTITION OF orders_partitioned
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

-- migrate data from old table
INSERT INTO orders_partitioned (id, tenant_id, customer_name, total_amount, status, created_at)
SELECT id, tenant_id, customer_name, total_amount, status, created_at FROM orders;

-- drop old table
DROP TABLE IF EXISTS orders;

ALTER TABLE orders_partitioned RENAME TO orders;

-- indexes and RLS
CREATE INDEX IF NOT EXISTS idx_orders_tenant ON orders(tenant_id);

ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS orders_policy ON orders;
CREATE POLICY orders_policy ON orders
    FOR ALL
    USING (tenant_id::text = current_setting('app.current_tenant', true)::text)
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true)::text);
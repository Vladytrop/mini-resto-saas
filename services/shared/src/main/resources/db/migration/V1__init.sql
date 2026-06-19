create extension if not exists "pgcrypto";
-- Create Orders Table
create table if not exists orders (
    id UUID primary key default gen_random_uuid(),
    tenant_id varchar(255) not null,
    customer_name varchar(255),
    total_amount decimal(10,2) not null,
    status varchar(255) not null,
    created_at timestamp not null default now()
);

alter table orders enable row level security;
alter table orders force row level security;

drop policy if exists orders_policy on orders;
create policy orders_policy on orders
    for all
    using (tenant_id::text = current_setting('app.current_tenant', true)::text)
    with check (tenant_id::text = current_setting('app.current_tenant', true)::text);

-- Create loyalty_accounts table
create table if not exists loyalty_accounts (
    id UUID primary key default gen_random_uuid(),
    tenant_id varchar(255) not null,
    customer_name varchar(255) not null,
    points integer not null default 0,
    created_at timestamp not null default now()
);

create unique index if not exists idx_loyalty_account_tenant_customer on loyalty_accounts(tenant_id, customer_name);

alter table loyalty_accounts enable row level security;
alter table loyalty_accounts force row level security;

-- Create payments table
create table if not exists payments (
    id UUID primary key default gen_random_uuid(),
    order_id UUID not null,
    tenant_id varchar(255) not null,
    provider_code varchar(255) not null,
    amount decimal(10,2) not null,
    status varchar(255) not null,
    transaction_id varchar(255),
    idempotency_key varchar(255),
    created_at timestamp not null default NOW()
);

create index if not exists idx_payments_order_tenant on payments(order_id, tenant_id);

alter table payments enable row level security;
alter table payments force row level security;

drop policy if exists payments_policy on payments;

create policy payments_policy on payments
    for all
    using (tenant_id::text = current_setting('app.current_tenant', true)::text)
    with check (tenant_id::text = current_setting('app.current_tenant', true)::text);
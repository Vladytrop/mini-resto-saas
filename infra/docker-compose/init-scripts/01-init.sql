create table orders (
    id uuid primary key default gen_random_uuid(),
    tenant_id varchar(255) not null,
    customer_name varchar(255) not null,
    total_amount decimal(10,2) not null,
    status varchar(255) not null,
    created_at timestamp not null
);

insert into orders (tenant_id, customer_name, total_amount, status, created_at)
    values ('tenant-1', 'John Doe', 100.00, 'CREATED', now()),
           ('tenant-2', 'Mark Diy', 200.00, 'CREATED', now()),
           ('tenant-3', 'Mike Loe', 300.00, 'CREATED', now());

alter table orders enable row level security;

create policy orders_policy on orders
       for all
       using (tenant_id::text = current_setting('app.current_tenant', true)::text)
       with check (tenant_id::text = current_setting('app.current_tenant', true)::text);

alter table orders force row level security;

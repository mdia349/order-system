create table shipment (
    id uuid primary key,
    order_id uuid not null,
    status varchar(32) not null,
    created_at timestamptz not null default now()
);

create index idx_shipment_order_id on shipment(order_id);

create table processed_event (
    event_id uuid primary key,
    processed_at timestamptz not null default now()
);
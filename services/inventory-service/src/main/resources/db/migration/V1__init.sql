create table inventory_item (
                                sku varchar(64) primary key,
                                quantity int not null,
                                updated_at timestamptz not null default now()
);

create table processed_event (
                                 event_id uuid primary key,
                                 processed_at timestamptz not null default now()
);

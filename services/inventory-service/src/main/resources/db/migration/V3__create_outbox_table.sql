create table outbox_event (
                              id uuid primary key,
                              aggregate_type varchar(64) not null,
                              aggregate_id uuid not null,
                              event_type varchar(128) not null,
                              payload text not null,
                              occurred_at timestamptz not null,
                              published_at timestamptz null
);

create index idx_inventory_outbox_unpublished on outbox_event(published_at) where published_at is null;
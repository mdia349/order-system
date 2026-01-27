create table orders (
                        id uuid primary key,
                        user_id uuid not null,
                        status varchar(32) not null,
                        created_at timestamptz not null default now(),
                        updated_at timestamptz not null default now()
);

create table outbox_event (
                              id uuid primary key,
                              aggregate_type varchar(64) not null,
                              aggregate_id uuid not null,
                              event_type varchar(128) not null,
                              payload jsonb not null,
                              occurred_at timestamptz not null,
                              published_at timestamptz null
);

create index idx_outbox_unpublished on outbox_event(published_at) where published_at is null;
create index idx_outbox_occurred on outbox_event(occurred_at);

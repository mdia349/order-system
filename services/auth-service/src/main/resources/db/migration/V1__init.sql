create table app_user (
                          id uuid primary key,
                          email varchar(255) not null unique,
                          password_hash varchar(255) not null,
                          roles varchar(255) not null,
                          created_at timestamptz not null default now()
);

create table refresh_token (
                               id uuid primary key,
                               user_id uuid not null references app_user(id) on delete cascade,
                               token_hash varchar(255) not null,
                               expires_at timestamptz not null,
                               revoked_at timestamptz null,
                               created_at timestamptz not null default now()
);

create index idx_refresh_token_user on refresh_token(user_id);
create index idx_refresh_token_hash on refresh_token(token_hash);

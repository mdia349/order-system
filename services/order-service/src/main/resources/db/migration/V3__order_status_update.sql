alter table orders
alter column status type varchar(50),
alter column status set not null,
alter column status set default 'CREATED';
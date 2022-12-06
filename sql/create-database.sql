--Setup database
DROP DATABASE IF EXISTS vafs_backoffice;
CREATE DATABASE vafs_backoffice;

DROP DATABASE IF EXISTS vafs_cqrs;
CREATE DATABASE vafs_cqrs;
\c vafs_cqrs;


DROP TABLE IF EXISTS public.counter;

CREATE TABLE IF NOT EXISTS public.counter (
     id                    VARCHAR(23) NOT NULL,
     route                 VARCHAR(23)NOT NULL UNIQUE,
     count                 BIGINT NOT NULL,
     write_side_offset     BIGINT NOT NULL,
     PRIMARY KEY(id, route)
);

INSERT INTO public.counter VALUES('ProcessCallApp', 'default', 0, 0);

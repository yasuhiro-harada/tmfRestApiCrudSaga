set -e

psql -U postgres << EOSQL

CREATE TABLE products (
    product_no integer,
    name text,
    price numeric,
    createdate timestamp
);
ALTER TABLE products ADD CONSTRAINT products_pkey PRIMARY KEY(product_no);

INSERT INTO products (product_no, name, price, createdate ) VALUES
    (1, 'Cheese', 9.99, '2021-4-1 20:38:40'),
    (2, 'Bread', 1.99, '2001-02-16 20:38:40'),
    (3, 'Milk', 2.99, '2021-04-16 10:18:10');

EOSQL
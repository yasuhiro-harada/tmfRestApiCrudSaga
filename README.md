# tmfRestApiCrud
 java CRUD API compliant with TM Forum RestAPI specification
## Overview
 This sample can be used to get an idea of how to implement CRUD in compliance with TMF630(TM Forum RestAPI specification).
## Target 
 This sample is intended for developers who have never implemented the code according to TMF630.
## Scope
 This sample is for creating an API that conforms to the TMF630 specifications. It has not been tested and is not guaranteed to work.
It supports the basic REST-APIs that are often used.  
Only the following chapters in "TMF630_REST_API_Design_Guidelines_Part_1_v4.0.1.pdf" are supported.  
4. Query Resources Patterns  
5. Modify resource patterns  
6. Create Resource Patterns  
7. Delete Resource Pattern    
Also, among the above, "Regular expression search", "Various OR conditional search", and "Multi-line update" are not supported.
## Compliant specifications
[TMF630_REST_API_Design_Guidelines_Part_1_v4.0.1.pdf](https://www.tmforum.org/resources/specification/tmf630-rest-api-design-guidelines-part-1-r14-5-1/)
## Sample data in database
The following I/O images are posted on the assumption that the following sample data has been saved.
### Oracle
```
CREATE TABLE products (
    product_no NUMBER,
    name VARCHAR2(128),
    price BINARY_DOUBLE,
    createdate DATE,
    primary key( product_no)
);

INSERT INTO products (product_no, name, price, createdate) VALUES (1, 'Cheese', 9.99, TO_DATE('2021-4-1 20:38:40', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO products (product_no, name, price, createdate) VALUES (2, 'Bread', 1.99, TO_DATE('2001-02-16 20:38:40', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO products (product_no, name, price, createdate) VALUES (3, 'Milk', 2.99, TO_DATE('2021/4/16 10:18:10', 'YYYY-MM-DD HH24:MI:SS'));
```
### postgresql
```
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
```

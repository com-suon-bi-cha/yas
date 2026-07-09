ALTER TABLE IF EXISTS "order"
ALTER COLUMN customer_id TYPE VARCHAR(255)
USING customer_id::text;

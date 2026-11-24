DO $$
DECLARE
    status_type VARCHAR;
BEGIN
    SELECT data_type
    INTO status_type
    FROM information_schema.columns
    WHERE table_name = 'orders'
      AND column_name = 'status';

    IF status_type = 'integer' THEN
        ALTER TABLE orders
            ALTER COLUMN status TYPE VARCHAR(32)
            USING CASE status
                WHEN 0 THEN 'PENDING'
                WHEN 1 THEN 'CONFIRMED'
                WHEN 2 THEN 'PREPARING'
                WHEN 3 THEN 'FOR_PICKUP'
                WHEN 4 THEN 'RECEIVED'
                WHEN 5 THEN 'CANCELLED'
                ELSE NULL
            END;
    END IF;
END $$;

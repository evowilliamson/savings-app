-- ============================================================================
-- Khaohom's Savings Tracker - PostgreSQL Database Schema
-- ============================================================================
-- 
-- This schema creates the database structure for tracking savings across
-- multiple assets (Bitcoin, Gold, USD, etc.) with transaction history.
--
-- Usage:
-- 1. In Railway PostgreSQL console, go to "Data" → "Query"
-- 2. Copy and paste this entire file
-- 3. Click "Run Query"
-- 4. Verify success message and seed data
--
-- ============================================================================

-- Drop existing tables if they exist (be careful in production!)
DROP TABLE IF EXISTS savings_transactions CASCADE;
DROP TABLE IF EXISTS assets CASCADE;

-- ============================================================================
-- ASSETS TABLE
-- ============================================================================
-- Stores information about different asset types (BTC, GOLD, USD, etc.)
-- with their CAGR (Compound Annual Growth Rate) for future projections

CREATE TABLE assets (
    id                  SERIAL PRIMARY KEY,
    asset_name          VARCHAR(50) UNIQUE NOT NULL,  -- 'BTC', 'GOLD', 'USD', etc.
    display_name        VARCHAR(100) NOT NULL,        -- 'Bitcoin', 'Gold', 'US Dollar'
    cagr_percent        NUMERIC DEFAULT 0,            -- CAGR for prognosis (e.g., 25.0 for 25%)
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast lookups by asset name
CREATE INDEX idx_assets_name ON assets (asset_name);

-- Add comment to table
COMMENT ON TABLE assets IS 'Asset types with their CAGR rates for future value projections';
COMMENT ON COLUMN assets.asset_name IS 'Short asset identifier (must match Google Sheet Asset column)';
COMMENT ON COLUMN assets.display_name IS 'Human-readable asset name for display';
COMMENT ON COLUMN assets.cagr_percent IS 'Compound Annual Growth Rate percentage for projections';

-- ============================================================================
-- SAVINGS TRANSACTIONS TABLE
-- ============================================================================
-- Stores all savings transactions synced from Google Sheet

CREATE TABLE savings_transactions (
    id                  SERIAL PRIMARY KEY,
    transaction_date    DATE NOT NULL,
    amount              NUMERIC NOT NULL,                      -- Amount of asset purchased
    asset_id            INTEGER NOT NULL REFERENCES assets(id) ON DELETE RESTRICT,
    price               NUMERIC,                               -- Asset price at purchase (for BTC/GOLD)
    usd_value_at_tx     NUMERIC NOT NULL,                      -- USD value at transaction time
    usd_cumulative      NUMERIC NOT NULL,                      -- Cumulative USD invested
    reason              TEXT,                                  -- Reason for transaction
    status              VARCHAR(20),                           -- 'paid' or 'not paid'
    usdthb_rate         NUMERIC NOT NULL,                      -- USD/THB exchange rate at transaction
    synced_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Prevent duplicate transactions
    UNIQUE (transaction_date, amount, asset_id, usd_value_at_tx)
);

-- Indexes for efficient queries
CREATE INDEX idx_savings_date ON savings_transactions (transaction_date DESC);
CREATE INDEX idx_savings_asset ON savings_transactions (asset_id);
CREATE INDEX idx_savings_status ON savings_transactions (status);

-- Add comments
COMMENT ON TABLE savings_transactions IS 'All savings transactions synced from Google Sheet';
COMMENT ON COLUMN savings_transactions.transaction_date IS 'Date of transaction (from Google Sheet column A)';
COMMENT ON COLUMN savings_transactions.amount IS 'Amount purchased (from Google Sheet column B)';
COMMENT ON COLUMN savings_transactions.price IS 'Asset price at purchase time (column C, optional)';
COMMENT ON COLUMN savings_transactions.usd_value_at_tx IS 'USD value at transaction (column D)';
COMMENT ON COLUMN savings_transactions.usd_cumulative IS 'Cumulative USD invested (column E)';
COMMENT ON COLUMN savings_transactions.reason IS 'Transaction reason (column G)';
COMMENT ON COLUMN savings_transactions.status IS 'Payment status: paid or not paid (column H)';
COMMENT ON COLUMN savings_transactions.usdthb_rate IS 'USD/THB exchange rate (column I)';

-- ============================================================================
-- SEED DATA
-- ============================================================================
-- Initial asset types with default CAGR values

INSERT INTO assets (asset_name, display_name, cagr_percent) VALUES
('BTC', 'Bitcoin', 25.0),
('GOLD', 'Gold', 15.0),
('USD', 'US Dollar', 5.0);

-- ============================================================================
-- VERIFICATION
-- ============================================================================
-- Verify the schema was created successfully

DO $$
BEGIN
    -- Check if tables exist
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'assets') AND
       EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'savings_transactions') THEN
        RAISE NOTICE 'âœ… Database schema created successfully!';
        RAISE NOTICE '';
        RAISE NOTICE 'Tables created:';
        RAISE NOTICE '  - assets (% rows)', (SELECT COUNT(*) FROM assets);
        RAISE NOTICE '  - savings_transactions (% rows)', (SELECT COUNT(*) FROM savings_transactions);
        RAISE NOTICE '';
        RAISE NOTICE 'Seed data inserted:';
        RAISE NOTICE '  - BTC (Bitcoin) - CAGR: 25%%';
        RAISE NOTICE '  - GOLD (Gold) - CAGR: 15%%';
        RAISE NOTICE '  - USD (US Dollar) - CAGR: 5%%';
        RAISE NOTICE '';
        RAISE NOTICE 'âœ… Ready to sync data from Google Sheet!';
    ELSE
        RAISE EXCEPTION 'â�Œ Failed to create tables';
    END IF;
END $$;

-- ============================================================================
-- OPTIONAL: HELPER QUERIES
-- ============================================================================
-- Uncomment these to run useful queries after setup

-- View all assets
-- SELECT * FROM assets ORDER BY asset_name;

-- View recent transactions
-- SELECT 
--     t.id,
--     t.transaction_date,
--     a.display_name as asset,
--     t.amount,
--     t.usd_value_at_tx,
--     t.status
-- FROM savings_transactions t
-- JOIN assets a ON t.asset_id = a.id
-- ORDER BY t.transaction_date DESC
-- LIMIT 10;

-- View portfolio summary
-- SELECT 
--     a.display_name as asset,
--     SUM(t.amount) as total_amount,
--     COUNT(*) as num_transactions,
--     MAX(t.transaction_date) as last_transaction
-- FROM savings_transactions t
-- JOIN assets a ON t.asset_id = a.id
-- GROUP BY a.display_name
-- ORDER BY a.display_name;

-- ============================================================================
-- MAINTENANCE QUERIES
-- ============================================================================

-- Add a new asset type
-- INSERT INTO assets (asset_name, display_name, cagr_percent) 
-- VALUES ('ETH', 'Ethereum', 30.0);

-- Update CAGR for an asset
-- UPDATE assets SET cagr_percent = 50.0 WHERE asset_name = 'BTC';

-- View all indexes
-- SELECT 
--     tablename, 
--     indexname, 
--     indexdef 
-- FROM pg_indexes 
-- WHERE schemaname = 'public'
-- ORDER BY tablename, indexname;

-- ============================================================================
-- END OF SCHEMA
-- ============================================================================
import express from 'express';
import cors from 'cors';
import pg from 'pg';
import dotenv from 'dotenv';
import rateLimit from 'express-rate-limit';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

// Database connection
const pool = new pg.Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
});

// Test database connection
pool.query('SELECT NOW()', (err, res) => {
  if (err) {
    console.error('Database connection error:', err);
  } else {
    console.log('Database connected successfully at:', res.rows[0].now);
  }
});

// Middleware
app.use(cors({
  origin: process.env.FRONTEND_URL || '*',
  credentials: true
}));
app.use(express.json());

// Rate limiting
const postLimiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 10,
  message: 'Too many sync requests, please try again later.'
});

const getLimiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 60,
  message: 'Too many requests, please try again later.'
});

// Routes

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// GET /api/assets - Get all assets with CAGR values
app.get('/api/assets', getLimiter, async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT id, asset_name, display_name, cagr_percent, created_at, updated_at
      FROM assets
      ORDER BY asset_name
    `);
    
    res.json(result.rows);
  } catch (error) {
    console.error('Error fetching assets:', error);
    res.status(500).json({ error: 'Failed to fetch assets' });
  }
});

// GET /api/payments - Get all transactions with asset details
app.get('/api/payments', getLimiter, async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT 
        st.id,
        st.transaction_date,
        st.amount,
        a.asset_name,
        a.display_name as asset_display_name,
        st.thb_price,
        st.usd_value_at_tx,
        st.usd_cumulative,
        st.reason,
        st.status,
        st.usdthb_rate,
        st.synced_at,
        st.updated_at
      FROM savings_transactions st
      JOIN assets a ON st.asset_id = a.id
      ORDER BY st.transaction_date DESC
    `);
    
    res.json(result.rows);
  } catch (error) {
    console.error('Error fetching payments:', error);
    res.status(500).json({ error: 'Failed to fetch payments' });
  }
});

// GET /api/current-exchange-rate - Get current USD/THB rate
app.get('/api/current-exchange-rate', getLimiter, async (req, res) => {
  try {
    // Fetch from exchangerate-api.com (free tier)
    const response = await fetch('https://api.exchangerate-api.com/v4/latest/USD');
    const data = await response.json();
    
    if (data && data.rates && data.rates.THB) {
      res.json({
        rate: data.rates.THB,
        timestamp: new Date().toISOString()
      });
    } else {
      // Fallback rate if API fails
      res.json({
        rate: 33.5,
        timestamp: new Date().toISOString(),
        note: 'Fallback rate - API unavailable'
      });
    }
  } catch (error) {
    console.error('Error fetching exchange rate:', error);
    // Return fallback rate
    res.json({
      rate: 33.5,
      timestamp: new Date().toISOString(),
      note: 'Fallback rate - API error'
    });
  }
});

// POST /api/sync-payments - Sync payments from Google Sheets
app.post('/api/sync-payments', postLimiter, async (req, res) => {
  try {
    const { password, payments } = req.body;

    // Validate password
    if (!password || password !== process.env.SYNC_PASSWORD) {
      return res.status(401).json({ error: 'Invalid password' });
    }

    // Validate payments array
    if (!payments || !Array.isArray(payments) || payments.length === 0) {
      return res.status(400).json({ error: 'Invalid payments data' });
    }

    let inserted = 0;
    let updated = 0;
    const errors = [];

    // Start transaction
    const client = await pool.connect();
    
    try {
      await client.query('BEGIN');

      for (let i = 0; i < payments.length; i++) {
        const payment = payments[i];
        
        // Validate required fields
        if (!payment.date || !payment.amount || !payment.asset || 
            payment.usd_value === undefined || payment.usd_cum === undefined || 
            !payment.usdthb_rate) {
          errors.push(`Row ${i + 1}: Missing required fields`);
          continue;
        }

        // Look up asset_id
        const assetResult = await client.query(
          'SELECT id FROM assets WHERE asset_name = $1',
          [payment.asset.toUpperCase()]
        );

        if (assetResult.rows.length === 0) {
          errors.push(`Row ${i + 1}: Asset '${payment.asset}' not found in database`);
          continue;
        }

        const assetId = assetResult.rows[0].id;

        // Upsert payment
        const upsertQuery = `
          INSERT INTO savings_transactions (
            transaction_date, 
            amount, 
            asset_id, 
            thb_price, 
            usd_value_at_tx, 
            usd_cumulative, 
            reason, 
            status, 
            usdthb_rate
          ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
          ON CONFLICT (transaction_date, amount, asset_id, usd_value_at_tx) 
          DO UPDATE SET
            thb_price = EXCLUDED.thb_price,
            usd_cumulative = EXCLUDED.usd_cumulative,
            reason = EXCLUDED.reason,
            status = EXCLUDED.status,
            usdthb_rate = EXCLUDED.usdthb_rate,
            updated_at = CURRENT_TIMESTAMP
          RETURNING (xmax = 0) AS inserted
        `;

        const result = await client.query(upsertQuery, [
          payment.date,
          payment.amount,
          assetId,
          payment.thb_price || null,
          payment.usd_value,
          payment.usd_cum,
          payment.reason || null,
          payment.status || 'not paid',
          payment.usdthb_rate
        ]);

        if (result.rows[0].inserted) {
          inserted++;
        } else {
          updated++;
        }
      }

      await client.query('COMMIT');

      // If there were errors but some succeeded, return partial success
      if (errors.length > 0) {
        return res.status(207).json({ 
          message: `Synced ${inserted + updated} payments with ${errors.length} errors`,
          inserted,
          updated,
          errors
        });
      }

      res.json({ 
        message: `Synced ${inserted + updated} payments successfully`,
        inserted,
        updated
      });

    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }

  } catch (error) {
    console.error('Error syncing payments:', error);
    res.status(500).json({ error: 'Failed to sync payments', details: error.message });
  }
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({ error: 'Not found' });
});

// Error handler
app.use((err, req, res, next) => {
  console.error('Server error:', err);
  res.status(500).json({ error: 'Internal server error' });
});

// Start server
app.listen(PORT, () => {
  console.log(`Khaohom's Savings API running on port ${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('SIGTERM signal received: closing HTTP server');
  pool.end(() => {
    console.log('Database pool closed');
    process.exit(0);
  });
});
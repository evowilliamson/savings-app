# System Architecture

Complete architecture documentation for Khaohom's Savings Tracker.

## High-Level Overview

```
┌──────────────────────────────────────────────────────────────┐
│                        USER                                  │
│                         👤                                   │
└───────────┬──────────────────────────┬───────────────────────┘
            │                          │
            │ Manual Entry             │ View & Interact
            ↓                          ↓
    ┌───────────────┐          ┌──────────────┐
    │ Google Sheet  │          │   Frontend   │
    │  (H1: Pass)   │          │  Mobile App  │
    │  Row 29+      │          │  (4 Screens) │
    └───────┬───────┘          └──────┬───────┘
            │                         │
            │ Apps Script             │ REST API
            │ (Manual Sync)           │ (Auto)
            ↓                         ↓
    ┌───────────────────────────────────────┐
    │         Backend API (Railway)         │
    │  ┌─────────────────────────────────┐  │
    │  │  Express Server (Node.js)       │  │
    │  │  • POST /api/sync-payments      │  │
    │  │  • GET  /api/payments           │  │
    │  │  • GET  /api/assets             │  │
    │  │  • GET  /api/current-ex-rate    │  │
    │  └─────────────────────────────────┘  │
    └───────────────┬───────────────────────┘
                    │
                    │ SQL Queries
                    ↓
    ┌───────────────────────────────────────┐
    │    PostgreSQL (Railway)               │
    │  ┌─────────────────────────────────┐  │
    │  │  Tables:                        │  │
    │  │  • assets (BTC, GOLD, USD)      │  │
    │  │  • savings_transactions         │  │
    │  └─────────────────────────────────┘  │
    └───────────────────────────────────────┘
            ↑
            │ Current Prices
            │
    ┌───────────────────┐
    │  External APIs    │
    │  • CoinGecko      │
    │  • ExchangeRate   │
    └───────────────────┘
```

## Component Details

### 1. Google Sheet (Data Source)

**Purpose**: Single source of truth for transaction data

**Structure**:
- Tab: "Kaohom Savings Account"
- Cell H1: Sync password
- Row 29+: Transaction records

**Columns**:
| Col | Field | Type | Example |
|-----|-------|------|---------|
| A | Date | Date | 2025-01-15 |
| B | Amount | Number/Text | 0.5 or ฿0.01 |
| C | THBPrice | Number | 95400 |
| D | USD | Number | 1315 |
| E | USD Cum. | Number | 1315 |
| F | Asset | Text | GOLD |
| G | Reason | Text | Birthday |
| H | Note | Text | paid |
| I | USDTHB | Number | 31.2 |

**Access**: Manual data entry only

### 2. Google Apps Script (Sync Layer)

**Purpose**: Bridge between Google Sheets and Backend API

**Trigger**: Manual (custom menu)

**Functions**:
- `onOpen()` - Creates custom menu
- `syncPayments()` - Main sync function
- `testConnection()` - Health check

**Process**:
1. Read password from H1
2. Read all rows from A29 onwards
3. Validate and parse each row
4. Build JSON payload
5. POST to backend API
6. Display success/error message

**Authentication**: Password in request body

**Error Handling**: User-friendly alerts

### 3. Backend API (Railway)

**Tech Stack**:
- Runtime: Node.js 18+
- Framework: Express 4.18
- Database Client: node-postgres (pg)
- Security: express-rate-limit, cors

**Endpoints**:

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/health` | No | Health check |
| GET | `/api/assets` | No | Get all assets with CAGR |
| GET | `/api/payments` | No | Get all transactions |
| GET | `/api/current-exchange-rate` | No | Get USD/THB rate |
| POST | `/api/sync-payments` | Yes | Sync from Google Sheet |

**Rate Limits**:
- POST: 10 requests/minute
- GET: 60 requests/minute

**CORS**: Configured for specific frontend domain

**Error Handling**:
- 400: Bad request (validation errors)
- 401: Unauthorized (invalid password)
- 500: Server error
- 207: Multi-status (partial success)

### 4. PostgreSQL Database (Railway)

**Schema**:

**Table: assets**
```sql
CREATE TABLE assets (
    id                  SERIAL PRIMARY KEY,
    asset_name          VARCHAR(50) UNIQUE NOT NULL,
    display_name        VARCHAR(100) NOT NULL,
    cagr_percent        NUMERIC DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

**Table: savings_transactions**
```sql
CREATE TABLE savings_transactions (
    id                  SERIAL PRIMARY KEY,
    transaction_date    DATE NOT NULL,
    amount              NUMERIC NOT NULL,
    asset_id            INTEGER NOT NULL REFERENCES assets(id),
    thb_price           NUMERIC,
    usd_value_at_tx     NUMERIC NOT NULL,
    usd_cumulative      NUMERIC NOT NULL,
    reason              TEXT,
    status              VARCHAR(20),
    usdthb_rate         NUMERIC NOT NULL,
    synced_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (transaction_date, amount, asset_id, usd_value_at_tx)
);
```

**Indexes**:
- `idx_assets_name` on `assets(asset_name)`
- `idx_savings_date` on `savings_transactions(transaction_date DESC)`
- `idx_savings_asset` on `savings_transactions(asset_id)`

**Seed Data**:
- BTC (Bitcoin) - 25% CAGR
- GOLD (Gold) - 15% CAGR
- USD (US Dollar) - 5% CAGR

### 5. Frontend Application (Railway)

**Tech Stack**:
- Build Tool: Vite 5.0
- UI: Vanilla JavaScript (ES6+)
- Charts: Chart.js 4.4
- Styling: Pure CSS (no frameworks)

**Screen Structure**:

**Screen 1: Assets + Summary**
- Assets table (zen design, no borders)
- Summary (Total, Cost, Profit, APY)
- Currency toggle (USD/THB)

**Screen 2: Portfolio Chart**
- Line chart showing cumulative value
- Chart.js with responsive design
- Touch interactions

**Screen 3: Transaction History**
- Zen table of all transactions
- Oldest first (chronological)
- Tap row → detail modal

**Screen 4: Prognosis**
- Future value projections
- Configurable timespan (1-30 years)
- CAGR-based calculations
- Projection chart

**Navigation**: Touch swipe (left/right)

**State Management**: Simple object-based state

**API Integration**:
- Backend API (payments, assets, exchange rate)
- CoinGecko API (BTC, Gold prices)

### 6. External APIs

**CoinGecko API** (Free tier, no key)
- Endpoint: `/simple/price`
- Purpose: Current BTC and Gold prices in USD
- Rate Limit: 10-50 calls/minute (free tier)

**ExchangeRate-API** (Free tier, no key)
- Endpoint: `/v4/latest/USD`
- Purpose: Current USD/THB exchange rate
- Updates: Daily

## Data Flow Diagrams

### Sync Flow (Google Sheet → Database)

```
┌─────────────┐
│   User      │
│  Adds row   │
│  in sheet   │
└──────┬──────┘
       │
       │ Clicks "Sync Payments"
       ↓
┌─────────────────────────┐
│   Apps Script           │
│  1. Read H1 (password)  │
│  2. Read rows 29+       │
│  3. Validate data       │
│  4. Build JSON          │
└──────┬──────────────────┘
       │
       │ POST /api/sync-payments
       │ { password, payments: [...] }
       ↓
┌─────────────────────────┐
│   Backend               │
│  1. Check password      │
│  2. Validate assets     │
│  3. Begin transaction   │
│  4. Upsert each row     │
│  5. Commit              │
└──────┬──────────────────┘
       │
       │ SQL UPSERT
       ↓
┌─────────────────────────┐
│   PostgreSQL            │
│  • Insert or update     │
│  • Return status        │
└──────┬──────────────────┘
       │
       │ Response: { inserted, updated }
       ↓
┌─────────────────────────┐
│   Apps Script           │
│  Show success alert     │
└─────────────────────────┘
```

### View Flow (Frontend → Database)

```
┌─────────────┐
│   User      │
│ Opens app   │
└──────┬──────┘
       │
       ↓
┌─────────────────────────┐
│   Frontend              │
│  1. Load state          │
│  2. Fetch data          │
└──────┬──────────────────┘
       │
       │ GET /api/payments
       │ GET /api/assets
       │ GET /api/current-exchange-rate
       ↓
┌─────────────────────────┐
│   Backend               │
│  1. Query database      │
│  2. Join tables         │
│  3. Return JSON         │
└──────┬──────────────────┘
       │
       │ SELECT ... JOIN ...
       ↓
┌─────────────────────────┐
│   PostgreSQL            │
│  Return rows            │
└──────┬──────────────────┘
       │
       │ Response: [{ ... }, ...]
       ↓
┌─────────────────────────┐
│   Frontend              │
│  • Fetch current prices │
│  • Calculate values     │
│  • Render screens       │
│  • Show charts          │
└─────────────────────────┘
       ↑
       │ API calls for prices
       │
┌─────────────────────────┐
│   CoinGecko API         │
│   ExchangeRate-API      │
└─────────────────────────┘
```

## Security Architecture

### Authentication

**Google Apps Script → Backend**:
- Method: Password in request body
- Password stored in: Google Sheet H1 + Railway env variable
- Validation: String comparison

**Frontend → Backend**:
- No authentication required (read-only endpoints)
- CORS restricts to specific frontend domain

### Data Protection

**Transport**:
- HTTPS enforced (automatic on Railway)
- TLS 1.2+ encryption

**Database**:
- PostgreSQL internal to Railway network
- Not publicly accessible
- Connection via DATABASE_URL (includes credentials)

**Passwords**:
- Never logged or exposed in responses
- Stored in environment variables (Railway)
- Not committed to Git (.env in .gitignore)

### Rate Limiting

**POST /api/sync-payments**:
- 10 requests per minute
- Prevents brute force attacks

**GET endpoints**:
- 60 requests per minute
- Prevents DoS

## Performance Considerations

### Backend

**Database Queries**:
- Indexed on frequently queried columns
- JOINs optimized with foreign keys
- Connection pooling (pg.Pool)

**Response Times**:
- Health check: <10ms
- GET /api/payments: <100ms
- POST /api/sync-payments: <500ms per payment

### Frontend

**Initial Load**:
- Minimal HTML/CSS/JS (<100KB total)
- Lazy load charts (Chart.js CDN)
- No heavy frameworks

**Runtime**:
- Vanilla JS (no virtual DOM overhead)
- Chart rendering: ~100ms
- Screen transitions: 300ms (CSS)

**API Calls**:
- Parallel fetching (Promise.all)
- Local state caching
- No real-time updates (manual refresh)

### Database

**Size Estimates**:
- Each transaction: ~200 bytes
- 1000 transactions: ~200KB
- 10,000 transactions: ~2MB
- Well under 500MB free tier limit

## Scalability

**Current Limits**:
- Railway Free Tier: $5/month credits
- PostgreSQL: 500MB storage, 1M rows
- Sufficient for personal use (years of data)

**If Scaling Needed**:
1. Archive old transactions
2. Upgrade Railway plan ($5/month)
3. Add caching layer (Redis)
4. Optimize database queries
5. Add pagination to frontend

## Deployment Architecture

```
┌──────────────────────────────────────────┐
│           Railway Platform               │
│                                          │
│  ┌────────────────┐  ┌────────────────┐ │
│  │   Backend      │  │   Frontend     │ │
│  │   Service      │  │   Service      │ │
│  │                │  │                │ │
│  │ • Node.js      │  │ • Static site  │ │
│  │ • Auto-deploy  │  │ • Vite build   │ │
│  │ • Domain gen   │  │ • Domain gen   │ │
│  └────────┬───────┘  └────────────────┘ │
│           │                              │
│           │ Internal network             │
│           ↓                              │
│  ┌────────────────┐                     │
│  │  PostgreSQL    │                     │
│  │  Database      │                     │
│  │                │                     │
│  │ • 500MB tier   │                     │
│  │ • Backups      │                     │
│  │ • Monitoring   │                     │
│  └────────────────┘                     │
│                                          │
└──────────────────────────────────────────┘
```

## Monitoring & Maintenance

**Railway Dashboard**:
- Deployment logs
- Resource usage
- Database size
- Error tracking

**Maintenance Tasks**:
- Monitor Railway credits usage
- Check database size monthly
- Review error logs weekly
- Update dependencies quarterly

## Disaster Recovery

**Backup Strategy**:
- Google Sheet: Google Drive auto-backup
- Database: Railway auto-backups (daily)
- Code: Git version control

**Recovery Steps**:
1. Restore from Google Sheet (re-sync)
2. Or restore from Railway database backup
3. Redeploy from Git if code issues

---

This architecture provides a simple, secure, and scalable solution for personal savings tracking.
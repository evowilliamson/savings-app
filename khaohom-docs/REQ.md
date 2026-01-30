# Khaohom's Savings Tracking App - Complete Requirements

## Project Goal
A simple, secure, private web app for viewing Khaohom's savings progress across multiple assets (BTC, Gold, USD, etc.).

- All data entry/editing happens manually in your Google Sheet (the single source of truth)
- You trigger sync manually via a custom menu in the sheet
- The viewer app is read-only and pulls from a PostgreSQL database (via your own backend)
- Four-screen mobile-friendly app with swipe navigation
- **Android phone only** (optimized for mobile portrait)
- **Internet required** (no offline mode)

## 1. Data Flow

1. You enter/edit rows in Google Sheet starting at **row 29** (columns: Date, Amount, THBPrice, USD, USD Cum., Asset, Reason, Note, USDTHB)
2. You click custom menu → Sync in the sheet → Google Apps Script collects **ALL rows** from A29 onwards → POSTs JSON to your backend
3. Your backend receives data → upserts into Postgres tables (database tracks sync state, no sheet column needed)
4. Web viewer app fetches from your backend API → displays summaries, chart, history, and prognosis

## 2. Google Sheet + Apps Script (Input Side)

- You maintain the sheet manually in tab: **"Kaohom Savings Account"**
- **Data starts at row 29** (A29)
- **Password stored in cell H1** (same tab as data)
- **Custom menu:** "Savings Tools" > "Sync Payments"

### Sheet Structure (Example):
```
Date        Amount      THBPrice  USD      USD Cum.  Asset       Reason              Note      USDTHB
2025/01/01  0.5                   $1,315   $1,315    GOLD        Birthday            paid      31.2
2025/02/17  ฿0.01000    $95,400   $954     $2,269    BTC         Chinese new year    paid      31.2
2025/03/01  ฿0.00185    $85,120   $157     $2,426    BTC         Monthly savings     paid      31.2
2025/04/01  $100                  $100     $2,526    USD         Gift                paid      31.2
```

### Sync Logic (Apps Script):
- Reads **ALL rows** starting from A29 (includes both "paid" and "not paid")
- Reads password from cell **H1** in "Kaohom Savings Account" tab
- Validates that each payment references an existing asset in the assets table
- If payment references non-existent asset → sync fails with error message
- Builds array of objects (one per row)
- POSTs to your endpoint (e.g. `https://your-railway-app.railway.app/api/sync-payments`)
- Includes password from H1 in request - must match `SYNC_PASSWORD` environment variable in Railway
- **No sync column needed** - database handles duplicate detection via unique constraint
- Shows toast/alert on success/failure

## 3. PostgreSQL Database (Two Tables)

### Assets Table:
```sql
CREATE TABLE assets (
    id                  SERIAL PRIMARY KEY,
    asset_name          VARCHAR(50) UNIQUE NOT NULL,  -- 'BTC', 'GOLD', 'USD', etc.
    display_name        VARCHAR(100) NOT NULL,        -- 'Bitcoin', 'Gold', 'US Dollar'
    cagr_percent        NUMERIC DEFAULT 0,            -- Default CAGR for prognosis (e.g., 45.0 for 45%)
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_assets_name ON assets (asset_name);

-- Example seed data:
INSERT INTO assets (asset_name, display_name, cagr_percent) VALUES
('BTC', 'Bitcoin', 25.0),
('GOLD', 'Gold', 15.0),
('USD', 'US Dollar', 5.0);
```

### Savings Transactions Table:
```sql
CREATE TABLE savings_transactions (
    id                  SERIAL PRIMARY KEY,
    transaction_date    DATE NOT NULL,
    amount              NUMERIC NOT NULL,
    asset_id            INTEGER NOT NULL REFERENCES assets(id),
    price               NUMERIC,                        -- Asset price at time of purchase (for BTC)
    usd_value_at_tx     NUMERIC NOT NULL,               -- USD column from sheet
    usd_cumulative      NUMERIC NOT NULL,               -- USD Cum. column from sheet
    reason              TEXT,                           -- Birthday, Monthly savings, etc.
    status              VARCHAR(20),                    -- 'paid' or 'not paid'
    usdthb_rate         NUMERIC NOT NULL,               -- Exchange rate at transaction time
    synced_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (transaction_date, amount, asset_id, usd_value_at_tx)
);

CREATE INDEX idx_savings_date ON savings_transactions (transaction_date DESC);
CREATE INDEX idx_savings_asset ON savings_transactions (asset_id);

-- Asset reference logic (in backend):
-- Join with assets table using asset name from sheet
-- If asset doesn't exist in assets table → reject transaction
```

## 4. Backend / Web Service (Minimal, No ORM)

**Language:** Node.js + Express + pg

### Endpoints:

#### POST /api/sync-payments
- **Body:** `{ "password": "...", "payments": [ {date, amount, price, usd_value, usd_cum, asset, reason, status, usdthb_rate}, ... ] }`
- **Auth:** Password from Google Sheet cell H1 must match `SYNC_PASSWORD` environment variable in Railway
- **Logic:** 
  - Verify password matches env variable (401 if mismatch)
  - For each payment:
    - Look up asset_id from assets table using asset name
    - If asset not found → return error, abort sync
  - Loop through validated payments → UPSERT each row using the unique constraint
  - Update `updated_at` timestamp on conflicts
- **Return:** `200` + `{ "message": "Synced X payments", "inserted": N, "updated": M }` or `401`/`400` on auth/validation failure

#### GET /api/payments
- **Returns:** array of all rows with asset details (JOIN with assets table), ordered by `transaction_date DESC` (newest first for display)
- **No auth required** - read-only endpoint
- **Format:**
```json
[
  {
    "id": 1,
    "transaction_date": "2025-01-01",
    "amount": 0.5,
    "asset_name": "GOLD",
    "asset_display_name": "Gold",
    "price": null,
    "usd_value_at_tx": 1315,
    "usd_cumulative": 1315,
    "reason": "Birthday",
    "status": "paid",
    "usdthb_rate": 31.2
  }
]
```

#### GET /api/assets
- **Returns:** array of all assets with their CAGR settings
- **No auth required** - read-only endpoint
- **Format:**
```json
[
  {
    "id": 1,
    "asset_name": "BTC",
    "display_name": "Bitcoin",
    "cagr_percent": 45.0
  }
]
```

#### GET /api/current-exchange-rate
- **Returns:** current USD/THB exchange rate from external API
- **Logic:** Fetch from reliable API (e.g., exchangerate-api.com or CoinGecko)
- **Format:**
```json
{
  "rate": 33.45,
  "timestamp": "2025-01-30T10:00:00Z"
}
```

### Deploy:
**Railway** (backend + PostgreSQL database)
- Backend: Node.js Express app
- Database: Railway PostgreSQL addon (free tier: 500MB storage, 1 million rows)
- Environment variables: `SYNC_PASSWORD`, `DATABASE_URL`

### Security:
- HTTPS (automatic on Railway)
- Password auth on POST endpoint (simple string comparison)
- CORS configured for Railway frontend domain
- Rate limiting: 10 req/min on POST, 60 req/min on GET

## 5. Web Viewer App (Mobile Web App – Read-Only)

**Type:** Mobile web app (NOT PWA - no offline, no service worker)

### Navigation:
- Four screens, horizontal swipe (left/right)
  - **Screen 1 (default):** Assets list + Summary (Total, Cost, Profit, APY)
  - **Screen 2:** Chart (cumulative portfolio value over time)
  - **Screen 3:** Transaction history list
  - **Screen 4:** Prognosis (future value projections)
- **Currency toggle:** USD ⇄ THB (top of screens 1, 2, and 4, persistent via localStorage, default USD)
- **Data source:** GET from your backend `/api/payments` and `/api/assets`
- **Price APIs:**
  - Current BTC price: CoinGecko `bitcoin` (USD)
  - Current Gold price: CoinGecko `tether-gold` (XAUT in USD)
  - Current USDTHB rate: Backend endpoint `/api/current-exchange-rate`

### Screen 1 – Assets + Summary

**Header:** 
- App title: "🎁 Khaohom's Savings"
- Currency toggle button: **USD** / **THB**
- Refresh button (↻)

**Asset List (zen table - minimalist, no borders):**
```
            Amount       USD
BTC         ฿0.02866     $2,522
Gold        1.00000      $5,532
US Dollar   $0           $0
... (more assets from database)
```
- **Scrollable** if more than 3-4 assets
- **Zen design:** No borders, no cards - just text aligned in table format with spacing
- Currency symbol adapts: $ or ฿ based on toggle
- **Asset aggregation:** Backend groups transactions by asset and sums amounts
- **Dynamic asset types:** Frontend displays whatever assets exist in database (fetched from `/api/assets` and aggregated from transactions)

**Summary Section (below asset list, zen table):**
```
Total          $8,054 / ฿251,283
Cost           $5,648
Profit         $2,405 (42.6%)
APY %          46.54%
```
- **Zen table format** (no borders, just aligned text)
- Rows: Total, Cost, Profit, APY %
- No background colors (removed green backgrounds)

### Screen 2 – Chart

**Header:** 
- Title: "Portfolio Chart"
- Currency toggle (synced with Screen 1)
- Refresh button

**Chart (full screen height):**
- **X-axis:** Transaction dates (sorted oldest → newest for time series)
- **Y-axis:** Cumulative portfolio value (USD or THB based on toggle)
- **Line:**
  - **Cumulative portfolio value** (single line, primary color)
  - Shows running total from USD Cum. in database
  - Converted to THB if toggle selected using current exchange rate
- **Simplified:** No BTC/XAUT overlays (too complex)
- **Touch interactions:** Tap point to see exact value and date

### Screen 3 – Transaction History

**Header:** 
- Title: "Transaction History"
- Currency toggle (synced with Screen 1)
- Refresh button

**Transaction Table (zen table - no borders):**
```
Date        Amount      Value    Asset
2025/01/01  0.5         $1,315   GOLD
2025/02/17  ฿0.01000    $954     BTC
2025/03/01  ฿0.00185    $157     BTC
2025/04/01  ฿0.00150    $124     BTC
... (scrollable - oldest first)
```

**Table Details:**
- **Date:** YYYY/MM/DD format
- **Amount:** Shows raw amount with currency prefix (฿ for BTC, numeric for Gold, $ for USD)
- **Value:** USD or THB based on toggle (column header changes: "USD" or "THB")
  - Shows value at purchase time
  - Converted using stored `usdthb_rate` if THB selected
- **Asset:** Asset name from database
- **Sort:** Oldest first (chronological, fixed - no user sorting)
- **Zen design:** No borders, just text aligned with spacing

**Row Click → Detail Modal:**
When user taps any row, show popup card with full details:
```
┌─────────────────────────────────────┐
│  Transaction Details           ✕    │
├─────────────────────────────────────┤
│  📅 Date: 2025/02/17                │
│  💰 Amount: ฿0.01000 BTC            │
│  💵 Value: $954 (฿29,765)           │
│  📍 THB Price: ฿95,400/BTC          │
│  📝 Reason: Chinese new year        │
│  ✅ Status: paid                    │
│  💱 Rate: 1 USD = 31.2 THB          │
└─────────────────────────────────────┘
```
- Modal dims background
- Shows all fields from database
- Close button (✕) or tap outside to dismiss
- Status badge: green (paid) or orange (not paid)

### Screen 4 – Prognosis (Future Value Projection)

**Header:**
- Title: "Future Value Prognosis"
- Currency toggle (synced with other screens)
- Refresh button

**Timespan Input:**
```
Projection Period (years):  [  5  ]  ← Input field
```
- Default: 5 years
- User can modify (1-30 years)
- Auto-updates calculations on change

**Future Asset Values (zen table - no borders):**
```
            Current      After 5Y
BTC         $2,522       $18,034
Gold        $5,532       $8,127
US Dollar   $0           $0
─────────────────────────────────
Total       $8,054       $26,161
```
- **Current:** Shows current market value (amount × current_price from CoinGecko)
- **Calculation:** Current market value × (1 + CAGR/100)^years
- **CAGR values:** From assets table (BTC: 25%, GOLD: 15%, USD: 5%)
- **Assumption:** USD/THB rate remains constant (use current rate for both columns)
- **Zen design:** No borders, minimalist alignment

**Future Value Chart:**
- **X-axis:** Years (0 to selected timespan, e.g., Y0, Y1, Y2... Y5)
- **Y-axis:** Total portfolio value (USD or THB based on toggle)
- **Line:** Single line showing projected total value over time
- **Calculation:** Sum of all assets' current market values compounded annually at their respective CAGRs
- **Simple design:** Clean line chart showing total only, no individual asset breakdown

**Calculation Logic:**
```javascript
// For each asset:
current_value = sum(amount * current_price) for all transactions of this asset
future_value = current_value * Math.pow(1 + (cagr / 100), years)

// Total future value:
total_future = sum(future_value for all assets)

// Chart data points (year 0 to timespan):
for (let year = 0; year <= timespan; year++) {
  total_value_at_year[year] = sum(
    current_value_of_asset * Math.pow(1 + (cagr_of_asset / 100), year)
    for all assets
  )
}
```

## 6. Non-Functional Requirements

- **Platform:** Android phone only (portrait orientation, responsive 360px-420px width)
- **No offline mode:** Internet required for all functionality
- **No PWA features:** No service worker, no manifest, no install prompt
- **UI:** Mobile-first, clean, minimal design
  - Light/dark mode (follows system preference)
  - Smooth swipe animations between screens
  - Zen aesthetic: minimal borders, clean typography, plenty of whitespace
- **Font:** `font-family: 'Roboto', system-ui, sans-serif`
- **Performance:** Target 60fps animations
- **Loading:** Show skeleton loaders during API calls
- **Error handling:** Toast messages for network errors

## 7. API Integration Details

### CoinGecko API (Free tier, no key needed):
```javascript
// Current BTC price
GET https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd
Response: {"bitcoin":{"usd":98500}}

// Current Gold (XAUT) price
GET https://api.coingecko.com/api/v3/simple/price?ids=tether-gold&vs_currencies=usd
Response: {"tether-gold":{"usd":2630}}
```

### Exchange Rate API:
```javascript
// Backend endpoint fetches from external source
GET /api/current-exchange-rate
Response: {"rate": 33.45, "timestamp": "2025-01-30T10:00:00Z"}
```

### Backend Endpoints Summary:
- `GET /api/payments` - All transactions with asset details
- `GET /api/assets` - All assets with CAGR values
- `GET /api/current-exchange-rate` - Current USD/THB rate
- `POST /api/sync-payments` - Sync from Google Sheet (password protected)

## 8. Visual Design System

### Color Palette:
```css
/* Light mode */
--primary: #1976D2;        /* Blue for accents */
--surface: #FFFFFF;        /* White background */
--text-primary: #212121;   /* Dark gray text */
--text-secondary: #757575; /* Light gray text */
--border: #E0E0E0;        /* Subtle borders (used sparingly) */
--success: #4CAF50;       /* Green for profit/paid status */
--warning: #FF9800;       /* Orange for unpaid status */

/* Dark mode */
--primary: #42A5F5;
--surface: #121212;
--text-primary: #FFFFFF;
--text-secondary: #B0B0B0;
--border: #2C2C2C;
--success: #66BB6A;
--warning: #FFA726;
```

### Typography:
```css
font-family: 'Roboto', system-ui, sans-serif;

/* Headers */
h1: 24px, 600 weight
h2: 20px, 600 weight
h3: 18px, 500 weight

/* Body */
body: 16px, 400 weight
small: 14px, 400 weight
```

### Spacing:
```css
--space-xs: 4px;
--space-sm: 8px;
--space-md: 16px;
--space-lg: 24px;
--space-xl: 32px;
```

### Zen Table Design:
```css
/* No borders, just spacing and alignment */
.zen-table {
  width: 100%;
  border-collapse: collapse;
}

.zen-table td {
  padding: var(--space-md) var(--space-sm);
  border: none;
}

.zen-table tr:not(:last-child) td {
  border-bottom: 1px solid var(--border); /* Optional subtle separator */
}

/* Even more minimal: no separators at all */
.zen-table-pure td {
  padding: var(--space-md) var(--space-sm);
  border: none;
}
```

## 9. Screen Layouts (ASCII)

### Screen 1 - Assets + Summary
```
┌─────────────────────────────────────┐ ← 100vw
│  🎁 Khaohom's Savings         ↻     │ ← Header (56px)
│  [●USD]  [ THB ]                    │ ← Toggle (40px)
├─────────────────────────────────────┤
│ Asset List (zen table):             │
│                                     │
│             Amount      USD         │
│ BTC         ฿0.02866    $2,522      │
│ Gold        1.00000     $5,532      │
│ US Dollar   $0          $0          │
│                                     │
├─────────────────────────────────────┤
│ Summary (zen table):                │
│                                     │
│ Total       $8,054 / ฿251,283       │
│ Cost        $5,648                  │
│ Profit      $2,405 (42.6%)          │
│ APY %       46.54%                  │
│                                     │
│  Last updated: 2 mins ago           │ ← Footer (32px)
└─────────────────────────────────────┘
       👈 Swipe left for chart
```

### Screen 2 - Portfolio Chart
```
┌─────────────────────────────────────┐ ← 100vw
│  Portfolio Chart              ↻     │ ← Header (56px)
│  [●USD]  [ THB ]                    │ ← Toggle (40px)
├─────────────────────────────────────┤
│ Chart (full screen height):         │
│ │                          ╱        │
│ │                     ╱────         │
│ │              ╱─────               │
│ │       ╱─────                      │
│ │──────                             │
│ └───────────────────────────────────│
│   Jan  Feb  Mar  Apr  May  Jun      │
│                                     │
│                                     │
│                                     │
│                                     │
│  Last updated: 2 mins ago           │ ← Footer (32px)
└─────────────────────────────────────┘
  👈 Swipe left     Swipe right 👉
```

### Screen 3 - Transaction History
```
┌─────────────────────────────────────┐ ← 100vw
│  Transaction History          ↻     │ ← Header (56px)
│  [●USD]  [ THB ]                    │ ← Toggle (40px)
├─────────────────────────────────────┤
│ ┌─────────────────────────────────┐ │
│ │ Date       Amount   Value  Asset│ │ ← Table header
│ │                                 │ │
│ │ 2025/01/01 0.5      $1315  GOLD │ │ ← Row (48px)
│ │ 2025/02/17 ฿0.01000 $954   BTC  │ │
│ │ 2025/03/01 ฿0.00185 $157   BTC  │ │
│ │ 2025/04/01 ฿0.00150 $124   BTC  │ │
│ │ 2025/05/01 ฿0.00156 $148   BTC  │ │
│ │ 2025/06/01 ฿0.00148 $154   BTC  │ │
│ │          ... (scroll)           │ │
│ └─────────────────────────────────┘ │
│                                     │
│  Last updated: 2 mins ago           │ ← Footer (32px)
└─────────────────────────────────────┘
  👈 Swipe left     Swipe right 👉
```

### Screen 4 - Prognosis
```
┌─────────────────────────────────────┐ ← 100vw
│  Future Value Prognosis       ↻     │ ← Header (56px)
│  [●USD]  [ THB ]                    │ ← Toggle (40px)
├─────────────────────────────────────┤
│ Projection Period (years): [  5  ]  │ ← Input (48px)
├─────────────────────────────────────┤
│ Future Values (zen table):          │
│                                     │
│             Current     After 5Y    │
│ BTC         $2,522      $18,034     │
│ Gold        $5,532      $8,127      │
│ US Dollar   $0          $0          │
│ ─────────────────────────────────── │
│ Total       $8,054      $26,161     │
│                                     │
├─────────────────────────────────────┤
│ Projection Chart:                   │
│ │                              ╱    │
│ │                         ╱────     │
│ │                   ╱────           │
│ │             ╱────                 │
│ │       ╱────                       │
│ │──────                             │
│ └───────────────────────────────────│
│   Y0   Y1   Y2   Y3   Y4   Y5       │
│                                     │
│  Last updated: 2 mins ago           │ ← Footer (32px)
└─────────────────────────────────────┘
       👉 Swipe right for dashboard
```

## 10. Implementation Phases

### Phase 1: Backend + Database (Day 1-2)
1. ✅ PostgreSQL schema setup (two tables: assets + savings_transactions)
2. ✅ Seed assets table with initial values (BTC, GOLD, USD with CAGRs)
3. ✅ Node.js Express server with endpoints
4. ✅ Password authentication (env variable)
5. ✅ Asset validation on payment sync
6. ✅ Exchange rate endpoint integration
7. ✅ Environment configuration (.env.example)
8. ✅ Railway deployment guide
9. ✅ API testing (Postman collection)

### Phase 2: Google Apps Script (Day 2)
10. ✅ Apps Script code with password auth
11. ✅ Asset validation before sync
12. ✅ Menu integration
13. ✅ Error handling & user feedback
14. ✅ Testing guide (sync from A29)

### Phase 3: Frontend App (Day 3-5)
15. ✅ Project structure (Vite + vanilla JS)
16. ✅ Screen 1: Assets + Summary with zen tables
17. ✅ Screen 2: Portfolio Chart
18. ✅ Screen 3: Transaction history
19. ✅ Screen 4: Prognosis with projections
20. ✅ Currency toggle logic
21. ✅ CoinGecko integration
22. ✅ Exchange rate integration
23. ✅ Four-screen swipe navigation
24. ✅ Zen table styling (no borders)

### Phase 4: Testing & Polish (Day 6-7)
24. ✅ End-to-end testing flow
25. ✅ Android phone testing (Chrome)
26. ✅ Performance optimization
27. ✅ Error handling & edge cases
28. ✅ User guide / README

### Ready to Build?
I can now generate complete, production-ready code for any of these phases. Which would you like first?

**Recommended order:**
1. **Backend + Database** (foundation with asset table)
2. **Google Apps Script** (data pipeline with validation)
3. **Frontend App** (user interface with 4 screens)

Or I can create a **complete starter template** with all three integrated and ready to deploy to Railway.
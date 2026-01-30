# 🎁 Khaohom's Savings Tracker

A simple, secure mobile web app for tracking savings across multiple assets (Bitcoin, Gold, USD, etc.) with future value projections.

## Features

- 📊 **4-Screen Mobile Interface**
  - Screen 1: Assets list + Summary (Total, Cost, Profit, APY)
  - Screen 2: Portfolio value chart
  - Screen 3: Transaction history
  - Screen 4: Future value prognosis with configurable CAGRs

- 💱 **Dual Currency Support**: Toggle between USD and THB
- 🔄 **Google Sheets Integration**: Manage all data in a familiar spreadsheet
- 📈 **Compound Growth Projections**: See future values based on configurable CAGR per asset
- 🎨 **Zen Design**: Minimalist, border-free tables with clean typography
- 📱 **Mobile-First**: Optimized for Android phones with swipe navigation

## Architecture

```
┌─────────────────┐
│  Google Sheet   │ ← Manual data entry (single source of truth)
└────────┬────────┘
         │ Apps Script (manual sync)
         ↓
┌─────────────────┐
│  Backend API    │ ← Node.js + Express + PostgreSQL (Railway)
└────────┬────────┘
         │ REST API
         ↓
┌─────────────────┐
│  Frontend App   │ ← Vite + Vanilla JS + Chart.js (Railway)
└─────────────────┘
```

## Tech Stack

### Backend
- **Runtime**: Node.js 18+
- **Framework**: Express
- **Database**: PostgreSQL (Railway)
- **APIs**: CoinGecko (prices), ExchangeRate-API (USD/THB)

### Frontend
- **Build Tool**: Vite
- **UI**: Vanilla JavaScript + CSS
- **Charts**: Chart.js
- **No Framework**: Lightweight and fast

### Data Sync
- **Google Apps Script**: Custom menu integration
- **Authentication**: Simple password verification

## Quick Start

### Prerequisites

- Node.js 18+
- Google account (for Google Sheets)
- Railway account (for hosting)

### 1. Clone Repository

```bash
git clone https://github.com/YOUR-USERNAME/khaohom-savings.git
cd khaohom-savings
```

### 2. Backend Setup (Local Development)

```bash
cd backend
npm install
cp .env.example .env
# Edit .env with your settings
npm run dev
```

### 3. Frontend Setup (Local Development)

```bash
cd frontend
npm install
# Edit main.js to set API_BASE_URL to http://localhost:3000
npm run dev
```

### 4. Database Setup

```bash
# Connect to your PostgreSQL database
psql -U postgres -d khaohom_savings

# Run schema
\i database/schema.sql
```

### 5. Google Apps Script Setup

1. Open your Google Sheet
2. Extensions → Apps Script
3. Copy code from `google-apps-script/Code.gs`
4. Update `BACKEND_URL` with your backend URL
5. Save and refresh sheet

## Deployment

See [DEPLOYMENT.md](./DEPLOYMENT.md) for detailed Railway deployment instructions.

Quick summary:
1. Deploy backend to Railway (with PostgreSQL addon)
2. Deploy frontend to Railway
3. Configure Google Apps Script with production URLs
4. Add password to cell H1 in Google Sheet

## Google Sheet Structure

### Tab: "Kaohom Savings Account"

**Cell H1**: Password (must match backend env variable)

**Starting Row 29**:
| Column | Field | Example | Description |
|--------|-------|---------|-------------|
| A | Date | 2025-01-01 | Transaction date |
| B | Amount | 0.5 or ฿0.01000 | Amount (฿ prefix for BTC) |
| C | THBPrice | 95400 | Price in THB (for BTC) |
| D | USD | 1315 | USD value at purchase |
| E | USD Cum. | 1315 | Cumulative USD value |
| F | Asset | GOLD | Asset type (must exist in DB) |
| G | Reason | Birthday | Reason for transaction |
| H | Note | paid | Status: "paid" or "not paid" |
| I | USDTHB | 31.2 | USD/THB exchange rate |

### Adding New Assets

Assets must exist in the database before syncing transactions.

**Direct SQL** (in Railway PostgreSQL console):
```sql
INSERT INTO assets (asset_name, display_name, cagr_percent) 
VALUES ('ETH', 'Ethereum', 30.0);
```

**Asset Names in Database**:
- BTC → Bitcoin (25% CAGR)
- GOLD → Gold (15% CAGR)
- USD → US Dollar (5% CAGR)

## Usage

### Syncing Data

1. Add/edit rows in Google Sheet (starting row 29)
2. Click "Savings Tools" → "Sync Payments"
3. Confirm sync
4. Data is now in database

### Viewing Data

1. Open frontend app on your phone
2. Screen 1: See current assets and summary
3. Swipe left → Screen 2: View portfolio chart
4. Swipe left → Screen 3: See transaction history
5. Swipe left → Screen 4: View future projections

### Prognosis (Screen 4)

1. Navigate to Screen 4
2. Change projection period (default: 5 years)
3. See projected values based on CAGR
4. View growth chart

**Assumptions**:
- CAGR values from database
- USD/THB rate remains constant
- Current prices from CoinGecko

## API Endpoints

### Backend API

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/health` | GET | No | Health check |
| `/api/assets` | GET | No | Get all assets with CAGRs |
| `/api/payments` | GET | No | Get all transactions |
| `/api/current-exchange-rate` | GET | No | Get USD/THB rate |
| `/api/sync-payments` | POST | Yes | Sync from Google Sheet |

### External APIs

- **CoinGecko**: Current BTC and Gold prices (no key required)
- **ExchangeRate-API**: USD/THB exchange rate (no key required)

## File Structure

```
khaohom-savings/
├── backend/
│   ├── server.js              # Express server
│   ├── package.json
│   ├── .env.example
│   └── .gitignore
├── frontend/
│   ├── index.html             # Main HTML
│   ├── style.css              # All styles
│   ├── main.js                # Application logic
│   ├── vite.config.js
│   ├── package.json
│   └── .gitignore
├── database/
│   └── schema.sql             # PostgreSQL schema
├── google-apps-script/
│   └── Code.gs                # Google Apps Script
├── DEPLOYMENT.md              # Deployment guide
└── README.md                  # This file
```

## Configuration

### Backend (.env)

```bash
DATABASE_URL=postgresql://...       # Set by Railway
SYNC_PASSWORD=your-password         # Your secret password
PORT=3000                           # Port (Railway sets automatically)
NODE_ENV=production                 # Environment
FRONTEND_URL=https://...            # Your frontend URL (for CORS)
```

### Frontend (main.js)

```javascript
const CONFIG = {
  API_BASE_URL: 'https://your-backend.railway.app',
  // ...
};
```

### Google Apps Script (Code.gs)

```javascript
const CONFIG = {
  BACKEND_URL: 'https://your-backend.railway.app/api/sync-payments',
  // ...
};
```

## Customization

### Change CAGR Values

Update directly in database:
```sql
UPDATE assets 
SET cagr_percent = 50.0 
WHERE asset_name = 'BTC';
```

### Add New Asset Type

```sql
INSERT INTO assets (asset_name, display_name, cagr_percent) 
VALUES ('ETH', 'Ethereum', 35.0);
```

Then add transactions in Google Sheet with Asset = "ETH"

### Modify UI Colors

Edit `frontend/style.css`:
```css
:root {
  --primary: #1976D2;  /* Change to your color */
  /* ... */
}
```

## Troubleshooting

### Common Issues

**1. "Invalid password" when syncing**
- Check cell H1 has the correct password
- Verify `SYNC_PASSWORD` in Railway matches exactly

**2. "Asset not found" error**
- Asset must exist in database first
- Asset names are case-sensitive (use uppercase: BTC, GOLD, USD)

**3. Frontend shows no data**
- Check browser console for errors
- Verify `API_BASE_URL` in main.js
- Check CORS settings (FRONTEND_URL in backend)

**4. Chart not displaying**
- Make sure there are transactions in database
- Check browser console for Chart.js errors
- Navigate to Screen 2 (chart renders on navigation)

### Debug Mode

**Backend Logs**:
```bash
# In Railway, click on backend service → Deployments → View logs
```

**Frontend Console**:
```javascript
// Open browser dev tools (F12)
// Check Console tab for errors
```

**Database Query**:
```sql
-- Check all payments
SELECT * FROM savings_transactions ORDER BY transaction_date DESC;

-- Check all assets
SELECT * FROM assets;
```

## Security

- ✅ HTTPS enabled automatically (Railway)
- ✅ Password authentication for sync endpoint
- ✅ Rate limiting (10 POST/min, 60 GET/min)
- ✅ CORS configured for specific frontend domain
- ✅ SQL injection prevention (parameterized queries)
- ⚠️ Password stored in Google Sheet cell H1 (keep sheet private)

## Performance

- **Frontend**: Lightweight vanilla JS, no heavy frameworks
- **Backend**: Minimal dependencies, direct PostgreSQL queries
- **Database**: Indexed queries for fast lookups
- **Target**: 60fps animations on mobile

## Browser Support

- ✅ Chrome (Android) - Primary target
- ✅ Safari (iOS) - Should work
- ✅ Firefox Mobile - Should work
- ❌ IE11 - Not supported

## Future Enhancements

Potential features for v2:
- [ ] Recurring transactions
- [ ] Multiple portfolios
- [ ] Export to PDF
- [ ] Email alerts
- [ ] Budget tracking
- [ ] More asset types (stocks, bonds, etc.)

## Contributing

This is a personal project, but suggestions are welcome!

1. Fork the repository
2. Create feature branch
3. Make changes
4. Submit pull request

## License

MIT License - Feel free to use for personal projects

## Support

For issues or questions:
1. Check [DEPLOYMENT.md](./DEPLOYMENT.md) first
2. Review this README
3. Check Railway logs
4. Open a GitHub issue

## Acknowledgments

- Built with ❤️ for Khaohom
- CoinGecko API for crypto/gold prices
- Railway for easy hosting
- Chart.js for beautiful charts

---

**Note**: Remember to update the `API_BASE_URL` in all configuration files before deploying!
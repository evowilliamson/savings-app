# Quick Start Guide

Get Khaohom's Savings running in 30 minutes!

## What You Need

- [ ] GitHub account
- [ ] Railway account (free: https://railway.app)
- [ ] Google Sheet with your data
- [ ] 30 minutes

## Step-by-Step Setup

### 1️⃣ Prepare Your Google Sheet (5 min)

**Create tab**: "Kaohom Savings Account"

**Cell H1**: Add a strong password (e.g., `MySecure2025Pass!`)

**Row 29 onwards**: Add your transaction data

| A (Date) | B (Amount) | C (THBPrice) | D (USD) | E (USD Cum) | F (Asset) | G (Reason) | H (Note) | I (USDTHB) |
|----------|------------|--------------|---------|-------------|-----------|------------|----------|------------|
| 2025-01-15 | 0.5 | | 1315 | 1315 | GOLD | Birthday | paid | 31.2 |
| 2025-02-01 | ฿0.01000 | 95400 | 954 | 2269 | BTC | Savings | paid | 31.2 |

**Note**: Asset must be BTC, GOLD, or USD (add more later if needed)

### 2️⃣ Deploy Backend to Railway (10 min)

1. **Push backend to GitHub**:
   ```bash
   cd khaohom-savings/backend
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/YOUR-USERNAME/khaohom-backend.git
   git push -u origin main
   ```

2. **Create Railway project**:
   - Go to https://railway.app/new
   - Click "Deploy from GitHub repo"
   - Select your backend repo
   - Railway auto-detects Node.js

3. **Add PostgreSQL**:
   - Click "New" → "Database" → "PostgreSQL"
   - Railway sets DATABASE_URL automatically

4. **Set environment variables**:
   - Click backend service → "Variables"
   - Add:
     ```
     SYNC_PASSWORD=MySecure2025Pass!
     NODE_ENV=production
     ```
   - (FRONTEND_URL will be added later)

5. **Initialize database**:
   - Click PostgreSQL service → "Data" → "Query"
   - Copy entire `database/schema.sql`
   - Paste and click "Run Query"
   - Verify: "Database schema created successfully!"

6. **Get backend URL**:
   - Click backend service → "Settings" → "Networking"
   - Click "Generate Domain"
   - Copy URL: `https://your-backend-abc123.railway.app`
   - **SAVE THIS URL** 📝

### 3️⃣ Deploy Frontend to Railway (10 min)

1. **Update frontend config**:
   ```bash
   cd ../frontend
   ```
   
   Open `main.js`, find line 18:
   ```javascript
   API_BASE_URL: 'https://your-app.railway.app',
   ```
   
   Replace with your actual backend URL from step 2.6

2. **Push frontend to GitHub**:
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/YOUR-USERNAME/khaohom-frontend.git
   git push -u origin main
   ```

3. **Deploy to Railway**:
   - Go to https://railway.app/new
   - Click "Deploy from GitHub repo"
   - Select your frontend repo
   - Railway auto-detects Vite

4. **Get frontend URL**:
   - Click frontend service → "Settings" → "Networking"
   - Click "Generate Domain"
   - Copy URL: `https://your-frontend-xyz789.railway.app`
   - **SAVE THIS URL** 📝

5. **Update backend CORS**:
   - Go back to backend service
   - Click "Variables"
   - Add:
     ```
     FRONTEND_URL=https://your-frontend-xyz789.railway.app
     ```
   - Backend redeploys automatically

### 4️⃣ Setup Google Apps Script (5 min)

1. **Open Apps Script**:
   - Open your Google Sheet
   - Extensions → Apps Script

2. **Copy code**:
   - Delete default code
   - Copy entire `google-apps-script/Code.gs`
   - Paste into editor

3. **Update config** (line 23):
   ```javascript
   BACKEND_URL: 'https://your-backend-abc123.railway.app/api/sync-payments',
   ```
   Replace with your actual backend URL + `/api/sync-payments`

4. **Save**:
   - Click disk icon to save
   - Refresh your Google Sheet
   - You should see new menu: "Savings Tools"

### 5️⃣ Test Everything (5 min)

**Test 1: Backend Health**
```
Visit: https://your-backend-abc123.railway.app/health
Should see: {"status":"ok","timestamp":"..."}
```

**Test 2: Apps Script Connection**
```
In Google Sheet:
1. Click "Savings Tools" → "Test Connection"
2. Should show: "Backend connection successful!"
```

**Test 3: Sync Data**
```
In Google Sheet:
1. Click "Savings Tools" → "Sync Payments"
2. Click "Yes" to confirm
3. Should show: "Synced X payments successfully"
```

**Test 4: View Frontend**
```
Visit: https://your-frontend-xyz789.railway.app
Should see:
- Screen 1: Your assets and summary
- Swipe left: Chart
- Swipe left: Transaction history
- Swipe left: Future projections
```

## 🎉 You're Done!

Save your frontend URL and add it to your phone's home screen!

## Quick Reference Card

**Your URLs** (write these down):
```
Backend:  https://your-backend-abc123.railway.app
Frontend: https://your-frontend-xyz789.railway.app
```

**Your Password** (in Google Sheet H1 and Railway backend env):
```
SYNC_PASSWORD: MySecure2025Pass!
```

**Regular Usage**:
1. Add transactions to Google Sheet
2. Click "Savings Tools" → "Sync Payments"
3. Refresh frontend app

## Common Quick Fixes

**"Invalid password" when syncing**
→ Check H1 matches SYNC_PASSWORD exactly (case-sensitive)

**"Asset not found"**
→ Asset column must be: BTC, GOLD, or USD (uppercase)

**Frontend shows no data**
→ Check browser console, verify API_BASE_URL in main.js

**Need to add new asset type**
→ In Railway PostgreSQL Query tab:
```sql
INSERT INTO assets (asset_name, display_name, cagr_percent) 
VALUES ('ETH', 'Ethereum', 30.0);
```

## Next Steps

✅ **Customize CAGR values** (in Railway PostgreSQL):
```sql
UPDATE assets SET cagr_percent = 50.0 WHERE asset_name = 'BTC';
```

✅ **Add more transactions** in Google Sheet

✅ **View projections** in Screen 4

✅ **Monitor usage** in Railway dashboard

## Need Help?

1. Check [README.md](./README.md) - Full documentation
2. Check [DEPLOYMENT.md](./DEPLOYMENT.md) - Detailed steps
3. Check Railway logs - Backend/Frontend → Deployments → Logs
4. Open GitHub issue

---

**Pro Tips**:
- Bookmark frontend URL on your phone
- Add to home screen for app-like experience
- Use strong password (min 12 characters)
- Back up your Google Sheet regularly
- Monitor Railway usage (free tier: $5/month credits)

That's it! You now have a fully functional savings tracker! 🚀
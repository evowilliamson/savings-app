# Deployment Guide - Railway

This guide walks you through deploying Khaohom's Savings app to Railway.

## Prerequisites

- GitHub account
- Railway account (sign up at https://railway.app)
- Your Google Sheet with data starting at row 29

## Part 1: Deploy Backend + Database

### Step 1: Push Code to GitHub

1. Create a new GitHub repository
2. Push the `backend` folder to the repository:

```bash
cd khaohom-savings/backend
git init
git add .
git commit -m "Initial backend commit"
git remote add origin https://github.com/YOUR-USERNAME/YOUR-REPO.git
git push -u origin main
```

### Step 2: Create Railway Project

1. Go to https://railway.app
2. Click "New Project"
3. Select "Deploy from GitHub repo"
4. Select your backend repository
5. Railway will auto-detect Node.js

### Step 3: Add PostgreSQL Database

1. In your Railway project, click "New"
2. Select "Database" → "PostgreSQL"
3. Railway will create a database and set `DATABASE_URL` automatically

### Step 4: Set Environment Variables

1. Click on your backend service
2. Go to "Variables" tab
3. Add these variables:

```
SYNC_PASSWORD=your-secret-password-here
NODE_ENV=production
FRONTEND_URL=https://your-frontend.railway.app
```

(You'll update FRONTEND_URL after deploying the frontend)

### Step 5: Initialize Database Schema

1. In Railway, click on your PostgreSQL database
2. Click "Data" tab
3. Click "Query" 
4. Copy and paste the entire contents of `database/schema.sql`
5. Click "Run Query"
6. Verify that you see:
   - "Database schema created successfully!"
   - 3 rows in assets table (BTC, GOLD, USD)

### Step 6: Get Backend URL

1. Click on your backend service
2. Go to "Settings" tab
3. Under "Networking", click "Generate Domain"
4. Copy the generated URL (e.g., `https://your-backend.railway.app`)
5. Save this URL - you'll need it for:
   - Google Apps Script
   - Frontend configuration

## Part 2: Deploy Frontend

### Step 1: Update Frontend Configuration

1. Open `frontend/main.js`
2. Find this line:
```javascript
API_BASE_URL: 'https://your-app.railway.app',
```
3. Replace with your actual backend URL from Part 1, Step 6

### Step 2: Push Frontend to GitHub

1. Create another GitHub repository (or use a different branch)
2. Push the `frontend` folder:

```bash
cd ../frontend
git init
git add .
git commit -m "Initial frontend commit"
git remote add origin https://github.com/YOUR-USERNAME/YOUR-FRONTEND-REPO.git
git push -u origin main
```

### Step 3: Deploy Frontend to Railway

1. In Railway, create a new project (or add to existing)
2. Select "Deploy from GitHub repo"
3. Select your frontend repository
4. Railway will auto-detect Vite

### Step 4: Configure Build Settings

Railway should auto-detect, but verify:
- **Build Command**: `npm run build`
- **Start Command**: Not needed (static site)
- **Output Directory**: `dist`

### Step 5: Generate Frontend Domain

1. Click on frontend service
2. Go to "Settings" → "Networking"
3. Click "Generate Domain"
4. Copy the URL (e.g., `https://your-frontend.railway.app`)

### Step 6: Update Backend CORS

1. Go back to backend service
2. Update `FRONTEND_URL` environment variable with your actual frontend URL
3. Backend will restart automatically

## Part 3: Configure Google Apps Script

### Step 1: Open Google Apps Script

1. Open your Google Sheet
2. Go to Extensions → Apps Script
3. Delete any default code

### Step 2: Copy Script Code

1. Copy the entire contents of `google-apps-script/Code.gs`
2. Paste into the Apps Script editor

### Step 3: Update Configuration

Find this line:
```javascript
BACKEND_URL: 'https://your-app.railway.app/api/sync-payments',
```

Replace with your actual backend URL + `/api/sync-payments`

### Step 4: Save and Test

1. Click "Save" (disk icon)
2. Refresh your Google Sheet
3. You should see a new menu: "Savings Tools"

### Step 5: Add Password to Sheet

1. In your Google Sheet, go to cell H1
2. Enter the same password you used in `SYNC_PASSWORD` environment variable
3. This password must match exactly

## Part 4: Test the Complete System

### Test 1: Backend Health Check

Visit: `https://your-backend.railway.app/health`

Should return:
```json
{"status":"ok","timestamp":"2025-01-30T..."}
```

### Test 2: Google Apps Script Sync

1. In Google Sheet, click "Savings Tools" → "Test Connection"
2. Should show "Backend connection successful!"
3. Click "Savings Tools" → "Sync Payments"
4. Confirm the sync
5. Should show "Synced X payments successfully"

### Test 3: Frontend Access

1. Visit your frontend URL: `https://your-frontend.railway.app`
2. You should see:
   - Screen 1: Assets and summary
   - Swipe left to see chart
   - Swipe left again for transaction history
   - Swipe left again for prognosis

### Test 4: Data Flow

1. Add a new row in your Google Sheet (row 30 or later)
2. Fill in: Date, Amount, THBPrice, USD, USD Cum., Asset, Reason, Note, USDTHB
3. Make sure Asset matches one in database (BTC, GOLD, or USD)
4. Click "Savings Tools" → "Sync Payments"
5. Refresh your frontend
6. New transaction should appear!

## Troubleshooting

### "Invalid password" error
- Check that H1 cell contains exact same password as `SYNC_PASSWORD` env variable
- Password is case-sensitive

### "Asset not found" error
- Make sure asset name in sheet matches database exactly: BTC, GOLD, or USD
- Asset names are case-insensitive but should be uppercase

### Frontend shows no data
- Check browser console for errors
- Verify `API_BASE_URL` in `frontend/main.js` is correct
- Check CORS settings in backend (FRONTEND_URL env variable)

### Database connection error
- Railway PostgreSQL should set DATABASE_URL automatically
- Check "Variables" tab in Railway backend service

### Frontend won't build
- Make sure `package.json` is in frontend root
- Check Railway build logs for errors
- Verify Node.js version compatibility

## Monitoring

### Railway Logs

To view logs:
1. Click on your service (backend or frontend)
2. Click "Deployments" tab
3. Click on latest deployment
4. View build and runtime logs

### Backend API Logs

All requests are logged. Check Railway logs for:
- Sync requests: `POST /api/sync-payments`
- Frontend requests: `GET /api/payments`, `GET /api/assets`
- Errors and stack traces

## Updating the App

### Update Backend

```bash
cd backend
# Make changes
git add .
git commit -m "Update description"
git push
```

Railway will auto-deploy.

### Update Frontend

```bash
cd frontend
# Make changes
git add .
git commit -m "Update description"
git push
```

Railway will auto-deploy.

### Update Database Schema

1. Connect to Railway PostgreSQL
2. Run SQL commands in the "Query" tab
3. Changes are immediate

### Update Google Apps Script

1. Edit in Apps Script editor
2. Click "Save"
3. Changes are immediate

## Cost & Limits

**Railway Free Tier (Hobby Plan):**
- $5 free credits per month
- Unlimited projects
- PostgreSQL: 500MB storage, 1M rows
- Should be sufficient for personal use

**If you exceed limits:**
- Add payment method for $5/month
- Or optimize by archiving old data

## Security Notes

1. **Never commit `.env` file** - it contains secrets
2. **Password in H1**: Keep sheet private or use a separate config sheet
3. **HTTPS**: Railway provides HTTPS automatically
4. **Rate Limiting**: Backend has rate limits to prevent abuse

## Next Steps

1. Test thoroughly with real data
2. Add more assets to database as needed
3. Customize CAGR values per asset
4. Monitor Railway usage dashboard

You're all set! 🎉
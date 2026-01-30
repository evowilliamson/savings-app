# Complete Directory Setup Guide

Follow these steps to organize your project files correctly for deployment.

## Overview

You'll create THREE separate directories:
1. **khaohom-backend** - For Railway backend deployment
2. **khaohom-frontend** - For Railway frontend deployment  
3. **khaohom-docs** - Optional, for documentation reference

## Step-by-Step Setup

### Step 1: Create Project Root Directory

```bash
# Create a main folder for the entire project
mkdir khaohom-savings-project
cd khaohom-savings-project
```

### Step 2: Setup Backend Directory

```bash
# Create backend directory
mkdir khaohom-backend
cd khaohom-backend

# Copy these files from the project:
# - backend/server.js
# - backend/package.json
# - backend/.env.example
# - backend/.gitignore

# Your backend directory should look like this:
khaohom-backend/
├── server.js
├── package.json
├── .env.example
└── .gitignore

# Initialize git (for Railway deployment)
git init
git add .
git commit -m "Initial backend commit"

# Create .env file from example
cp .env.example .env
# Edit .env and add your settings:
# SYNC_PASSWORD=your-strong-password-here
# NODE_ENV=production
# (DATABASE_URL will be set automatically by Railway)
# (FRONTEND_URL will be added after frontend deployment)
```

### Step 3: Setup Frontend Directory

```bash
# Go back to project root
cd ..

# Create frontend directory
mkdir khaohom-frontend
cd khaohom-frontend

# Copy these files from the project:
# - frontend/index.html
# - frontend/style.css
# - frontend/main.js
# - frontend/vite.config.js
# - frontend/package.json
# - frontend/.gitignore

# Your frontend directory should look like this:
khaohom-frontend/
├── index.html
├── style.css
├── main.js
├── vite.config.js
├── package.json
└── .gitignore

# Initialize git (for Railway deployment)
git init
git add .
git commit -m "Initial frontend commit"

# IMPORTANT: Before deploying, edit main.js line 18
# Change: API_BASE_URL: 'https://your-app.railway.app'
# To: API_BASE_URL: 'https://your-actual-backend-url.railway.app'
```

### Step 4: Keep Documentation (Optional)

```bash
# Go back to project root
cd ..

# Create docs directory
mkdir khaohom-docs
cd khaohom-docs

# Copy these files from the project:
# - README.md
# - QUICKSTART.md
# - DEPLOYMENT.md
# - ARCHITECTURE.md
# - FOLDER_STRUCTURE.md
# - database/schema.sql
# - google-apps-script/Code.gs

# Your docs directory should look like this:
khaohom-docs/
├── README.md
├── QUICKSTART.md
├── DEPLOYMENT.md
├── ARCHITECTURE.md
├── FOLDER_STRUCTURE.md
├── database/
│   └── schema.sql
└── google-apps-script/
    └── Code.gs
```

## Final Directory Structure

After setup, your project root should look like this:

```
khaohom-savings-project/
│
├── khaohom-backend/          ← Deploy this to Railway
│   ├── server.js
│   ├── package.json
│   ├── .env.example
│   ├── .env               ← Create from .env.example (DO NOT COMMIT)
│   ├── .gitignore
│   ├── .git/              ← Git repository for Railway
│   └── node_modules/      ← Created after npm install
│
├── khaohom-frontend/         ← Deploy this to Railway
│   ├── index.html
│   ├── style.css
│   ├── main.js            ← EDIT: Update API_BASE_URL before deploy
│   ├── vite.config.js
│   ├── package.json
│   ├── .gitignore
│   ├── .git/              ← Git repository for Railway
│   ├── node_modules/      ← Created after npm install
│   └── dist/              ← Created after npm run build
│
└── khaohom-docs/             ← Reference only (not deployed)
    ├── README.md
    ├── QUICKSTART.md
    ├── DEPLOYMENT.md
    ├── ARCHITECTURE.md
    ├── FOLDER_STRUCTURE.md
    ├── database/
    │   └── schema.sql     ← Copy to Railway PostgreSQL Query tab
    └── google-apps-script/
        └── Code.gs        ← Copy to Google Apps Script editor
```

## Deployment Checklist

### Backend Deployment

```bash
cd khaohom-backend

# 1. Create .env file
cp .env.example .env

# 2. Edit .env
nano .env  # or use your preferred editor
# Add:
# SYNC_PASSWORD=MyStrongPassword123!
# NODE_ENV=production

# 3. Test locally (optional)
npm install
npm run dev
# Visit http://localhost:3000/health

# 4. Create GitHub repository
# Go to https://github.com/new
# Create repo: khaohom-backend

# 5. Push to GitHub
git remote add origin https://github.com/YOUR-USERNAME/khaohom-backend.git
git branch -M main
git push -u origin main

# 6. Deploy on Railway
# - Go to https://railway.app/new
# - Select "Deploy from GitHub repo"
# - Choose khaohom-backend
# - Add PostgreSQL database
# - Set environment variables in Railway dashboard
# - Get backend URL: https://your-backend-xyz.railway.app
```

### Frontend Deployment

```bash
cd ../khaohom-frontend

# 1. IMPORTANT: Update API URL
nano main.js  # or use your preferred editor
# Line 18: Change to your actual backend URL
# API_BASE_URL: 'https://your-backend-xyz.railway.app'

# 2. Test locally (optional)
npm install
npm run dev
# Visit http://localhost:5173

# 3. Create GitHub repository
# Go to https://github.com/new
# Create repo: khaohom-frontend

# 4. Push to GitHub
git add main.js  # Add the updated file
git commit -m "Update API_BASE_URL"
git remote add origin https://github.com/YOUR-USERNAME/khaohom-frontend.git
git branch -M main
git push -u origin main

# 5. Deploy on Railway
# - Go to https://railway.app/new
# - Select "Deploy from GitHub repo"
# - Choose khaohom-frontend
# - Railway auto-detects Vite
# - Get frontend URL: https://your-frontend-abc.railway.app

# 6. Update backend CORS
# - Go to backend service in Railway
# - Add environment variable:
# FRONTEND_URL=https://your-frontend-abc.railway.app
```

### Database Setup

```bash
# 1. In Railway, click your PostgreSQL database
# 2. Click "Data" tab
# 3. Click "Query"
# 4. Copy contents of khaohom-docs/database/schema.sql
# 5. Paste into Query tab
# 6. Click "Run Query"
# 7. Verify: Should see "Database schema created successfully!"
# 8. Verify: Should see 3 rows in assets table
```

### Google Apps Script Setup

```bash
# 1. Open your Google Sheet
# 2. Go to Extensions → Apps Script
# 3. Delete default code
# 4. Copy contents of khaohom-docs/google-apps-script/Code.gs
# 5. Paste into Apps Script editor
# 6. Update line 23:
#    BACKEND_URL: 'https://your-backend-xyz.railway.app/api/sync-payments'
# 7. Click Save (disk icon)
# 8. Refresh your Google Sheet
# 9. Verify: New menu "Savings Tools" appears
# 10. Add password to cell H1 (must match SYNC_PASSWORD from backend)
```

## Alternative: Monorepo Setup

If you prefer to keep everything in one repository:

```bash
mkdir khaohom-savings
cd khaohom-savings

# Create structure
mkdir -p backend frontend database google-apps-script

# Copy files to respective directories
# backend/ - backend files
# frontend/ - frontend files  
# database/ - schema.sql
# google-apps-script/ - Code.gs

# Initialize single git repo
git init
git add .
git commit -m "Initial commit"

# Deploy to Railway
# - Create project from GitHub repo
# - For backend: Set root directory to "backend"
# - For frontend: Set root directory to "frontend"
```

Your directory structure would be:
```
khaohom-savings/
├── .git/
├── README.md
├── backend/
│   ├── server.js
│   ├── package.json
│   └── ...
├── frontend/
│   ├── index.html
│   ├── main.js
│   └── ...
├── database/
│   └── schema.sql
└── google-apps-script/
    └── Code.gs
```

## Quick Reference

**What goes where:**

| File | Destination | Deploy to Railway? |
|------|-------------|-------------------|
| backend/server.js | khaohom-backend/ | ✅ YES |
| backend/package.json | khaohom-backend/ | ✅ YES |
| backend/.env.example | khaohom-backend/ | ✅ YES |
| backend/.gitignore | khaohom-backend/ | ✅ YES |
| frontend/index.html | khaohom-frontend/ | ✅ YES |
| frontend/style.css | khaohom-frontend/ | ✅ YES |
| frontend/main.js | khaohom-frontend/ | ✅ YES (edit first!) |
| frontend/vite.config.js | khaohom-frontend/ | ✅ YES |
| frontend/package.json | khaohom-frontend/ | ✅ YES |
| frontend/.gitignore | khaohom-frontend/ | ✅ YES |
| database/schema.sql | Reference only | ❌ Copy to Railway |
| google-apps-script/Code.gs | Reference only | ❌ Copy to Apps Script |
| README.md | Reference only | ❌ Documentation |

## Files to Edit Before Deployment

**1. backend/.env** (create from .env.example)
```env
SYNC_PASSWORD=YourStrongPasswordHere123!
NODE_ENV=production
# FRONTEND_URL will be added after frontend deployment
```

**2. frontend/main.js** (line 18)
```javascript
API_BASE_URL: 'https://your-actual-backend.railway.app',
```

**3. google-apps-script/Code.gs** (line 23)
```javascript
BACKEND_URL: 'https://your-actual-backend.railway.app/api/sync-payments',
```

**4. Google Sheet cell H1**
```
Add the same password as SYNC_PASSWORD
```

## Common Mistakes to Avoid

❌ **Don't** put everything in one folder and try to deploy it
✅ **Do** separate backend and frontend into different directories

❌ **Don't** forget to update API_BASE_URL in main.js before deployment
✅ **Do** update it with your actual Railway backend URL

❌ **Don't** commit .env file with secrets
✅ **Do** keep .env local and use Railway environment variables

❌ **Don't** forget to initialize git in backend and frontend directories
✅ **Do** run `git init` in each deployment directory

❌ **Don't** deploy without testing locally first
✅ **Do** run `npm install` and `npm run dev` to test

## Summary Commands

```bash
# Setup everything
mkdir khaohom-savings-project
cd khaohom-savings-project

# Backend
mkdir khaohom-backend
cd khaohom-backend
# Copy backend files here
git init
cd ..

# Frontend  
mkdir khaohom-frontend
cd khaohom-frontend
# Copy frontend files here
git init
cd ..

# Docs (optional)
mkdir khaohom-docs
cd khaohom-docs
# Copy documentation files here
cd ..
```

Now you're ready to deploy! 🚀
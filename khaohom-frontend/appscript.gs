/**
 * Khaohom's Savings - Google Apps Script
 * 
 * This script syncs payment data from Google Sheets to the backend API.
 * 
 * Setup Instructions:
 * 1. Open your Google Sheet
 * 2. Go to Extensions > Apps Script
 * 3. Copy this entire code into the script editor
 * 4. Update the BACKEND_URL constant below with your Railway backend URL
 * 5. Save the script
 * 6. Refresh your Google Sheet - you should see a new menu "Savings Tools"
 * 7. Make sure password is in cell H1 of "Kaohom Savings Account" tab
 */

// ============================================================================
// CONFIGURATION
// ============================================================================

const CONFIG = {
  // IMPORTANT: Update this with your Railway backend URL
  BACKEND_URL: 'https://your-app.railway.app/api/sync-payments',
  
  // Sheet configuration
  SHEET_NAME: 'Kaohom Savings Account',
  PASSWORD_CELL: 'H1',
  DATA_START_ROW: 29,
  
  // Column indices (A=0, B=1, etc.)
  COLUMNS: {
    DATE: 0,        // A
    AMOUNT: 1,      // B
    THB_PRICE: 2,   // C
    USD: 3,         // D
    USD_CUM: 4,     // E
    ASSET: 5,       // F (formerly "Description")
    REASON: 6,      // G
    NOTE: 7,        // H
    USDTHB: 8       // I
  }
};

// ============================================================================
// MENU CREATION
// ============================================================================

/**
 * Creates custom menu when sheet opens
 */
function onOpen() {
  const ui = SpreadsheetApp.getUi();
  ui.createMenu('Savings Tools')
    .addItem('Sync Payments', 'syncPayments')
    .addSeparator()
    .addItem('Test Connection', 'testConnection')
    .addToUi();
}

// ============================================================================
// MAIN SYNC FUNCTION
// ============================================================================

/**
 * Main function to sync all payments from sheet to backend
 */
function syncPayments() {
  const ui = SpreadsheetApp.getUi();
  
  try {
    // Get the sheet
    const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(CONFIG.SHEET_NAME);
    if (!sheet) {
      ui.alert('Error', `Sheet "${CONFIG.SHEET_NAME}" not found!`, ui.ButtonSet.OK);
      return;
    }
    
    // Get password from H1
    const password = sheet.getRange(CONFIG.PASSWORD_CELL).getValue();
    if (!password) {
      ui.alert('Error', `Password not found in cell ${CONFIG.PASSWORD_CELL}!`, ui.ButtonSet.OK);
      return;
    }
    
    // Show loading message
    const response = ui.alert(
      'Sync Payments',
      'This will sync all payments from row 29 onwards to the backend. Continue?',
      ui.ButtonSet.YES_NO
    );
    
    if (response !== ui.Button.YES) {
      return;
    }
    
    // Get all data starting from row 29
    const lastRow = sheet.getLastRow();
    if (lastRow < CONFIG.DATA_START_ROW) {
      ui.alert('Info', 'No data found to sync.', ui.ButtonSet.OK);
      return;
    }
    
    const numRows = lastRow - CONFIG.DATA_START_ROW + 1;
    const dataRange = sheet.getRange(CONFIG.DATA_START_ROW, 1, numRows, 9); // Columns A-I
    const data = dataRange.getValues();
    
    // Parse payments
    const payments = [];
    let skippedRows = 0;
    
    for (let i = 0; i < data.length; i++) {
      const row = data[i];
      const rowNumber = CONFIG.DATA_START_ROW + i;
      
      // Skip empty rows (check if date is empty)
      if (!row[CONFIG.COLUMNS.DATE]) {
        skippedRows++;
        continue;
      }
      
      // Parse the row into payment object
      const payment = {
        date: formatDate(row[CONFIG.COLUMNS.DATE]),
        amount: parseFloat(row[CONFIG.COLUMNS.AMOUNT]) || 0,
        thb_price: parseFloat(row[CONFIG.COLUMNS.THB_PRICE]) || null,
        usd_value: parseFloat(row[CONFIG.COLUMNS.USD]) || 0,
        usd_cum: parseFloat(row[CONFIG.COLUMNS.USD_CUM]) || 0,
        asset: String(row[CONFIG.COLUMNS.ASSET]).trim().toUpperCase(),
        reason: String(row[CONFIG.COLUMNS.REASON]).trim() || null,
        status: String(row[CONFIG.COLUMNS.NOTE]).trim() || 'not paid',
        usdthb_rate: parseFloat(row[CONFIG.COLUMNS.USDTHB]) || 0
      };
      
      // Validate required fields
      if (!payment.date || !payment.asset || payment.usd_value === 0 || payment.usdthb_rate === 0) {
        Logger.log(`Row ${rowNumber}: Skipping - missing required fields`);
        skippedRows++;
        continue;
      }
      
      payments.push(payment);
    }
    
    if (payments.length === 0) {
      ui.alert('Info', 'No valid payments found to sync.', ui.ButtonSet.OK);
      return;
    }
    
    Logger.log(`Found ${payments.length} valid payments to sync (${skippedRows} rows skipped)`);
    
    // Send to backend
    const payload = {
      password: password,
      payments: payments
    };
    
    const options = {
      method: 'post',
      contentType: 'application/json',
      payload: JSON.stringify(payload),
      muteHttpExceptions: true
    };
    
    Logger.log(`Sending ${payments.length} payments to ${CONFIG.BACKEND_URL}`);
    
    const apiResponse = UrlFetchApp.fetch(CONFIG.BACKEND_URL, options);
    const statusCode = apiResponse.getResponseCode();
    const responseText = apiResponse.getContentText();
    
    Logger.log(`Response status: ${statusCode}`);
    Logger.log(`Response body: ${responseText}`);
    
    // Handle response
    if (statusCode === 200 || statusCode === 207) {
      const result = JSON.parse(responseText);
      let message = result.message || 'Sync completed';
      
      if (result.errors && result.errors.length > 0) {
        message += '\n\nWarnings:\n' + result.errors.slice(0, 5).join('\n');
        if (result.errors.length > 5) {
          message += `\n... and ${result.errors.length - 5} more errors`;
        }
      }
      
      ui.alert('Success', message, ui.ButtonSet.OK);
    } else if (statusCode === 401) {
      ui.alert('Error', 'Invalid password. Please check cell H1.', ui.ButtonSet.OK);
    } else {
      const errorMsg = tryParseError(responseText);
      ui.alert('Error', `Sync failed: ${errorMsg}`, ui.ButtonSet.OK);
    }
    
  } catch (error) {
    Logger.log(`Error: ${error.toString()}`);
    ui.alert('Error', `Sync failed: ${error.toString()}`, ui.ButtonSet.OK);
  }
}

// ============================================================================
// TEST FUNCTION
// ============================================================================

/**
 * Test connection to backend
 */
function testConnection() {
  const ui = SpreadsheetApp.getUi();
  
  try {
    const healthUrl = CONFIG.BACKEND_URL.replace('/api/sync-payments', '/health');
    const response = UrlFetchApp.fetch(healthUrl, { muteHttpExceptions: true });
    const statusCode = response.getResponseCode();
    
    if (statusCode === 200) {
      ui.alert('Success', 'Backend connection successful!', ui.ButtonSet.OK);
    } else {
      ui.alert('Warning', `Backend returned status ${statusCode}`, ui.ButtonSet.OK);
    }
  } catch (error) {
    ui.alert('Error', `Connection failed: ${error.toString()}`, ui.ButtonSet.OK);
  }
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

/**
 * Format date to YYYY-MM-DD
 */
function formatDate(dateValue) {
  if (!dateValue) return null;
  
  let date;
  if (dateValue instanceof Date) {
    date = dateValue;
  } else if (typeof dateValue === 'string') {
    date = new Date(dateValue);
  } else {
    return null;
  }
  
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  
  return `${year}-${month}-${day}`;
}

/**
 * Try to parse error message from response
 */
function tryParseError(responseText) {
  try {
    const json = JSON.parse(responseText);
    return json.error || json.message || responseText;
  } catch (e) {
    return responseText.substring(0, 200);
  }
}
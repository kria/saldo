/*
 * Saldo - http://github.com/kria/saldo
 * 
 * Copyright (C) 2010 Kristian Adrup
 * 
 * This file is part of Saldo.
 * 
 * Saldo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Saldo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.adrup.saldo.widget;

import com.adrup.saldo.Account;
import com.adrup.saldo.AccountHashKey;
import com.adrup.saldo.DatabaseAdapter;
import com.adrup.saldo.R;
import com.adrup.saldo.Util;
import com.adrup.saldo.bank.BankException;
import com.adrup.saldo.bank.BankLogin;
import com.adrup.saldo.bank.BankManager;
import com.adrup.saldo.bank.BankManagerFactory;
import com.adrup.util.NumberUtil;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.Map;

/**
 * A Service that facilitates updates of the widgets. Does both online querying and database persistance.
 * 
 * @author Kristian Adrup
 *
 */
public class WidgetService extends Service {
	private static final String TAG = "WidgetUpdateService";
	public static final String ACTION_UPDATE = "com.adrup.saldo.widget.intent.action.UPDATE";
	public static final String ACTION_NEXT_ACCOUNT = "com.adrup.saldo.widget.intent.action.NEXT_ACCOUNT";
	public static final String ACTION_PREVIOUS_ACCOUNT = "com.adrup.saldo.widget.intent.action.PREVIOUS_ACCOUNT";
	public static final String UPDATE_INTENT_EXTRA_ACCOUNT_ID = "com.adrup.saldo.account_id";
	
	private int appWidgetId;
	
	
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate()");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(TAG, "-> onStart()");
		appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		if (appWidgetId == -1) {
			Log.e(TAG, "No appWidgetId.. that's unpossible!");
			return;
		}
		Log.d(TAG, "appWidgetId: " + appWidgetId);
		
		String action = intent.getAction();
		if (ACTION_UPDATE.equals(action)) {
			new UpdateTask().execute();
		} else if (ACTION_NEXT_ACCOUNT.equals(action)) {
			pageAccount(true);
		} else if (ACTION_PREVIOUS_ACCOUNT.equals(action)) {
			pageAccount(false);
		}
		Log.d(TAG, "<- onStart()");
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind()");
		return null;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
	}

	private void pageAccount(boolean forward) {
		int currentAccountId = WidgetConfigurationActivity.loadAccountIdPref(WidgetService.this, appWidgetId);
		if (currentAccountId == -1) {
			Log.e(TAG, "No currentAccountId");
			return;
		}
		String accounts = WidgetConfigurationActivity.loadAccountIdsPref(WidgetService.this, appWidgetId);
		if (accounts == null) {
			Log.e(TAG, "No accounts");
			return;
		}
		String[] accountarr = accounts.split(",");
		int currIndex = -1;
		int newIndex = 0;
		int noOfAccounts = accountarr.length;
		for (int i = 0; i < noOfAccounts; i++) {
			if (accountarr[i].equals(String.valueOf(currentAccountId))) {
				currIndex = i;
				break;
			}
		}
		if (currIndex == -1) {
			Log.d(TAG, "Unknown currIndex, starting over");
			newIndex = 0;
		} else if (forward) {
			if (currIndex == noOfAccounts - 1) newIndex = 0;
			else newIndex = currIndex + 1;
		} else {
			if (currIndex == 0) newIndex = noOfAccounts - 1;
			else newIndex = currIndex - 1;
		}
		int newAccountId = Integer.parseInt(accountarr[newIndex]);
		WidgetConfigurationActivity.saveAccountIdPref(this, appWidgetId, newAccountId);
		
		RemoteViews views = SaldoWidgetProvider.buildUpdate(WidgetService.this, appWidgetId, newAccountId, newIndex, noOfAccounts, true);
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(WidgetService.this);
		widgetManager.updateAppWidget(appWidgetId, views);
	}
	
	private class UpdateTask extends AsyncTask<Void, String, Void> {
		private static final String TAG = "WidgetUpdateService.UpdateTask";

		@Override
		protected Void doInBackground(Void... progress) {
			Log.d(TAG, "doInBackground()");
			run();
			return null;
		}

		@Override
		protected void onCancelled() {
			Log.d(TAG, "onCancelled()");
		}

		@Override
		protected void onProgressUpdate(String... progress) {
			Log.d(TAG, "onProgressUpdate: " + progress[0]);
			Toast.makeText(WidgetService.this, progress[0], Toast.LENGTH_LONG).show();
		}

		@Override
		protected void onPostExecute(Void result) {
			Log.d(TAG, "onPostExecute()");
		}
		
		private void run() {
			
			int accountId = WidgetConfigurationActivity.loadAccountIdPref(WidgetService.this, appWidgetId);
			if (accountId == -1) {
				publishProgress("No accountId.. that's unpossible!");
				Log.e(TAG, "No accountId.. that's unpossible!");
				return;
			}
			
			DatabaseAdapter dbAdapter = new DatabaseAdapter(WidgetService.this);
			dbAdapter.open();
			Account dbAccount = dbAdapter.fetchAccount(accountId);
			BankLogin bankLogin = dbAdapter.fetchBankLogin(dbAccount.getBankLoginId());
			BankManager bankManager = null;
			try {
				bankManager = BankManagerFactory.createBankManager(bankLogin);
			} catch (BankException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
						
			RemoteViews views = SaldoWidgetProvider.buildUpdate(WidgetService.this, appWidgetId, accountId, true);
			AppWidgetManager widgetManager = AppWidgetManager.getInstance(WidgetService.this);
			views.setViewVisibility(R.id.layout_widget_progress, View.VISIBLE);
			widgetManager.updateAppWidget(appWidgetId, views);
			try {
				
				// Show progress bar
				
				Map<AccountHashKey, Account> accounts = bankManager.getAccounts();

				Account account = accounts.get(new AccountHashKey(dbAccount.getRemoteId(), bankLogin.getId()));
				if (account == null) {
					publishProgress("unable to get account");
					Log.e(TAG, "account is null!");
					views.setViewVisibility(R.id.layout_widget_progress, View.GONE);
					widgetManager.updateAppWidget(appWidgetId, views);
					
					//send progress error
					return;
				}
				long newBalance = account.getBalance();
				Log.d(TAG, "newBalance= " + newBalance);
				
				//Get previous balance from db
				long prevBalance = 0;
				Log.d(TAG, "getting account from db");
				dbAdapter = new DatabaseAdapter(WidgetService.this);
				
				try {
					dbAdapter.open();
					dbAccount = dbAdapter.fetchAccount(accountId);
					if (dbAccount != null) {
						prevBalance = dbAccount.getBalance();
						Log.d(TAG, "widget balance from from db: " + dbAccount.getBalance());
					}
				} catch (SQLException e) {
					Log.e(TAG, "SQLException reading account in run", e);
				} finally {
					dbAdapter.close();
				}
				Log.d(TAG, "prevBalance= " + prevBalance);
				
				// Toast.makeText(this, "balance= " + balance,
				// Toast.LENGTH_SHORT).show();

				int steps = 20; // number of balance animation steps
				long diff = newBalance - prevBalance;
				
				if (diff != 0) {
					long inc = NumberUtil.roundToSignificantFigures(diff / steps, 1);
					if (inc == 0) inc = diff > 0 ? 1 : -1;
					Log.d(TAG, "inc= " + inc);
					long tempBalance = prevBalance + inc;
					int i = 1;
					int sanityCount = 25;
					while (i < sanityCount && (diff > 0 ? tempBalance < newBalance : tempBalance > newBalance)) {
						Log.d(TAG, "tempBalance(" + i + ")= " + tempBalance);
						i++;
						views.setTextViewText(R.id.layout_widget_balance, Util.toCurrencyString(tempBalance));
						widgetManager.updateAppWidget(appWidgetId, views);
						tempBalance += inc;
					}
					// set final balance
					Log.d(TAG, "final Balance= " + newBalance);
					views.setTextViewText(R.id.layout_widget_balance, Util.toCurrencyString(newBalance));
					// Set back current accountId if someone changed it with arrow keys during update
					WidgetConfigurationActivity.saveAccountIdPref(WidgetService.this, appWidgetId, accountId);
				} else Log.d(TAG, "no balance change");
				// Hide progress bar
				views.setViewVisibility(R.id.layout_widget_progress, View.GONE);
				widgetManager.updateAppWidget(appWidgetId, views);
				Log.d(TAG, "widget updated");

				Log.d(TAG, "writing accounts to db");
				// Save accounts to database..
				dbAdapter = new DatabaseAdapter(WidgetService.this);
				try {
					dbAdapter.open();
					for (Account acc : accounts.values()) {
						boolean result = dbAdapter.saveAccount(acc);
						Log.d(TAG, "createAccount result= " + result);
					}
					Log.d(TAG, "db updated");
				} catch (SQLException e) {
					Log.e(TAG, "SQLException in run()", e);
				} finally {
					dbAdapter.close();
				}

			} catch (BankException e) {
				Log.e(TAG, e.getMessage(), e);
				publishProgress(e.getMessage());
				//TODO: alot of hiding going on, move this stuff to one place
				views.setViewVisibility(R.id.layout_widget_progress, View.GONE);
				widgetManager.updateAppWidget(appWidgetId, views);
			} catch (Exception e) {
				Log.e(TAG, e.getMessage(), e);
				publishProgress(e.getMessage());
				views.setViewVisibility(R.id.layout_widget_progress, View.GONE);
				widgetManager.updateAppWidget(appWidgetId, views);
			}
			
		}
	}
	public static Uri createUri(int appWidgetId) {
		return Uri.parse("saldo://widget/?appwidget_id=" + appWidgetId);
	}
}
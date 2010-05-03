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
public class WidgetUpdateService extends Service {
	private static final String TAG = "WidgetUpdateService";
	public static final String UPDATE_INTENT_EXTRA_ACCOUNT_ID = "com.adrup.saldo.account_id";
	//public static final String UPDATE_INTENT_EXTRA_WIDGET_ID = "com.adrup.saldo.widget_id";
	
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
		new UpdateTask().execute();
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
			Toast.makeText(WidgetUpdateService.this, progress[0], Toast.LENGTH_LONG).show();
		}

		@Override
		protected void onPostExecute(Void result) {
			Log.d(TAG, "onPostExecute()");
		}
		
		private void run() {
			
			int accountId = WidgetConfigurationActivity.loadAccountIdPref(WidgetUpdateService.this, appWidgetId);
			if (accountId == -1) {
				publishProgress("No accountId.. that's unpossible!");
				Log.e(TAG, "No accountId.. that's unpossible!");
				return;
			}
			
			DatabaseAdapter dbAdapter = new DatabaseAdapter(WidgetUpdateService.this);
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
						
			RemoteViews views = SaldoWidgetProvider.buildUpdate(WidgetUpdateService.this, appWidgetId, accountId, true);
			AppWidgetManager widgetManager = AppWidgetManager.getInstance(WidgetUpdateService.this);
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
				dbAdapter = new DatabaseAdapter(WidgetUpdateService.this);
				
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
				long inc = NumberUtil.roundToSignificantFigures(diff / steps, 1);
				Log.d(TAG, "inc= " + inc);
				long tempBalance = prevBalance + inc;
				int i = 1;
				while (diff > 0 ? tempBalance < newBalance : tempBalance > newBalance) {
					Log.d(TAG, "tempBalance(" + i + ")= " + tempBalance);
					i++;
					views.setTextViewText(R.id.layout_widget_text, Util.toCurrencyString(tempBalance));
					widgetManager.updateAppWidget(appWidgetId, views);
					tempBalance += inc;
				}
				// set final balance
				Log.d(TAG, "final Balance= " + newBalance);
				views.setTextViewText(R.id.layout_widget_text, Util.toCurrencyString(newBalance));
				// Hide progress bar
				views.setViewVisibility(R.id.layout_widget_progress, View.GONE);
				widgetManager.updateAppWidget(appWidgetId, views);
				Log.d(TAG, "widget updated");

				Log.d(TAG, "writing accounts to db");
				// Save accounts to database..
				dbAdapter = new DatabaseAdapter(WidgetUpdateService.this);
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
}
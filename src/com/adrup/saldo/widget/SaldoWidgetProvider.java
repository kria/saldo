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
import com.adrup.saldo.AutoUpdateReceiver;
import com.adrup.saldo.DatabaseAdapter;
import com.adrup.saldo.R;
import com.adrup.saldo.Saldo;
import com.adrup.saldo.Util;
import com.adrup.saldo.bank.BankLogin;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * The Class that handles the lifecycle and redraw of the Saldo widgets.
 * 
 * @author Kristian Adrup
 *
 */
public class SaldoWidgetProvider extends AppWidgetProvider {
	private static final String TAG = "SaldoWidget";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d(TAG, "onUpdate()");

		final int N = appWidgetIds.length;
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];
			updateWidget(context, appWidgetManager, appWidgetId);
		}
	}
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive()");
		super.onReceive(context, intent);
		String action = intent.getAction();
        Log.d(TAG, "action:" + action);
        if(action.contentEquals(AutoUpdateReceiver.WIDGET_REFRESH)){
        	AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        	ComponentName thisWidget = new ComponentName(context, SaldoWidgetProvider.class);
        	int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        	final int N = appWidgetIds.length;
    		for (int i = 0; i < N; i++) {
    			int appWidgetId = appWidgetIds[i];
    			updateWidget(context, appWidgetManager, appWidgetId);
    		}
        }
	}
	
	private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
		int accountId = WidgetConfigurationActivity.loadAccountIdPref(context, appWidgetId);
		if (accountId >= 0) {
			RemoteViews views = buildUpdate(context, appWidgetId, accountId, true);
			appWidgetManager.updateAppWidget(appWidgetId, views);
		} else {
			Log.d(TAG, "No accountId set for appWidgetId " + appWidgetId);
		}
	}
	
	static RemoteViews buildUpdate(Context context, int appWidgetId, int accountId, boolean readDb) {
		int currIndex = -1;
		int noOfAccounts = 0;
		
		String accounts = WidgetConfigurationActivity.loadAccountIdsPref(context, appWidgetId);
		if (accounts != null) {
			String[] accountarr = accounts.split(",");
			
			noOfAccounts = accountarr.length;
			for (int i = 0; i < noOfAccounts; i++) {
				if (accountarr[i].equals(String.valueOf(accountId))) {
					currIndex = i;
					break;
				}
			}
		}
		if (currIndex == -1) {
			Log.d(TAG, "Unknown currIndex, starting over");
			currIndex = 0;
		} 
		return buildUpdate(context, appWidgetId, accountId, currIndex, noOfAccounts, true);
	}
	static RemoteViews buildUpdate(Context context, int appWidgetId, int accountId, int currentAccountIndex, int noOfAccounts,  boolean readDb) {
		Log.d(TAG, "buildUpdate()");
		// Get the layout for the widget
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.saldo_appwidget);

		// Attach on-click listeners to buttons
		
		// Main Saldo activity launch button
		Intent intent = new Intent(context, Saldo.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		views.setOnClickPendingIntent(R.id.layout_widget_launcher, pendingIntent);
		
		// Update button
		Uri uri = WidgetService.createUri(appWidgetId);
		intent = new Intent(context, WidgetService.class);
		intent.setAction(WidgetService.ACTION_UPDATE);
		intent.setData(uri);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.layout_widget_btn_update, pendingIntent);
		// Next account button
		intent = new Intent(context, WidgetService.class);
		intent.setAction(WidgetService.ACTION_NEXT_ACCOUNT);
		intent.setData(uri);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.widget_arrow_up_btn, pendingIntent);
		// Previous account button
		intent = new Intent(context, WidgetService.class);
		intent.setAction(WidgetService.ACTION_PREVIOUS_ACCOUNT);
		intent.setData(uri);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.widget_arrow_down_btn, pendingIntent);
		
		//pager info
		views.setTextViewText(R.id.layout_widget_pager, String.format("%s/%s", currentAccountIndex + 1, noOfAccounts));
		
		// Update widget info from db if it exists
		Log.d(TAG, "getting account from db");
		DatabaseAdapter dbAdapter = new DatabaseAdapter(context);

		// if first time skip db read
		if (!readDb)
			return views;

		try {
			dbAdapter.open();
			Account account = dbAdapter.fetchAccount(accountId);
			if (account != null) {
				StringBuilder accountName = new StringBuilder();
				BankLogin bankLogin = dbAdapter.fetchBankLogin(account.getBankLoginId());
				if (bankLogin != null) {
					accountName.append(bankLogin.getName());
					accountName.append(": ");
				}
				accountName.append(account.getName());
				views.setTextViewText(R.id.layout_widget_account, accountName.toString());
				views.setTextViewText(R.id.layout_widget_balance, Util.toCurrencyString(account.getBalance()));
				Log.d(TAG, "widget balance set from db: " + account.getBalance());
			}
		} catch (SQLException e) {
			Log.e(TAG, "SQLException reading account in buildUpdate", e);
		} finally {
			dbAdapter.close();
		}

		return views;
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		Log.d(TAG, "onDeleted()");
		// When the user deletes the widget, delete the preference associated with it.

		final int N = appWidgetIds.length;
		for (int i = 0; i < N; i++) {
			Log.d(TAG, "widget " + i + "deleted");
			WidgetConfigurationActivity.deleteAccountIdPref(context, i);
			WidgetConfigurationActivity.deleteAccountIdsPref(context, i);
		}
	}

	@Override
	public void onDisabled(Context context) {
		Log.d(TAG, "onDisabled()");
	}

	@Override
	public void onEnabled(Context context) {
		Log.d(TAG, "onEnabled()");
	}


}

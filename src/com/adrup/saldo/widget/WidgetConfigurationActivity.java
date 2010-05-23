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

import com.adrup.saldo.AccountsViewBinder;
import com.adrup.saldo.DatabaseAdapter;
import com.adrup.saldo.R;
import com.adrup.saldo.bank.Account;

import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;

/**
 * An activity that displays the configuration screen that let's the user pick an account, when adding a widget.
 * 
 * @author Kristian Adrup
 * 
 */
public class WidgetConfigurationActivity extends ListActivity {
	private static final String TAG = "WidgetConfigurationActivity";

	private static final String PREFS_NAME = "com.adrup.saldo.widget.SaldoWidgetProvider";
	private static final String ACCOUNT_ID_PREFIX_KEY = "account_id_";
	private static final String ACCOUNT_IDS_PREFIX_KEY = "account_ids_";

	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	private Cursor mCursor;
	private DatabaseAdapter dbAdapter;
	private ListView listView;

	public WidgetConfigurationActivity() {
		super();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);

		// Set the result to CANCELED. This will cause the widget host to cancel
		// out of the widget placement if they press the back button.
		setResult(RESULT_CANCELED);

		// Set the view layout resource to use.
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.widget_configuration);
		


		// Bind the action for the save button.
		Button okButton = (Button) findViewById(R.id.layout_widget_config_ok_btn);
		okButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				addWidget();
			}
		});
		// Find the widget id from the intent.
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		// If they gave us an intent without the widget id, just bail.
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
		}

		// mAppWidgetPrefix.setText(loadTitlePref(WidgetConfigurationActivity.this, mAppWidgetId));

		dbAdapter = new DatabaseAdapter(this);
		dbAdapter.open(); // TODO: try/catch

		mCursor = dbAdapter.fetchAllAccountsCursor();
		startManagingCursor(mCursor);

		// Now create a new list adapter bound to the cursor.
		// SimpleListAdapter is designed for binding to a Cursor.
		// TODO: fix a Checkable layout, this one is crap
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, // Context.
				R.layout.widget_multi_accounts_list_item, // Specify the row template to use (here, two columns bound to
															// the two retrieved cursor rows).
				mCursor, // Pass in the cursor to bind to.
				new String[] { Account.KEY_NAME }, // Array of cursor columns
				// to bind to.
				new int[] { android.R.id.text1 }); // Parallel array of which template objects to bind to
		// those columns.

		adapter.setViewBinder(new AccountsViewBinder());
		// Bind to our new adapter.
		setListAdapter(adapter);
		listView = getListView();

		listView.setItemsCanFocus(false);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume()");
		super.onResume();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause()");
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onPause()");
		super.onDestroy();
		dbAdapter.close();
	}

	protected void addWidget() {
		int noOfAccounts = listView.getCount();

		ArrayList<Integer> selectedAccountIds = new ArrayList<Integer>();
		for (int pos = 0; pos < noOfAccounts; pos++) {
			if (listView.isItemChecked(pos)) {
				selectedAccountIds.add((int) listView.getItemIdAtPosition(pos));
			}
		}
		int noOfSelectedAccounts = selectedAccountIds.size();
		if (noOfSelectedAccounts == 0) {
			finish();
		} else {
			int accountId = selectedAccountIds.get(0);

			String accountIdsString = TextUtils.join(",", selectedAccountIds);
			saveAccountIdPref(this, mAppWidgetId, accountId);
			saveAccountIdsPref(this, mAppWidgetId, accountIdsString);

			// Push widget update to surface
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
			RemoteViews views = SaldoWidgetProvider.buildUpdate(this, mAppWidgetId, accountId, 0, noOfSelectedAccounts,
					true);
			appWidgetManager.updateAppWidget(mAppWidgetId, views);

			// Make sure we pass back the original appWidgetId
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			setResult(RESULT_OK, resultValue);
			finish();
		}

	}

	// Write the accountId to the SharedPreferences object for this widget
	static void saveAccountIdPref(Context context, int appWidgetId, int accountId) {
		SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();

		prefs.putInt(ACCOUNT_ID_PREFIX_KEY + appWidgetId, accountId);
		prefs.commit();
	}

	// Read the prefix from the SharedPreferences object for this widget.
	// If there is no preference saved, get the default from a resource
	static int loadAccountIdPref(Context context, int appWidgetId) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		int accountId = prefs.getInt(ACCOUNT_ID_PREFIX_KEY + appWidgetId, -1);
		return accountId;
	}

	static void deleteAccountIdPref(Context context, int appWidgetId) {
		SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
		prefs.remove(ACCOUNT_ID_PREFIX_KEY + appWidgetId);
		prefs.commit();
	}

	static void saveAccountIdsPref(Context context, int appWidgetId, String accountIds) {
		SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();

		prefs.putString(ACCOUNT_IDS_PREFIX_KEY + appWidgetId, accountIds);
		prefs.commit();
	}

	static String loadAccountIdsPref(Context context, int appWidgetId) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
		String accountIds = prefs.getString(ACCOUNT_IDS_PREFIX_KEY + appWidgetId, null);
		return accountIds;
	}

	static void deleteAccountIdsPref(Context context, int appWidgetId) {
		SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
		prefs.remove(ACCOUNT_IDS_PREFIX_KEY + appWidgetId);
		prefs.commit();
	}

}

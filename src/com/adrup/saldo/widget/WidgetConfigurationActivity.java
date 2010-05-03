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
import com.adrup.saldo.AccountsViewBinder;
import com.adrup.saldo.DatabaseAdapter;
import com.adrup.saldo.R;

import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
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

	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	EditText mAppWidgetPrefix;

	private Cursor mCursor;
	private DatabaseAdapter dbAdapter;

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
		// setContentView(R.layout.widget_configuration);

		// Find the EditText
		// mAppWidgetPrefix = (EditText)findViewById(R.id.appwidget_prefix);

		// Bind the action for the save button.
		// findViewById(R.id.save_button).setOnClickListener(mOnClickListener);

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
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, // Context.
				R.layout.widget_config_list_item, // Specify the row template to use (here, two columns bound to the two
													// retrieved cursor rows).
				mCursor, // Pass in the cursor to bind to.
				new String[] { Account.KEY_ORDINAL, Account.KEY_NAME, Account.KEY_BALANCE }, // Array of cursor columns
																								// to bind to.
				new int[] { R.id.layout_widget_config_ordinal, R.id.layout_widget_config_name,
						R.id.layout_widget_config_balance }); // Parallel array of which template objects to bind to
																// those columns.

		adapter.setViewBinder(new AccountsViewBinder());
		// Bind to our new adapter.
		setListAdapter(adapter);

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
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		int accountId = (int)id;
		saveAccountIdPref(this, mAppWidgetId, accountId);
		

        // Push widget update to surface
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        RemoteViews views = SaldoWidgetProvider.buildUpdate(this, mAppWidgetId, accountId, true);
        appWidgetManager.updateAppWidget(mAppWidgetId, views);

		// Make sure we pass back the original appWidgetId
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);
		finish();
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

	static void loadAllTitlePrefs(Context context, ArrayList<Integer> appWidgetIds, ArrayList<String> texts) {
	}

}


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

package com.adrup.saldo;

import com.adrup.saldo.bank.BankException;
import com.adrup.saldo.bank.BankLogin;
import com.adrup.saldo.bank.BankManager;
import com.adrup.saldo.bank.BankManagerFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Main entry for application.
 * 
 * @author Kristian Adrup
 *
 */
public class Saldo extends Activity {
	private static final String TAG = "Saldo";
	static final int DIALOG_ABOUT_ID = 0;
	private Cursor mCursor = null;
	private DatabaseAdapter mDbAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Button button = (Button) findViewById(R.id.layout_main_btn_go);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				new UpdateAccountsTask().execute();
			}
		});

		// set uop cursor
		mDbAdapter = new DatabaseAdapter(this);
		try {
			mDbAdapter.open();

			mCursor = mDbAdapter.fetchAllAccountsCursor();
			startManagingCursor(mCursor);

			// Now create a new list adapter bound to the cursor.
			// SimpleListAdapter is designed for binding to a Cursor.
			SimpleCursorAdapter adapter = new SimpleCursorAdapter(Saldo.this, // Context.
					R.layout.widget_config_list_item, // Specify the row template to use (here, two columns bound to the
														// two
					// retrieved cursor rows).
					mCursor, // Pass in the cursor to bind to.
					new String[] { Account.KEY_ORDINAL, Account.KEY_NAME, Account.KEY_BALANCE }, // Array of cursor
																									// columns
					// to bind to.
					new int[] { R.id.layout_widget_config_ordinal, R.id.layout_widget_config_name,
							R.id.layout_widget_config_balance }); // Parallel array of which template objects to bind to
			// those columns.

			adapter.setViewBinder(new AccountsViewBinder());
			// Bind to our new adapter.
			ListView myList = (ListView) findViewById(R.id.layout_main_accounts);
			myList.setAdapter(adapter);

		} catch (SQLException e) {
			Log.e(TAG, "SQLException in onCreate()", e);
		} catch (Exception e) {
			Log.e(TAG, "Exception in onCreate()", e);
		} finally {
			// dbAdapter.close();
		}
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
	protected void onSaveInstanceState(Bundle outState) {
		Log.d(TAG, "onSaveInstanceState()");
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy()");
		mDbAdapter.close();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu()");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.d(TAG, "onPrepareOptionsMenu()");
		super.onPrepareOptionsMenu(menu);

		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Log.d(TAG, "onMenuItemSelected()");
		switch (item.getItemId()) {
		case R.id.menu_options_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		case R.id.menu_options_about:
			showDialog(DIALOG_ABOUT_ID);
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Log.d(TAG, "onCreateDialog()");
		Dialog dialog;
		switch (id) {
		case DIALOG_ABOUT_ID:
			AlertDialog alertDialog;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			Context mContext = this;
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.about, null);
			TextView version = (TextView) layout.findViewById(R.id.layout_about_version);
			version.setText(getString(R.string.version, getVersionName()));
			builder = new AlertDialog.Builder(mContext);
			builder.setView(layout);
			alertDialog = builder.create();
			alertDialog.setTitle(R.string.menu_item_about);
			alertDialog.setIcon(android.R.drawable.ic_dialog_info);
			dialog = alertDialog;
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	/**
	 * Get the version name for the application
	 * 
	 * @return version name as String
	 */
	public String getVersionName() {
		PackageInfo pinfo;
		try {
			pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			return pinfo.versionName;
		} catch (NameNotFoundException e) {
			return null;
		}

	}

	private class UpdateAccountsTask extends AsyncTask<Void, String, Map<AccountHashKey, Account>> {
		private static final String TAG = "Saldo.UpdateAccountsTask";
		private View progress = null;
		private TextView message = null;

		@Override
		protected void onPreExecute() {
			progress = Saldo.this.findViewById(R.id.layout_main_progress);
			progress.setVisibility(View.VISIBLE);
			message = (TextView) Saldo.this.findViewById(R.id.layout_main_message);
		}

		@Override
		protected Map<AccountHashKey, Account> doInBackground(Void... params) {
			Log.d(TAG, "doInBackground()");

			List<BankLogin> bankLogins = mDbAdapter.fetchAllBankLogins();

			if (bankLogins.isEmpty()) {
				publishProgress("No banks added!");
				return null;
			}

			Map<AccountHashKey, Account> accounts = new LinkedHashMap<AccountHashKey, Account>();

			for (BankLogin bankLogin : bankLogins) {
				try {
					BankManager bankManager = BankManagerFactory.createBankManager(bankLogin);
					String msg = String.format("Fetching accounts from %s.", bankLogin.getName());
					Log.d(TAG, msg);
					publishProgress(msg);
					bankManager.getAccounts(accounts);
				} catch (BankException e) {
					Log.e(TAG, e.getMessage(), e);
					publishProgress(e.getMessage());
				}
			}

			return accounts;
		}

		@Override
		protected void onCancelled() {
			Log.d(TAG, "onCancelled()");
			progress.setVisibility(View.INVISIBLE);
		}

		@Override
		protected void onProgressUpdate(String... progress) {
			Log.d(TAG, "onProgressUpdate: " + progress[0]);
			message.setText(progress[0]);

		}

		@Override
		protected void onPostExecute(Map<AccountHashKey, Account> accounts) {
			Log.d(TAG, "onPostExecute()");
			progress.setVisibility(View.INVISIBLE);

			if (accounts == null) {
				return;
			}
			if (accounts.isEmpty()) {
				message.setText("no accounts retrieved");
				return;
			}
			message.setText("finished refresh");

			// Save accounts to database..
			Log.d(TAG, "writing accounts to db");
			DatabaseAdapter dbAdapter = new DatabaseAdapter(Saldo.this);
			try {
				dbAdapter.open();
				for (Account acc : accounts.values()) {
					boolean result = dbAdapter.saveAccount(acc);
					Log.d(TAG, "createAccount result= " + result);
				}
				Log.d(TAG, "db updated");

				mCursor.requery();

			} catch (SQLException e) {
				Log.e(TAG, "SQLException in onPostExecute()", e);
				message.setText("db persist failed");
			} catch (Exception e) {
				Log.e(TAG, "Exception in onPostExecute()", e);
				message.setText(e.getMessage());
			} finally {
				dbAdapter.close();
			}

		}
	}

}

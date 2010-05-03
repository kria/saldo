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

import com.adrup.saldo.bank.BankLogin;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * An activity that displays the settings and a list of {@link BankLogin BankLogins}.
 * 
 * @author Kristian Adrup
 * 
 */
public class SettingsActivity extends ListActivity {
	private static final int ACTIVITY_CREATE = 0;
	private static final int ACTIVITY_EDIT = 1;

	private static final int INSERT_ID = Menu.FIRST;
	private static final int DELETE_ID = Menu.FIRST + 1;

	private DatabaseAdapter mDbAdapter;
	private Spinner mSpinner;
	private SharedPreferences mPrefs;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		mSpinner = (Spinner) findViewById(R.id.update_freq_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.update_interval_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinner.setAdapter(adapter);

		mDbAdapter = new DatabaseAdapter(this);
		mDbAdapter.open();
		fillData();
		registerForContextMenu(getListView());
	}

	@Override
	protected void onPause() {
		super.onPause();
		int interval = mSpinner.getSelectedItemPosition();
		int prevInterval = mPrefs.getInt(Constants.PREF_UPDATE_INTERVAL, 0);
		if (interval != prevInterval) {
			mPrefs.edit().putInt(Constants.PREF_UPDATE_INTERVAL, interval).commit();
			AutoUpdateReceiver.setAlarm(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSpinner.setSelection(mPrefs.getInt(Constants.PREF_UPDATE_INTERVAL, 0));
	}

	private void fillData() {
		Cursor bankLoginsCursor = mDbAdapter.fetchAllBankLoginsCursor();
		startManagingCursor(bankLoginsCursor);

		// Create an array to specify the fields we want to display in the list (only Name)
		String[] from = new String[] { BankLogin.KEY_NAME };

		// and an array of the fields we want to bind those fields to (in this case just text1)
		int[] to = new int[] { R.id.layout_bank_logins_list_name };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter bankLogins = new SimpleCursorAdapter(this, R.layout.bank_logins_list_item,
				bankLoginsCursor, from, to);
		setListAdapter(bankLogins);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, INSERT_ID, 0, R.string.banklogins_menu_new);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case INSERT_ID:
			createBankLogin();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.banklogins_menu_delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case DELETE_ID:
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			mDbAdapter.deleteBankLogin(info.id);
			fillData();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	private void createBankLogin() {
		Intent i = new Intent(this, BankLoginEditActivity.class);
		startActivityForResult(i, ACTIVITY_CREATE);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent i = new Intent(this, BankLoginEditActivity.class);
		i.putExtra(BankLogin.KEY_ID, id);
		startActivityForResult(i, ACTIVITY_EDIT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		fillData();
	}
}

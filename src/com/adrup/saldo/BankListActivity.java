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
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * An activity that displays a list of {@link BankLogin BankLogins} and lets you add/edit/delete.
 * 
 * @author Kristian Adrup
 * 
 */
public class BankListActivity extends ListActivity {
	private static final int ACTIVITY_CREATE = 0;
	private static final int ACTIVITY_EDIT = 1;

	private static final int INSERT_ID = Menu.FIRST;
	private static final int DELETE_ID = Menu.FIRST + 1;

	private DatabaseAdapter mDbAdapter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bank_logins_list);
		
		Button addButton = (Button) findViewById(R.id.layout_banklogins_add_btn);
		addButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				createBankLogin();
			}
		});
		
		mDbAdapter = new DatabaseAdapter(this);
		mDbAdapter.open();
		fillData();
		registerForContextMenu(getListView());
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
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

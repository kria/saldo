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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Defines the basic CRUD operations for Saldo. Exposes both Cursors  and rudimentary ORM.
 * 
 * Code is derived from the android Notepad example.
 * 
 * @author Kristian Adrup
 *
 */
public class DatabaseAdapter {
	private static final String TAG = "DatabaseAdapter";
	private static final String DATABASE_NAME = "data";
	private static final int DATABASE_VERSION = 2;

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE_ACCOUNTS = "create table " + Account.DATABASE_TABLE + "("
			+ Account.KEY_ID + " integer primary key autoincrement, " + Account.KEY_REMOTE_ID + " integer not null, "
			+ Account.KEY_BANK_LOGIN_ID + " integer not null, " + Account.KEY_ORDINAL + " integer not null, "
			+ Account.KEY_NAME + " text not null, " + Account.KEY_BALANCE + " integer not null);";

	private static final String DATABASE_CREATE_BANK_LOGINS = "create table " + BankLogin.DATABASE_TABLE + "("
			+ BankLogin.KEY_ID + " integer primary key autoincrement, " + BankLogin.KEY_BANK_ID + " integer not null, "
			+ BankLogin.KEY_NAME + " text not null, " + BankLogin.KEY_USERNAME + " text not null, "
			+ BankLogin.KEY_PASSWORD + " text not null);";

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {
		private static final String TAG = "DatabaseHelper";

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "onCreate()");
			db.execSQL(DATABASE_CREATE_ACCOUNTS);
			db.execSQL(DATABASE_CREATE_BANK_LOGINS);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d(TAG, "onUpgrade()");
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
					+ ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + Account.DATABASE_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + BankLogin.DATABASE_TABLE);
			onCreate(db);
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public DatabaseAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Open the Saldo database. If it cannot be opened, try to create a new instance of the database. If it cannot be
	 * created, throw an exception to signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public DatabaseAdapter open() throws SQLException {
		Log.d(TAG, "open()");
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		Log.d(TAG, "close()");
		mDbHelper.close();
	}

	/**
	 * Create a new or update an old account. 
	 * 
	 * @param account
	 * @return true if successful, otherwise false
	 */
	public boolean saveAccount(Account account) {
		Log.d(TAG, "saveAccount()");
		ContentValues args = new ContentValues();

		args.put(Account.KEY_REMOTE_ID, account.getRemoteId());
		args.put(Account.KEY_BANK_LOGIN_ID, account.getBankLoginId());
		args.put(Account.KEY_ORDINAL, account.getOrdinal());
		args.put(Account.KEY_NAME, account.getName());
		args.put(Account.KEY_BALANCE, account.getBalance());

		// first try to update row
		if (mDb.update(Account.DATABASE_TABLE, args, Account.KEY_REMOTE_ID + "=" + account.getRemoteId() + " AND "
				+ Account.KEY_BANK_LOGIN_ID + "=" + account.getBankLoginId(), null) > 0) {
			Log.d(TAG, "saveAccount: account updated");
			return true;
		}

		// if row doesn't exist, try to insert it
		if (mDb.insert(Account.DATABASE_TABLE, null, args) == -1) {
			Log.d(TAG, "saveAccount: insert failed");
			return false;
		} else {
			Log.d(TAG, "saveAccount: account inserted");
			return true;
		}
	}

	/**
	 * Delete the account with the given rowId
	 * 
	 * @param rowId
	 *            id of account to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteAccount(long id) {
		Log.d(TAG, "deleteAccount()");
		return mDb.delete(Account.DATABASE_TABLE, Account.KEY_ID + "=" + id, null) > 0;
	}

	/**
	 * Return a Cursor over the list of all accounts in the database
	 * 
	 * @return Cursor over all accounts
	 */
	public Cursor fetchAllAccountsCursor() {
		Log.d(TAG, "fetchAllAccountsCursor()");
		return mDb.query(Account.DATABASE_TABLE, new String[] { Account.KEY_ID, Account.KEY_REMOTE_ID,
				Account.KEY_BANK_LOGIN_ID, Account.KEY_ORDINAL, Account.KEY_NAME, Account.KEY_BALANCE }, null, null,
				null, null, null);
	}
	
	public Map<AccountHashKey, Account> fetchAllAccountsMap() {
		Log.d(TAG, "fetchAllBankLogins()");
		Cursor cursor = fetchAllAccountsCursor();
		Account account = null;
		Map<AccountHashKey, Account> accounts = new LinkedHashMap<AccountHashKey, Account>();

		while (cursor.moveToNext()) {
			account = populateAccount(cursor);
			accounts.put(new AccountHashKey(account.getRemoteId(), account.getBankLoginId()), account);
		}

		cursor.close();

		return accounts;
	}

	/**
	 * Return a Cursor positioned at the account that matches the given rowId
	 * 
	 * @param rowId
	 *            id of account to retrieve
	 * @return Cursor positioned to matching account, if found
	 * @throws SQLException
	 *             if account could not be found/retrieved
	 */
	public Cursor fetchAccountCursor(int id) throws SQLException {
		Log.d(TAG, "fetchAccount()");
		Cursor mCursor = mDb.query(true, Account.DATABASE_TABLE, new String[] { Account.KEY_ID, Account.KEY_REMOTE_ID,
				Account.KEY_BANK_LOGIN_ID, Account.KEY_ORDINAL, Account.KEY_NAME, Account.KEY_BALANCE }, Account.KEY_ID
				+ "=" + id, null, null, null, null, null);
		return mCursor;
	}

	public Account fetchAccount(int id) {
		Log.d(TAG, "getAccount()");
		Cursor cursor = fetchAccountCursor(id);
		Account account = null;

		if (cursor.moveToFirst())
			account = populateAccount(cursor);

		cursor.close();

		return account;
	}

	private Account populateAccount(Cursor cursor) {
		Log.d(TAG, "populateAccount()");
		int id = cursor.getInt(cursor.getColumnIndexOrThrow(Account.KEY_ID));
		int remoteId = cursor.getInt(cursor.getColumnIndexOrThrow(Account.KEY_REMOTE_ID));
		int bankId = cursor.getInt(cursor.getColumnIndexOrThrow(Account.KEY_BANK_LOGIN_ID));
		int ordinal = cursor.getInt(cursor.getColumnIndexOrThrow(Account.KEY_ORDINAL));
		String name = cursor.getString(cursor.getColumnIndexOrThrow(Account.KEY_NAME));
		long balance = cursor.getLong(cursor.getColumnIndexOrThrow(Account.KEY_BALANCE));

		Account account = new Account(id, remoteId, bankId, ordinal, name, balance);
		Log.d(TAG, "account populated");
		return account;
	}

	public boolean updateBankLogin(long id, BankLogin bankLogin) {
		Log.d(TAG, "updateBankLogin()");
		ContentValues args = new ContentValues();
		args.put(BankLogin.KEY_BANK_ID, bankLogin.getBankId());
		args.put(BankLogin.KEY_NAME, bankLogin.getName());
		args.put(BankLogin.KEY_USERNAME, bankLogin.getUsername());
		args.put(BankLogin.KEY_PASSWORD, bankLogin.getPassword());

		return mDb.update(BankLogin.DATABASE_TABLE, args, BankLogin.KEY_ID + "=" + id, null) > 0;
	}

	public int saveBankLogin(BankLogin bankLogin) {
		Log.d(TAG, "saveBankLogin()");
		ContentValues args = new ContentValues();
		args.put(BankLogin.KEY_BANK_ID, bankLogin.getBankId());
		args.put(BankLogin.KEY_NAME, bankLogin.getName());
		args.put(BankLogin.KEY_USERNAME, bankLogin.getUsername());
		args.put(BankLogin.KEY_PASSWORD, bankLogin.getPassword());

		int ret = -1;
		if (bankLogin.getId() > 0) {
			ret = mDb.update(BankLogin.DATABASE_TABLE, args, BankLogin.KEY_ID + "=" + bankLogin.getId(), null);
			Log.d(TAG, ret > 0 ? "banklogin updated: " + bankLogin.getId() : "banklogin update failed");
		} else {
			ret = (int) mDb.insert(BankLogin.DATABASE_TABLE, null, args);
			Log.d(TAG, ret > 0 ? "banklogin inserted:" + ret : "banklogin insert failed");
		}
		return ret;
	}

	public boolean deleteBankLogin(long id) {
		Log.d(TAG, "deleteBankLogin()");
		// first delete associated accounts, the delete bank login
		mDb.delete(Account.DATABASE_TABLE, Account.KEY_BANK_LOGIN_ID + "=" + id, null);
		return mDb.delete(BankLogin.DATABASE_TABLE, BankLogin.KEY_ID + "=" + id, null) > 0;
	}

	public Cursor fetchAllBankLoginsCursor() {
		Log.d(TAG, "fetchAllBankLoginsCursor()");
		return mDb.query(BankLogin.DATABASE_TABLE, new String[] { BankLogin.KEY_ID, BankLogin.KEY_BANK_ID,
				BankLogin.KEY_NAME, BankLogin.KEY_USERNAME, BankLogin.KEY_PASSWORD }, null, null, null, null, null);
	}

	public List<BankLogin> fetchAllBankLogins() {
		Log.d(TAG, "fetchAllBankLogins()");
		Cursor cursor = fetchAllBankLoginsCursor();
		BankLogin bankLogin = null;
		ArrayList<BankLogin> bankLogins = new ArrayList<BankLogin>();

		while (cursor.moveToNext()) {
			bankLogin = populateBankLogin(cursor);
			bankLogins.add(bankLogin);
		}

		cursor.close();

		return bankLogins;
	}

	public Cursor fetchBankLoginCursor(int id) throws SQLException {
		Log.d(TAG, "fetchBankLoginCursor()");
		Cursor mCursor = mDb.query(true, BankLogin.DATABASE_TABLE, new String[] { BankLogin.KEY_ID,
				BankLogin.KEY_BANK_ID, BankLogin.KEY_NAME, BankLogin.KEY_USERNAME, BankLogin.KEY_PASSWORD },
				BankLogin.KEY_ID + "=" + id, null, null, null, null, null);
		return mCursor;
	}

	public BankLogin fetchBankLogin(int id) {
		Log.d(TAG, "getBankLogin()");
		Cursor cursor = fetchBankLoginCursor(id);
		BankLogin bankLogin = null;

		if (cursor.moveToFirst())
			bankLogin = populateBankLogin(cursor);

		cursor.close();

		return bankLogin;
	}

	private BankLogin populateBankLogin(Cursor cursor) {
		Log.d(TAG, "populateBankLogin()");
		int id = cursor.getInt(cursor.getColumnIndexOrThrow(BankLogin.KEY_ID));
		int bankId = cursor.getInt(cursor.getColumnIndexOrThrow(BankLogin.KEY_BANK_ID));
		String name = cursor.getString(cursor.getColumnIndexOrThrow(BankLogin.KEY_NAME));
		String username = cursor.getString(cursor.getColumnIndexOrThrow(BankLogin.KEY_USERNAME));
		String password = cursor.getString(cursor.getColumnIndexOrThrow(BankLogin.KEY_PASSWORD));

		BankLogin bankLogin = new BankLogin(id, bankId, name, username, password);
		Log.d(TAG, "bankLogin populated");
		return bankLogin;
	}

	/**
	 * Update the note using the details provided. The note to be updated is specified using the rowId, and it is
	 * altered to use the title and body values passed in
	 * 
	 * @param rowId
	 *            id of note to update
	 * @param title
	 *            value to set note title to
	 * @param body
	 *            value to set note body to
	 * @return true if the note was successfully updated, false otherwise
	 */
	public boolean updateAccount(long id, Account account) {
		Log.d(TAG, "updateAccount()");
		ContentValues args = new ContentValues();
		args.put(Account.KEY_ID, account.getId());
		args.put(Account.KEY_REMOTE_ID, account.getRemoteId());
		args.put(Account.KEY_BANK_LOGIN_ID, account.getBankLoginId());
		args.put(Account.KEY_ORDINAL, account.getOrdinal());
		args.put(Account.KEY_NAME, account.getName());
		args.put(Account.KEY_BALANCE, account.getBalance());

		return mDb.update(Account.DATABASE_TABLE, args, Account.KEY_ID + "=" + id, null) > 0;
	}
}

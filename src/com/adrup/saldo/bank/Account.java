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

package com.adrup.saldo.bank;

/**
 * Holds information about a bank account and maps to a database row. Used both when retrieving data from online banks
 * with {@link com.adrup.saldo.bank.BankManager BankManager} and when retrieving/saving data with
 * {@link com.adrup.saldo.DatabaseAdapter DatabaseAdapter}.
 * 
 * @author Kristian Adrup
 * 
 */
public class Account implements RemoteAccount {

	public static final String DATABASE_TABLE = "accounts";
	public static final String KEY_ID = "_id";
	public static final String KEY_REMOTE_ID = "remote_id";
	public static final String KEY_BANK_LOGIN_ID = "bank_login_id";
	public static final String KEY_ORDINAL = "ordinal";
	public static final String KEY_NAME = "name";
	public static final String KEY_ALIAS = "alias";
	public static final String KEY_BALANCE = "balance";
	public static final String KEY_FLAGS = "flags";
	// TODO: Add timestamp.

	private int mId;
	private String mRemoteId;
	private int mBankLoginId;
	private int mOrdinal;
	private String mName;
	private String mAlias;
	private long mBalance;
	private int mFlags = AccountFlags.VISIBLE | AccountFlags.NOTIFY;

	/**
	 * Creates account with a unknown id (i.e. not yet persisted).
	 * 
	 * @see #Account(int, int, int, int, String, long)
	 */
	public Account(String remoteId, int bankLoginId, int ordinal, String name, long balance) {
		this.mRemoteId = remoteId;
		this.mBankLoginId = bankLoginId;
		this.mOrdinal = ordinal;
		this.mName = name;
		this.mBalance = balance;
	}

	/**
	 * Creates account with a known id (i.e. has been persisted).
	 * 
	 * @param remoteId
	 *            the id that identifies the account on the bank side
	 * @param bankLoginId
	 *            the id of the {@link com.adrup.saldo.bank.BankLogin BankLogin} used to retrieve the account
	 * @param ordinal
	 *            the ordinal number of the bank account
	 * @param name
	 *            the name of the bank account
	 * @param balance
	 *            the account balance
	 */
	public Account(int id, String remoteId, int bankLoginId, int ordinal, String name, String alias, long balance, int flags) {
		this(remoteId, bankLoginId, ordinal, name, balance);
		this.mId = id;
		this.mAlias = alias;
		this.mFlags = flags;
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		this.mId = id;
	}

	@Override
	public int getBankLoginId() {
		return mBankLoginId;
	}

	public void setBankId(int bankLoginId) {
		this.mBankLoginId = bankLoginId;
	}
	
	@Override
	public String getRemoteId() {
		return mRemoteId;
	}

	public void setRemoteId(String remoteId) {
		this.mRemoteId = remoteId;
	}

	@Override
	public int getOrdinal() {
		return mOrdinal;
	}

	public void setOrdinal(int ordinal) {
		this.mOrdinal = ordinal;
	}

	@Override
	public String getName() {
		return mName;
	}

	public void setName(String name) {
		this.mName = name;
	}
	
	public String getAlias() {
		return mAlias;
	}

	public void setAlias(String alias) {
		this.mAlias = alias;
	}
	
	@Override
	public long getBalance() {
		return mBalance;
	}

	public void setBalance(long balance) {
		this.mBalance = balance;
	}
	
	public int getFlags() {
		return mFlags;
	}

	public void setFlags(int flags) {
		this.mFlags = flags;
	}

	public static final class AccountFlags {
		public static final int NONE = 0;
		public static final int VISIBLE = 1;
		public static final int NOTIFY = 2;
	}
}

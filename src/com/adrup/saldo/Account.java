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

/**
 * Holds information about a bank account and maps to a database row. Used both when retrieving data from online banks
 * with {@link com.adrup.saldo.bank.BankManager BankManager} and when retrieving/saving data with
 * {@link com.adrup.saldo.DatabaseAdapter DatabaseAdapter}.
 * 
 * @author Kristian Adrup
 * 
 */
public class Account {

	public static final String DATABASE_TABLE = "accounts";
	public static final String KEY_ID = "_id";
	public static final String KEY_REMOTE_ID = "remote_id";
	public static final String KEY_BANK_LOGIN_ID = "bank_login_id";
	public static final String KEY_ORDINAL = "ordinal";
	public static final String KEY_NAME = "name";
	public static final String KEY_BALANCE = "balance";
	// TODO: Add timestamp.

	private int mId;
	private int mRemoteId;
	private int mBankLoginId;
	private int mOrdinal;
	private String mName;
	private long mBalance;

	/**
	 * Creates account with a unknown id (i.e. not yet persisted).
	 * 
	 * @see #Account(int, int, int, int, String, long)
	 */
	public Account(int remoteId, int bankLoginId, int ordinal, String name, long balance) {
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
	public Account(int id, int remoteId, int bankLoginId, int ordinal, String name, long balance) {
		this(remoteId, bankLoginId, ordinal, name, balance);
		this.mId = id;
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		this.mId = id;
	}

	public int getBankLoginId() {
		return mBankLoginId;
	}

	public void setBankId(int bankLoginId) {
		this.mBankLoginId = bankLoginId;
	}

	public int getRemoteId() {
		return mRemoteId;
	}

	public void setRemoteId(int remoteId) {
		this.mRemoteId = remoteId;
	}

	public int getOrdinal() {
		return mOrdinal;
	}

	public void setOrdinal(int ordinal) {
		this.mOrdinal = ordinal;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		this.mName = name;
	}

	public long getBalance() {
		return mBalance;
	}

	public void setBalance(long balance) {
		this.mBalance = balance;
	}

}

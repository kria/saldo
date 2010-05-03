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
 * Represents a bank identity and holds credentials necessary to authenticate against the bank. {@code BankLogin} can be
 * saved to and retrieved from database using a {@link com.adrup.saldo.DatabaseAdapter DatabaseAdapter}.
 * 
 * @author Kristian Adrup
 * 
 */
public class BankLogin {
	public static final String DATABASE_TABLE = "bank_logins";
	public static final String KEY_ID = "_id";
	public static final String KEY_BANK_ID = "bank_id";
	public static final String KEY_NAME = "name";
	public static final String KEY_USERNAME = "username";
	public static final String KEY_PASSWORD = "password";

	private int mId;
	private int mBankId;
	private String mName;
	private String mUsername;
	private String mPassword;

	
	/**
	 * Creates {@code BankLogin} with a unknown id (i.e. not yet persisted).
	 * 
	 * @see #BankLogin(int, int, String, String, String)
	 */
	public BankLogin(int bankId, String name, String username, String password) {
		this.mBankId = bankId;
		this.mName = name;
		this.mUsername = username;
		this.mPassword = password;
	}

	/**
	 * Creates {@code BankLogin} with a known id (i.e. has been persisted).
	 * 
	 * @param id autoincremented db id
	 * @param bankId identifies the bank (Swedbank, Nordea etc.), constants defined in {@link BankManager} 
	 * @param name a user given name
	 * @param username
	 * @param password
	 */
	public BankLogin(int id, int bankId, String name, String username, String password) {
		this(bankId, name, username, password);
		this.mId = id;
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		this.mId = id;
	}

	public int getBankId() {
		return mBankId;
	}

	public void setBankId(int bankId) {
		this.mBankId = bankId;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		this.mName = name;
	}

	public String getUsername() {
		return mUsername;
	}

	public void setUsername(String username) {
		this.mUsername = username;
	}

	public String getPassword() {
		return mPassword;
	}

	public void setPassword(String password) {
		this.mPassword = password;
	}

}

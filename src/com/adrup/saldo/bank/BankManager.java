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

import com.adrup.saldo.Account;
import com.adrup.saldo.AccountHashKey;

import java.util.Map;

/**
 * An interface that exposes the common functions of the different banks. This interface needs to be implemented to add
 * new banks to the application.
 * 
 * @author Kristian Adrup
 * 
 */
public interface BankManager {
	public final static int SWEDBANK = 1;
	public final static int NORDEA = 2;

	public String getName();

	public Account getAccount(int id) throws BankException;

	public Map<AccountHashKey, Account> getAccounts() throws BankException;

	public Map<AccountHashKey, Account> getAccounts(Map<AccountHashKey, Account> accounts) throws BankException;
}

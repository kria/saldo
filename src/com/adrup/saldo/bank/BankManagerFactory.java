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

import com.adrup.saldo.bank.ica.IcaManager;
import com.adrup.saldo.bank.lf.LfBankManager;
import com.adrup.saldo.bank.nordea.NordeaManager;
import com.adrup.saldo.bank.swedbank.SwedbankManager;

import android.content.Context;

/**
 * A {@link BankManager} factory.
 * 
 * @author Kristian Adrup
 *
 */
public class BankManagerFactory {

	
	/**
	 * Creates the appropriate {@link BankManager} depending on the value of {@link BankLogin#getBankId() bankLogin.getBankId()}.
	 * 
	 * @param bankLogin 
	 * @return a {@link BankManager}
	 * @throws BankException
	 */
	public static BankManager createBankManager(Context context, BankLogin bankLogin) throws BankException {
		switch (bankLogin.getBankId()) {

		case BankManager.SWEDBANK:
			return new SwedbankManager(bankLogin);
		case BankManager.NORDEA:
			return new NordeaManager(bankLogin);
		case BankManager.LANSFORSAKRINGAR:
			return new LfBankManager(bankLogin, context);
		case BankManager.ICA:
			return new IcaManager(bankLogin, context);
		default:
			throw new BankException("Illegal Bank type.");
		}
	}
}

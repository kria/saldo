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

import android.database.Cursor;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Used to format specific fields within a accounts listing row.
 * 
 * @author Kristian Adrup
 * 
 */
public class AccountsViewBinder implements SimpleCursorAdapter.ViewBinder {
	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		int balanceIndex = cursor.getColumnIndex(Account.KEY_BALANCE);
		if (columnIndex == balanceIndex) {
			TextView balance = (TextView) view;
			balance.setText(Util.toCurrencyString(cursor.getLong(balanceIndex)));

			return true;
		}
		return false;
	}
}
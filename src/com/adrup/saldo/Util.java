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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Utility class for Saldo specific things. For other utilities see {@code com.adrup.util}.
 * 
 * @author Kristian Adrup
 *
 */
public class Util {
	
	public static String toCurrencyString(long amount) {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setGroupingSeparator(' ');
		DecimalFormat f = new DecimalFormat("#,### kr", symbols);
		return f.format(amount);
	}
	
}

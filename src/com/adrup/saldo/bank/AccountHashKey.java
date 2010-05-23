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

import com.adrup.util.HashCodeUtil;

/**
 * A composite hash key used when storing {@link Account} in a {@link java.util.Map Map}.
 * 
 * @author Kristian Adrup
 * 
 */
public final class AccountHashKey {
	private String mRemoteId;
	private int mBankLoginId;
	private int mHashCode;

	public AccountHashKey(String remoteId, int bankLoginId) {
		this.mRemoteId = remoteId;
		this.mBankLoginId = bankLoginId;
	}

	public String getRemoteId() {
		return mRemoteId;
	}

	public int getBankLoginId() {
		return mBankLoginId;
	}

	@Override
	public boolean equals(Object that) {
		if (this == that)
			return true;
		if (!(that instanceof AccountHashKey))
			return false;
		AccountHashKey thatAccountKey = (AccountHashKey) that;
		return this.mRemoteId.equals(thatAccountKey.mRemoteId) && this.mBankLoginId == thatAccountKey.mBankLoginId;
	}

	@Override
	public int hashCode() {
		if (mHashCode == 0) {
			int result = HashCodeUtil.SEED;
			result = HashCodeUtil.hash(result, mRemoteId);
			result = HashCodeUtil.hash(result, mBankLoginId);
			mHashCode = result;
		}
		return mHashCode;
	}

}

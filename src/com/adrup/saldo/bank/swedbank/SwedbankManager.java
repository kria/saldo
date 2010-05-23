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

package com.adrup.saldo.bank.swedbank;

import com.adrup.http.HttpException;
import com.adrup.http.HttpHelper;
import com.adrup.saldo.SaldoHttpClient;
import com.adrup.saldo.bank.Account;
import com.adrup.saldo.bank.AccountHashKey;
import com.adrup.saldo.bank.AuthenticationException;
import com.adrup.saldo.bank.BankException;
import com.adrup.saldo.bank.BankLogin;
import com.adrup.saldo.bank.BankManager;
import com.adrup.saldo.bank.RemoteAccount;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.text.Html;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A Swedbank implementation of {@link BankManager}.
 * 
 * @author Kristian Adrup
 *
 */
public class SwedbankManager implements BankManager {
	private static final String TAG = "SwedbankManager";
	private static final String NAME = "Swedbank";

	private static final String LOGIN_URL = "https://mobilbank.swedbank.se/banking/swedbank/login.html";
	private static final String ACCOUNTS_URL = "https://mobilbank.swedbank.se/banking/swedbank/accounts.html";
	private static final String TOKEN_PARAM = "_csrf_token";
	private static final String USER_PARAM = "xyz";
	private static final String PASS_PARAM = "zyx";

	private static final String TOKEN_REGEX = "_csrf_token\"[^>]+value=\"([^\"]+)\"";
	private static final String ACCOUNTS_REGEX = 
		"<a accesskey=\"\\d+\" href=\"/banking/swedbank/(account|loan)\\.html\\?id=(\\d+)\">\\s*" +
		"<span class=\"icon\">&nbsp;(\\d+)&nbsp;</span>([^<]+)<br/><span class=\"secondary\">([\\d -]+)</span>";

	private BankLogin mBankLogin;
	private Context mContext;

	public SwedbankManager(BankLogin bankLogin, Context context) {
		this.mBankLogin = bankLogin;
		this.mContext = context;
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public Map<AccountHashKey, RemoteAccount> getAccounts() throws BankException {
		return getAccounts(new LinkedHashMap<AccountHashKey, RemoteAccount>());
	}
	
	@Override
	public Map<AccountHashKey, RemoteAccount> getAccounts(Map<AccountHashKey, RemoteAccount> accounts) throws BankException {
		Log.d(TAG, "-> getAccounts()");
		HttpClient httpClient = new SaldoHttpClient(mContext);

		try {
			// First get token
			Log.d(TAG, "getting token...");
			String res = HttpHelper.get(httpClient, LOGIN_URL);
			Pattern pattern = Pattern.compile(TOKEN_REGEX);
			Matcher matcher = pattern.matcher(res);
			if (!matcher.find()) {
				Log.e(TAG, "No token match.");
				throw new SwedbankException("No token match.");
			}
			String token = matcher.group(1);
			Log.d(TAG, "token= " + token);

			// Then do login
			List<NameValuePair> parameters = new ArrayList<NameValuePair>(3);
			parameters.add(new BasicNameValuePair(TOKEN_PARAM, token));
			parameters.add(new BasicNameValuePair(USER_PARAM, mBankLogin.getUsername()));
			parameters.add(new BasicNameValuePair(PASS_PARAM, mBankLogin.getPassword()));

			Log.d(TAG, "logging in...");
			res = HttpHelper.post(httpClient, LOGIN_URL, parameters);
			
			//TODO: use regexp to grab swedbank err text
			if (res.contains("section error")) {
				//login failed.. bail
				throw new AuthenticationException("auth fail");
			}

			// Now we should be logged in with a cookie set, let's get accounts info
			Log.d(TAG, "getting account info...");
			res = HttpHelper.get(httpClient, ACCOUNTS_URL);
			//Log.d(TAG, "accounts html dump:");
			//Log.d(TAG, res);
			pattern = Pattern.compile(ACCOUNTS_REGEX);
			matcher = pattern.matcher(res);

			while (matcher.find()) {
				int groupCount = matcher.groupCount();
				if (groupCount < 5) {
					throw new SwedbankException("Pattern match issue: groupCount < 5");
				}
				for (int i = 1; i <= groupCount; i++) {
					Log.d(TAG, i + ":" + matcher.group(i));
				}
				String accountType = matcher.group(1);
				String remoteId = matcher.group(2);
				int ordinal = Integer.parseInt(matcher.group(3));
				String name = Html.fromHtml(matcher.group(4)).toString();
				long balance = Long.parseLong(matcher.group(5).replaceAll(" ", ""));
				accounts.put(new AccountHashKey(remoteId, mBankLogin.getId()), new Account(remoteId, mBankLogin.getId(), ordinal, name, balance));
			}

		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new SwedbankException(e.getMessage(), e);

		} catch (HttpException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new SwedbankException(e.getMessage(), e);
		}

		Log.d(TAG, "<- getAccounts()");

		return accounts;

	}
}

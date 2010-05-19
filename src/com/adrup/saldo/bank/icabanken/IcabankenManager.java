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

package com.adrup.saldo.bank.icabanken;

import com.adrup.http.EasySSLSocketFactory;
import com.adrup.http.HttpException;
import com.adrup.http.HttpHelper;
import com.adrup.saldo.Account;
import com.adrup.saldo.AccountHashKey;
import com.adrup.saldo.SaldoHttpClient;
import com.adrup.saldo.bank.AuthenticationException;
import com.adrup.saldo.bank.BankException;
import com.adrup.saldo.bank.BankLogin;
import com.adrup.saldo.bank.BankManager;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

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
 * A ICA implementation of {@link BankManager}.
 * 
 * @author Kristian Adrup
 * 
 */
public class IcabankenManager implements BankManager {
	private static final String TAG = "IcabankenManager";
	private static final String NAME = "ICA Banken";
	private static final String LOGIN_URL = "https://mobil.icabanken.se/login/login.aspx";
	private static final String ACCOUNT_URL = "https://mobil.icabanken.se/account/overview.aspx";
	private static final String USER_PARAM = "pnr_phone";
	private static final String PASS_PARAM = "pwd_phone";
	private static final String BUTTON_PARAM = "btnLogin";
	
	private static final String VIEWSTATE_PARAM = "__VIEWSTATE";
	private static final String EVENTVALIDATION_PARAM = "__EVENTVALIDATION";

	private static final String EVENTVALIDATION_REGEX = "__EVENTVALIDATION\"\\s+value=\"([^\"]+)\"";
	private static final String VIEWSTATE_REGEX = "__VIEWSTATE\"\\s+value=\"([^\"]+)\"";
	//private static final String ACCOUNTS_REGEX = "account\\.aspx\\?id=([^\"]+).+?>([^<]+)</a.+?Saldo([0-9 .,-]+)";

	//Inget disponibelt belopp
	private static final String ACCOUNTS_REGEX = "account\\.aspx\\?id=([^\"]+).+?>([^<]+)</a[^D]*Saldo([0-9 .,-]+)";

	//Disponibelt belopp
	///<div><a href="account.aspx?id=0000800577" accesskey="1">Mat pengar</a><br>- Disponibelt 133,90 kr<br>- Saldo 171,50 kr</div>
	private static final String ACCOUNTSDISP_REGEX =  	"account\\.aspx\\?id=([^\"]+).+?>([^<]+)</a.+?Disponibelt([0-9 .,-]+)";

	private BankLogin mBankLogin;
	private Context mContext;

	public IcabankenManager(BankLogin bankLogin, Context context) {
		this.mBankLogin = bankLogin;
		this.mContext = context;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public Account getAccount(int id) throws BankException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<AccountHashKey, Account> getAccounts() throws BankException {
		Map<AccountHashKey, Account> accounts = new LinkedHashMap<AccountHashKey, Account>();
		return getAccounts(accounts);
	}

	@Override
	public Map<AccountHashKey, Account> getAccounts(Map<AccountHashKey, Account> accounts) throws BankException {
		Log.d(TAG, "getAccounts()");

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		// Android doesn't like ICA's cert, so we need a forgiving TrustManager
		schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));
		HttpParams params = new BasicHttpParams();
		HttpClient httpClient = new SaldoHttpClient(mContext, new ThreadSafeClientConnManager(params, schemeRegistry), null);

		try {
			// get login page

			Log.d(TAG, "getting login page");

			String res = HttpHelper.get(httpClient, LOGIN_URL);

			Matcher matcher = Pattern.compile(VIEWSTATE_REGEX).matcher(res);
			if (!matcher.find()) {
				Log.e(TAG, "No viewstate match.");
				Log.d(TAG, res);
				throw new IcabankenException("No viewState match.");
				
			}
			String viewState = matcher.group(1);
			Log.d(TAG, "viewState= " + viewState);
			
			matcher = Pattern.compile(EVENTVALIDATION_REGEX).matcher(res);
			if (!matcher.find()) {
							Log.e(TAG, "No evenValidation match.");
				Log.d(TAG, res);
				throw new IcabankenException("No evenValidation match.");
			}
			String evenValidation = matcher.group(1);
			Log.d(TAG, "evenValidation= " + evenValidation);

			// do login post, should redirect us to the accounts page
			List<NameValuePair> parameters = new ArrayList<NameValuePair>(3);

			parameters.add(new BasicNameValuePair(VIEWSTATE_PARAM, viewState));
			parameters.add(new BasicNameValuePair(EVENTVALIDATION_PARAM, evenValidation));
			parameters.add(new BasicNameValuePair(USER_PARAM, mBankLogin.getUsername()));
			parameters.add(new BasicNameValuePair(PASS_PARAM, mBankLogin.getPassword()));
			parameters.add(new BasicNameValuePair(BUTTON_PARAM, "Logga in"));

			res = HttpHelper.post(httpClient, LOGIN_URL, parameters);

			if (res.contains("class=\"error\"")) {
				
				Log.d(TAG, "auth fail");
				throw new AuthenticationException("auth fail");
			}

			// get and extract account info
			res = HttpHelper.get(httpClient, ACCOUNT_URL);
			matcher = Pattern.compile(ACCOUNTS_REGEX).matcher(res);

			int remoteId = 1;
			int count = 0;
			while (matcher.find()) {
				count++;
				
				int ordinal = remoteId;
				String name = Html.fromHtml(matcher.group(2)).toString().trim();
				long balance = Long.parseLong(matcher.group(3).replaceAll("\\,|\\.| ", "")) / 100;
				accounts.put(new AccountHashKey(remoteId, mBankLogin.getId()), new Account(remoteId,
						mBankLogin.getId(), ordinal, name, balance));
				remoteId++;
			}
			
			matcher = Pattern.compile(ACCOUNTSDISP_REGEX).matcher(res);

			while (matcher.find()) {
				count++;
				
				int ordinal = remoteId;
				String name = Html.fromHtml(matcher.group(2)).toString().trim();
				long balance = Long.parseLong(matcher.group(3).replaceAll("\\,|\\.| ", "")) / 100;
				accounts.put(new AccountHashKey(remoteId, mBankLogin.getId()), new Account(remoteId,
						mBankLogin.getId(), ordinal, name, balance));
				remoteId++;
			}
			
			if (count == 0) {
				Log.d(TAG, "no accounts added");
				Log.d(TAG, res);
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new IcabankenException(e.getMessage(), e);
		} catch (HttpException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new IcabankenException(e.getMessage(), e);
		} finally {
			httpClient.getConnectionManager().shutdown();
		}

		return accounts;
	}
}

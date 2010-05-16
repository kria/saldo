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

package com.adrup.saldo.bank.lf;

import com.adrup.http.HttpException;
import com.adrup.http.HttpHelper;
import com.adrup.saldo.Account;
import com.adrup.saldo.AccountHashKey;
import com.adrup.saldo.bank.AuthenticationException;
import com.adrup.saldo.bank.BankException;
import com.adrup.saldo.bank.BankLogin;
import com.adrup.saldo.bank.BankManager;

import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

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
 * A Länsförsäkringar Bank implementation of {@link BankManager}.
 * 
 * @author Kristian Adrup
 * 
 */
public class LfBankManager implements BankManager {
	private static final String TAG = "LfBankManager";
	private static final String NAME = "Länsforsakringar Bank";

	private static final String LOGIN_URL = "https://secure246.lansforsakringar.se/lfportal/login/privat";
	private static final String USER_PARAM = "inputPersonalNumber";
	private static final String PASS_PARAM = "inputPinCode";
	private static final String VIEWSTATE_PARAM = "__VIEWSTATE";
	private static final String EVENTVALIDATION_PARAM = "__EVENTVALIDATION";

	private static final String VIEWSTATE_REGEX = "id=\"__VIEWSTATE\"\\s+value=\"([^\"]+)\"";
	private static final String EVENTVALIDATION_REGEX = "id=\"__EVENTVALIDATION\"\\s+value=\"([^\"]+)\"";
	private static final String ACCOUNTS_URL_REGEX = "<li class=\"bank\">\\s*<a href=\"([^\"]+)\"";
	private static final String TOKEN_REGEX = "var\\s+token\\s*=\\s*'([^']+)'";
	private static final String ACCOUNTS_REGEX = "<a.+>([^<]+)</span></a>.+>([\\d -,]+)</span></td>";

	private BankLogin bankLogin;
	private Context context;

	public LfBankManager(BankLogin bankLogin, Context context) {
		this.bankLogin = bankLogin;
		this.context = context;
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
		HttpClient httpClient = new DefaultHttpClient();

		try {
			// get login page
			Log.d(TAG, "getting login page");
			HttpContext httpContext = new BasicHttpContext();
			String res = HttpHelper.get(httpClient, LOGIN_URL, httpContext);
			HttpUriRequest currentReq = (HttpUriRequest) httpContext.getAttribute(ExecutionContext.HTTP_REQUEST);
			HttpHost currentHost = (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
			String action = currentHost.toURI() + currentReq.getURI();
			Log.e(TAG, "action=" + action);

			Matcher matcher = Pattern.compile(VIEWSTATE_REGEX).matcher(res);
			if (!matcher.find()) {
				Log.e(TAG, "No viewstate match.");
				Log.d(TAG, res);
				throw new LfBankException("No viewState match.");
			}
			String viewState = matcher.group(1);
			Log.d(TAG, "viewState= " + viewState);

			matcher = Pattern.compile(EVENTVALIDATION_REGEX).matcher(res);
			if (!matcher.find()) {
				Log.e(TAG, "No eventvalidation match.");
				Log.d(TAG, res);
				throw new LfBankException("No eventValidation match.");
			}
			String eventValidation = matcher.group(1);
			Log.d(TAG, "eventValidation= " + eventValidation);

			// do login post
			List<NameValuePair> parameters = new ArrayList<NameValuePair>(3);

			parameters.add(new BasicNameValuePair("__LASTFOCUS", ""));
			parameters.add(new BasicNameValuePair("__EVENTTARGET", ""));
			parameters.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
			parameters.add(new BasicNameValuePair(VIEWSTATE_PARAM, viewState));
			parameters.add(new BasicNameValuePair("selMechanism", "PIN-kod"));
			parameters.add(new BasicNameValuePair(USER_PARAM, bankLogin.getUsername()));
			parameters.add(new BasicNameValuePair(PASS_PARAM, bankLogin.getPassword()));
			parameters.add(new BasicNameValuePair("btnLogIn.x", "39"));
			parameters.add(new BasicNameValuePair("btnLogIn.y", "11"));
			parameters.add(new BasicNameValuePair(EVENTVALIDATION_PARAM, eventValidation));

			Log.d(TAG, "logging in...");
			res = HttpHelper.post(httpClient, action, parameters);

			if (res.contains("Felaktig inloggning")) {
				Log.d(TAG, "auth fail");
				throw new AuthenticationException("auth fail");
			}

			Log.d(TAG, "getting accountsUrl");

			// token
			matcher = Pattern.compile(TOKEN_REGEX).matcher(res);
			if (!matcher.find()) {
				Log.e(TAG, "No token match.");
				Log.d(TAG, res);
				throw new LfBankException("No token match.");
			}
			String token = matcher.group(1);
			Log.d(TAG, "token= " + token);

			// accountsUrl
			matcher = Pattern.compile(ACCOUNTS_URL_REGEX).matcher(res);

			if (!matcher.find()) {
				Log.e(TAG, "No accountsUrl match.");
				Log.d(TAG, res);
				throw new LfBankException("No accountsUrl match.");
			}
			String accountsUrl = Html.fromHtml(matcher.group(1)).toString();

			accountsUrl += "&_token=" + token;
			Log.d(TAG, "tokenized accountsUrl= " + accountsUrl);

			// get accounts page
			Log.d(TAG, "fetching accounts");
			res = HttpHelper.get(httpClient, accountsUrl);

			matcher = Pattern.compile(ACCOUNTS_REGEX).matcher(res);

			int remoteId = 1;
			int count = 0;
			while (matcher.find()) {
				count++;
				int groupCount = matcher.groupCount();
				for (int i = 1; i <= groupCount; i++) {
					Log.d(TAG, i + ":" + matcher.group(i));
				}
				if (groupCount < 2) {
					throw new BankException("Pattern match issue: groupCount < 2");
				}

				int ordinal = remoteId;
				String name = Html.fromHtml(matcher.group(1)).toString();
				long balance = Long.parseLong(matcher.group(2).replaceAll("\\,|\\.| ", "")) / 100;
				accounts.put(new AccountHashKey(remoteId, bankLogin.getId()), new Account(remoteId, bankLogin.getId(),
						ordinal, name, balance));
				remoteId++;
			}
			if (count == 0) {
				Log.d(TAG, "no accounts added");
				Log.d(TAG, res);
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new LfBankException(e.getMessage(), e);

		} catch (HttpException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new LfBankException(e.getMessage(), e);
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		

		return accounts;
	}
}

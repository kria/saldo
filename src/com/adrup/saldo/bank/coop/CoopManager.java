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
package com.adrup.saldo.bank.coop;

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
 * A coop implementation of {@link BankManager}.
 * 
 * @author Kristian Adrup
 * 
 */
public class CoopManager implements BankManager {
	private static final String TAG = "CoopManager";
	private static final String NAME = "coop";
	private static final String LOGIN_URL = "https://www.coop.se/Mina-sidor/Oversikt/";
	private static final String ACCOUNT_URL = "https://www.coop.se/Mina-sidor/Oversikt/?t=MedMeraAccount";
	private static final String CREDIT_URL = "https://www.coop.se/Mina-sidor/Oversikt/?t=MedMeraVisa";
	private static final String USER_PARAM = "ctl00$ContentPlaceHolderMainPageContainer$ContentPlaceHolderPreContent$RegisterMediumUserForm$TextBoxUserName";
	private static final String PASS_PARAM = "ctl00$ContentPlaceHolderMainPageContainer$ContentPlaceHolderPreContent$RegisterMediumUserForm$TextBoxPassword";
	private static final String BUTTON_PARAM = "ctl00$ContentPlaceHolderMainPageContainer$ContentPlaceHolderPreContent$RegisterMediumUserForm$ButtonLogin";
	
	private static final String VIEWSTATE_PARAM = "__VIEWSTATE";
	
	/*
	 * <div class="column6">
                <table class="randig">
                    <tbody><tr class="emphasize">
                        <td class="emphasize">Saldo:</td>

                        <td>28,12 kr</td>
                    </tr>
                    <tr>
                        <td class="emphasize">Disponibelt belopp:</td>
                        <td class="emphasize">28,12 kr</td>
                    </tr>
                    <tr class="emphasize">

                        <td class="emphasize">Ej bokförda köp:</td>
                        <td>0,00 kr</td>
                    </tr>
                    <tr>
                        <td class="emphasize">Beviljad kredit:</td>
                        <td>0,00 kr</td>
                    </tr>
                    
                    <tbody><tr class="emphasize">
                        <td class="emphasize">Saldo:</td>
                        <td>-16.001,74 kr</td>
                    </tr>
                    <tr>
                        <td class="emphasize">Disponibelt belopp:</td>
                        <td class="emphasize">3.852,90 kr</td>

                    </tr>
                    <tr class="emphasize">
                        <td class="emphasize">Ej bokförda köp:</td>
                        <td>-145,36 kr</td>
                    </tr>
                    <tr>
                        <td class="emphasize">Beviljad kredit:</td>

                        <td>20.000,00 kr</td>
                    </tr>
                    
                    
                    <tr class="emphasize">
                        <td class="emphasize">Fakturerat belopp:</td>
                        <td>5.313,34 kr</td>
                    </tr>
                    <tr>

                        <td class="emphasize">Lägsta belopp att betala:</td>
                        <td>200,00 kr</td>
                    </tr>
                    <tr class="emphasize">
                        <td class="emphasize">Oss tillhanda senast:</td>
                        <td>2010-05-31</td>
                    </tr>

                    
            </tbody>

	 */
	 
	private static final String VIEWSTATE_REGEX = "__VIEWSTATE\" value=\"([^\"]+)\"";
	private static final String ACCOUNTS_REGEX = "Disponibelt belopp[^\n]*[^>]*>([0-9., ]+)";
	
	private BankLogin mBankLogin;
	private Context mContext;

	public CoopManager(BankLogin bankLogin, Context context) {
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
				throw new CoopException("No viewState match.");
				
			}
			String viewState = matcher.group(1);
			Log.d(TAG, "viewState= " + viewState);
			
			// do login post, should redirect us to the accounts page
			List<NameValuePair> parameters = new ArrayList<NameValuePair>(3);

			parameters.add(new BasicNameValuePair(VIEWSTATE_PARAM, viewState));
			parameters.add(new BasicNameValuePair(USER_PARAM, mBankLogin.getUsername()));
			parameters.add(new BasicNameValuePair(PASS_PARAM, mBankLogin.getPassword()));
			parameters.add(new BasicNameValuePair(BUTTON_PARAM, "Logga in"));

			res = HttpHelper.post(httpClient, LOGIN_URL, parameters);

			if (res.contains("Felmeddelande")) {
				
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
				String name = "MedMera Konto";//Html.fromHtml(matcher.group(1)).toString().trim();
				long balance = Long.parseLong(matcher.group(1).replaceAll("\\,|\\.| ", "")) / 100;
				accounts.put(new AccountHashKey(remoteId, mBankLogin.getId()), new Account(remoteId,
						mBankLogin.getId(), ordinal, name, balance));
				remoteId++;
			}
			
			res = HttpHelper.get(httpClient, CREDIT_URL);
			matcher = Pattern.compile(ACCOUNTS_REGEX).matcher(res);
			while (matcher.find()) {
				count++;
				int ordinal = remoteId;
				String name = "MedMera Visa";//Html.fromHtml(matcher.group(1)).toString().trim();
				long balance = Long.parseLong(matcher.group(1).replaceAll("\\,|\\.| ", "")) / 100;
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
			throw new CoopException(e.getMessage(), e);
		} catch (HttpException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new CoopException(e.getMessage(), e);
		} finally {
			httpClient.getConnectionManager().shutdown();
		}

		return accounts;
	}
}
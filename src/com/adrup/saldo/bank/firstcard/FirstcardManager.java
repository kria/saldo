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

package com.adrup.saldo.bank.firstcard;

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
 * A Nordea implementation of {@link BankManager}.
 * 
 * @author Sed
 * @author Kristian Adrup
 *
 */
public class FirstcardManager implements BankManager {
    private static final String TAG = "FirstcardManager";
    private static final String NAME = "FirstCard";
    public final static String KEY_PREFIX = "FC_";

    private static final String LOGIN_URL = "https://www.firstcard.se/valkom.jsp";
    private static final String ACCOUNTS_URL = "https://www.firstcard.se/mkol/index.jsp";
    private static final String USER_PARAM = "pnr";
    private static final String PASS_PARAM = "intpwd";

    private static final String USER_AGENT = "Mozilla/5.0 (Linux; U; Android 1.5; en-se; HTC Hero Build/CUPCAKE) AppleWebKit/528.5+ (KHTML, like Gecko) Version/3.1.2 Mobile Safari/525.20.1";

    /*
     * <tr>
								<td colspan="2" valign="top"><a href="translist.jsp?p=a&amp;cardID=Mgn7h6ABJutrHwsfHWwohvhr1xdn--CS">xxxx xxxx xxxx 7580</a>
								</td>
								<td align="right" valign="top">x xxx,xx</td>
								</tr>
     */
    private static final String ACCOUNTS_REGEX = 
    	"cardID[^>]+>([^<]+)[^>]+>.*p\">([0-9\\p{Zs} .,-]+)";
	
	private BankLogin mBankLogin;
	private Context mContext;

	public FirstcardManager(BankLogin bankLogin, Context context) {
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
            // Do login
            List<NameValuePair> parameters = new ArrayList<NameValuePair>(3);
            //parameters.add(new BasicNameValuePair(TOKEN_PARAM, token));
            parameters.add(new BasicNameValuePair(USER_PARAM, mBankLogin.getUsername()));
            parameters.add(new BasicNameValuePair(PASS_PARAM, mBankLogin.getPassword()));
            parameters.add(new BasicNameValuePair("op", "login"));
            parameters.add(new BasicNameValuePair("searchIndex", ""));

            Log.d(TAG, "logging in...");
            String res = HttpHelper.post(httpClient, LOGIN_URL, parameters);
           
            if (res.contains("Du har angivit en felaktig identitet")) {
                //login failed.. bail
                throw new AuthenticationException("auth fail");
            }

            
            //ACCOUNTS
            Log.d(TAG, "getting account info...");
            res = HttpHelper.get(httpClient, ACCOUNTS_URL);
            //Log.d(TAG, "accounts html dump:");
            //Log.d(TAG, res);
            Pattern pattern = Pattern.compile(ACCOUNTS_REGEX, Pattern.DOTALL);
            Matcher matcher = pattern.matcher(res);

            int ordinal=1;
           while (matcher.find()) {

            	String remoteId = matcher.group(1).replaceAll("[^0-9]", "");
                ordinal = ordinal++;
                String name = "FirstCard";
                long balance = Long.parseLong(matcher.group(2).replaceAll("[^0-9\\-]", ""))/100;
                accounts.put(new AccountHashKey(remoteId, mBankLogin.getId()), new Account(remoteId, mBankLogin.getId(), ordinal, name, balance));
            }
                
                
         

        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new FirstcardException(e.getMessage(), e);

        } catch (HttpException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new FirstcardException(e.getMessage(), e);
        }

        Log.d(TAG, "<- getAccounts()");

        return accounts;

    }
} 
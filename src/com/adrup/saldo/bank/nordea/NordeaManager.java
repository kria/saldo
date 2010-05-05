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

package com.adrup.saldo.bank.nordea;

import com.adrup.http.HTTPException;
import com.adrup.http.HTTPHelper;
import com.adrup.saldo.Account;
import com.adrup.saldo.AccountHashKey;
import com.adrup.saldo.bank.AuthenticationException;
import com.adrup.saldo.bank.BankException;
import com.adrup.saldo.bank.BankLogin;
import com.adrup.saldo.bank.BankManager;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

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
public class NordeaManager implements BankManager {
    private static final String TAG = "NordeaManager";
    private static final String NAME = "Nordea";
    public final static String KEY_PREFIX = "NOR_";

    private static final String LOGIN_URL = "https://gfs.nb.se/bin2/gfskod";
    private static final String ACCOUNTS_URL = "https://gfs.nb.se/bin2/gfskod?OBJECT=KF00T";
    private static final String CREDITS_URL = "https://gfs.nb.se/bin2/gfskod?OBJECT=CC00T";
    private static final String TOKEN_PARAM = "_csrf_token";
    private static final String USER_PARAM = "kundnr";
    private static final String PASS_PARAM = "pinkod";

    private static final String OBJECT_PARAM = "OBJECT";
    private static final String CHECKCODE_PARAM = "CHECKCODE";
    
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; U; Android 1.5; en-se; HTC Hero Build/CUPCAKE) AppleWebKit/528.5+ (KHTML, like Gecko) Version/3.1.2 Mobile Safari/525.20.1";

    private static final String ACCOUNTS_REGEX = 
		"KF00T', '(\\d+)', '([^']+)'[^\\n]*[^>]*>([0-9.,-]+)";
	
    private static final String CREDITS_REGEX = 
		"Kontoutdrag[^>]*>([^<]+)<[^\\n]*[^>]*>([0-9.,-]+)";

    private static final String FOND_REGEX = 
		"sendFund[^>]*>([^<]+)<[^\\n]*[^>]*>([0-9.,-]+)";

 	
	

	private BankLogin bankLogin;
	
	public NordeaManager(BankLogin bankLogin) {
		this.bankLogin = bankLogin;
	}

	@Override
	public String getName() {
		return NAME;
	}
	
    @Override
    public Account getAccount(int id) throws BankException {
    	Map<AccountHashKey, Account> accounts = getAccounts();
        return accounts.get(KEY_PREFIX + String.valueOf(id));
    }
    
    @Override
    public Map<AccountHashKey, Account> getAccounts() throws BankException {
    	Map<AccountHashKey, Account> accounts = new LinkedHashMap<AccountHashKey, Account>();
		return getAccounts(accounts);
    }
    @Override
    public Map<AccountHashKey, Account> getAccounts(Map<AccountHashKey, Account> accounts) throws BankException {
        Log.d(TAG, "-> getAccounts()");
        HttpClient httpClient = new DefaultHttpClient();
        //TODO: grab internal agent
        //httpClient.getParams().setParameter(HttpProtocolParams.USER_AGENT, USER_AGENT);

        try {
            // Do login
            List<NameValuePair> parameters = new ArrayList<NameValuePair>(3);
            //parameters.add(new BasicNameValuePair(TOKEN_PARAM, token));
            parameters.add(new BasicNameValuePair(USER_PARAM, bankLogin.getUsername()));
            parameters.add(new BasicNameValuePair(PASS_PARAM, bankLogin.getPassword()));
            parameters.add(new BasicNameValuePair(OBJECT_PARAM, "TT00"));
            parameters.add(new BasicNameValuePair(CHECKCODE_PARAM, Integer.toString(((int) (System.currentTimeMillis() / 1000L)))+"123"));

            Log.d(TAG, "logging in...");
            String res = HTTPHelper.post(httpClient, LOGIN_URL, parameters);
            
            //TODO: use regexp to grab swedbank err text
            if (res.contains("Tekniskt fel")) {
                //login failed.. bail
                throw new AuthenticationException("auth fail");
            }

            
            //ACCOUNTS
            Log.d(TAG, "getting account info...");
            res = HTTPHelper.get(httpClient, ACCOUNTS_URL);
            //Log.d(TAG, "accounts html dump:");
            //Log.d(TAG, res);
            Pattern pattern = Pattern.compile(ACCOUNTS_REGEX);
            Matcher matcher = pattern.matcher(res);

            int ordinal=1;
            while (matcher.find()) {

            	int remoteId = Integer.parseInt(matcher.group(1));
                ordinal = Integer.parseInt(matcher.group(1));
                String name = Html.fromHtml(matcher.group(2)).toString();
                long balance = Long.parseLong(matcher.group(3).replaceAll("\\,|\\.", ""))/100;
                accounts.put(new AccountHashKey(remoteId, bankLogin.getId()), new Account(remoteId, bankLogin.getId(), ordinal, name, balance));
            }
            
            
            
            //CREDIT CARDS
            Log.d(TAG, "getting account info...");
            String res2 = HTTPHelper.get(httpClient, CREDITS_URL);
            Log.d(TAG, "accounts html dump:");
            Log.d(TAG, res);
            pattern = Pattern.compile(CREDITS_REGEX);
            matcher = pattern.matcher(res2);
            int i = ordinal;
            while (matcher.find()) {

            	ordinal = ++i;
            	int remoteId = ordinal+100; // we need a unique remoteId per bank login
                
                String name = Html.fromHtml(matcher.group(1)).toString();
                long balance = Long.parseLong(matcher.group(2).replaceAll("\\,|\\.", ""))/100;
                accounts.put(new AccountHashKey(remoteId, bankLogin.getId()), new Account(remoteId, bankLogin.getId(), ordinal, name, balance));
            }
            
           //FONDER
            pattern = Pattern.compile(FOND_REGEX);
            matcher = pattern.matcher(res);
            i = ordinal;
            while (matcher.find()) {

            	ordinal = ++i;
            	int remoteId = ordinal+200;
               
                String name = Html.fromHtml(matcher.group(1)).toString();
                long balance = Long.parseLong(matcher.group(2).replaceAll("\\,|\\.", ""))/100;
                accounts.put(new AccountHashKey(remoteId, bankLogin.getId()), new Account(remoteId, bankLogin.getId(), ordinal, name, balance));
            }
            
            
            
            //accounts.put(Integer.valueOf("5"), new Account(5, 5, "test", 2323));

        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new NordeaException(e.getMessage(), e);

        } catch (HTTPException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new NordeaException(e.getMessage(), e);
        }

        Log.d(TAG, "<- getAccounts()");

        return accounts;

    }
}  

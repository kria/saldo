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

package com.adrup.saldo.bank.statoil;

import com.adrup.http.EasySSLSocketFactory;
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
import com.adrup.saldo.bank.ica.IcaException;
import com.adrup.saldo.bank.icabanken.IcabankenException;

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
 * A Statoil implementation of {@link BankManager}.
 * 
 * @author Sed
 * @author Kristian Adrup
 *
 */
public class StatoilManager implements BankManager {
    private static final String TAG = "StatoilManager";
    private static final String NAME = "Statoil Mastercard";
    public final static String KEY_PREFIX = "ST_";

    private static final String LOGIN_URL2 = "https://applications.sebkort.com/nis/external/hidden.jsp";
    private static final String LOGIN_URL3 = "https://applications.sebkort.com/siteminderagent/forms/generic.fcc";
    private static final String ACCOUNT_URL = "https://applications.sebkort.com/nis/stse/getBillingUnits.do";
    private static final String USER_PARAM = "uname";
    private static final String PASS_PARAM = "password";


   
    /*
     * 	
	   	<tr class="Alternative"> 
							    <td><a href="getInvoiceList.do?id=75020ecc6c1f2c26210f9025">STATOIL MASTERCARD                                 ****** ******</a></td> 
							    
								   	
								   	 	<td>&nbsp;</td>   
								   	 	<td>&nbsp;</td>   
								    	<td class="Right">1 
								    	322,23</td>    
								    	
								    		<td class="Right">10 000,00</td>     
     */
    private static final String ACCOUNTS_REGEX = 
    	"getInvoiceList\\.do\\?id=.*;</td>[^\\n]*[^>]*>([^<]+)";
	
	private BankLogin mBankLogin;
	private Context mContext;

	public StatoilManager(BankLogin bankLogin, Context context) {
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
       // HttpClient httpClient = new SaldoHttpClient(mContext);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		// Android doesn't like ICA's cert, so we need a forgiving TrustManager
		schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));
		HttpParams params = new BasicHttpParams();
		HttpClient httpClient = new SaldoHttpClient(mContext, new ThreadSafeClientConnManager(params, schemeRegistry), null);
	
           
		try {
	  			
			//Do login
			List<NameValuePair> parameters = new ArrayList<NameValuePair>(3);
            parameters.add(new BasicNameValuePair("USERNAME", ("0122"+mBankLogin.getUsername()).toUpperCase()));
            parameters.add(new BasicNameValuePair("referer", "login.jsp"));
            String res = HttpHelper.post(httpClient, LOGIN_URL2, parameters);
           
        
			// Do login
			parameters = new ArrayList<NameValuePair>(3);
            parameters.add(new BasicNameValuePair(USER_PARAM, mBankLogin.getUsername()));
            parameters.add(new BasicNameValuePair(PASS_PARAM, mBankLogin.getPassword()));
            parameters.add(new BasicNameValuePair("target", "/nis/stse/main.do"));
            parameters.add(new BasicNameValuePair("prodgroup", "0122"));
            parameters.add(new BasicNameValuePair("USERNAME", ("0122"+mBankLogin.getUsername()).toUpperCase()));
            parameters.add(new BasicNameValuePair("METHOD", "LOGIN"));
            parameters.add(new BasicNameValuePair("CURRENT_METHOD", "PWD"));
            parameters.add(new BasicNameValuePair("choice", "PWD"));
            parameters.add(new BasicNameValuePair("forward", "Logga in"));

            Log.d(TAG, "logging in...");
            res = HttpHelper.post(httpClient, LOGIN_URL3, parameters);
            

            if (res.contains("errors.header")) {
                //login failed.. bail
            	 throw new AuthenticationException("auth fail");
            }

           
            
            //ACCOUNTS
            Log.d(TAG, "getting account info...");
            res = HttpHelper.get(httpClient, ACCOUNT_URL);
            //Log.d(TAG, "accounts html dump:");
            //Log.d(TAG, res);*

            
           Pattern pattern = Pattern.compile(ACCOUNTS_REGEX,Pattern.DOTALL);
           Matcher matcher = pattern.matcher(res);

           int ordinal=1;
           while (matcher.find()) {

            	String remoteId = String.valueOf(ordinal);
                ordinal = ordinal++;
                String name = "Statoil Mastercard";
                long balance = Long.parseLong(matcher.group(1).replaceAll("[^0-9\\-]", ""))/100;
                accounts.put(new AccountHashKey(remoteId, mBankLogin.getId()), new Account(remoteId, mBankLogin.getId(), ordinal, name, balance));
            }
                
                
         

        } catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new StatoilException(e.getMessage(), e);
		} catch (HttpException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new StatoilException(e.getMessage(), e);
		} finally {
			httpClient.getConnectionManager().shutdown();
		}

        Log.d(TAG, "<- getAccounts()");

        return accounts;

    }
}

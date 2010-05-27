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

package com.adrup.saldo.bank.preem;

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
 * A Preem implementation of {@link BankManager}.
 * 
 * @author Sed
 * @author Kristian Adrup
 *
 */
public class PreemManager implements BankManager {
    private static final String TAG = "PreemManager";
    private static final String NAME = "Preem";
    public final static String KEY_PREFIX = "PR_";

    private static final String LOGIN_URL = "https://partner.ikanobank.se/web/engines/page.aspx?structid=1437";
    private static final String ACCOUNT_URL = "https://partner.ikanobank.se/web/engines/page___1401.aspx";
    private static final String USER_PARAM = "ctl08$LoginWebUserControl$SSNControl$SSNSimpleValueUsercontrol$editValueTextbox";
    private static final String PASS_PARAM = "ctl08$LoginWebUserControl$passwordSimpleValueControl$passwordSimpleValueControl$editValueTextbox";

    private static final String EVENTVALIDATION_REGEX = "__EVENTVALIDATION\"\\s+value=\"([^\"]+)\"";
	private static final String VIEWSTATE_REGEX = "__VIEWSTATE\"\\s+value=\"([^\"]+)\"";
	
    /*
     * 

<span id="CustomerAccountInformationWebUserControl_BalanceAmountUserControl_AmountSimpleValueUsercontrol_LabelSpan">
    <span id="ctl11_CustomerAccountInformationWebUserControl_BalanceAmountUserControl_AmountSimpleValueUsercontrol_captionLabel">Aktuellt saldo</span>

</span>
<span id="CustomerAccountInformationWebUserControl_BalanceAmountUserControl_AmountSimpleValueUsercontrol_ReadOnlyValueSpan">
    271,00
</span>

      

    <span id="CustomerAccountInformationWebUserControl_BalanceAmountUserControl_currencyTextLiteralSpan">
         kr
    </span>
    <span id="ctl11_CustomerAccountInformationWebUserControl_BalanceAmountUserControl_AmountValidator" style="color:Red;display:none;">!</span>
    <span id="ctl11_CustomerAccountInformationWebUserControl_BalanceAmountUserControl_AmountRequiredFieldValidator" style="color:Red;display:none;">!</span>

</span>
    </span>
    <span id="CustomerAccountInformationWebUserControl_NotUsedLimitAmountUserControlSpan">
        
<span id="CustomerAccountInformationWebUserControl_NotUsedLimitAmountUserControl_AmountSpan">
    
<span id="CustomerAccountInformationWebUserControl_NotUsedLimitAmountUserControl_AmountSimpleValueUsercontrol_LabelSpan">
    <span id="ctl11_CustomerAccountInformationWebUserControl_NotUsedLimitAmountUserControl_AmountSimpleValueUsercontrol_captionLabel">Kvar att handla för</span>
</span>
<span id="CustomerAccountInformationWebUserControl_NotUsedLimitAmountUserControl_AmountSimpleValueUsercontrol_ReadOnlyValueSpan">
    229,00
</span>



     */
    private static final String ACCOUNTS_REGEX = 
    	"CustomerAccountInformationWebUserControl_BalanceAmountUserControl_AmountSimpleValueUsercontrol_ReadOnlyValueSpan\">([^<]+)";
	
	private BankLogin mBankLogin;
	private Context mContext;

	public PreemManager(BankLogin bankLogin, Context context) {
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
        //HttpClient httpClient = new SaldoHttpClient(mContext);

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		// Android doesn't like ICA's cert, so we need a forgiving TrustManager
		schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));
		HttpParams params = new BasicHttpParams();
		HttpClient httpClient = new SaldoHttpClient(mContext, new ThreadSafeClientConnManager(params, schemeRegistry), null);
	
		try {
        	String res = HttpHelper.get(httpClient, LOGIN_URL);
        	
        	Matcher matcher = Pattern.compile(VIEWSTATE_REGEX).matcher(res);
			if (!matcher.find()) {
				Log.e(TAG, "No viewstate match.");
				Log.d(TAG, res);
				throw new PreemException("No viewState match.");
				
			}
			String viewState = matcher.group(1);
			Log.d(TAG, "viewState= " + viewState);
			
			matcher = Pattern.compile(EVENTVALIDATION_REGEX).matcher(res);
			if (!matcher.find()) {
				Log.e(TAG, "No evenValidation match.");
				Log.d(TAG, res);
				throw new PreemException("No evenValidation match.");
			}
			String evenValidation = matcher.group(1);
			Log.d(TAG, "evenValidation= " + evenValidation);
			
			// Do login
            List<NameValuePair> parameters = new ArrayList<NameValuePair>(3);
            //parameters.add(new BasicNameValuePair(TOKEN_PARAM, token));
            parameters.add(new BasicNameValuePair(USER_PARAM, mBankLogin.getUsername()));
            parameters.add(new BasicNameValuePair(PASS_PARAM, mBankLogin.getPassword()));
            parameters.add(new BasicNameValuePair("ctl08$LoginButton", "Logga in"));
            parameters.add(new BasicNameValuePair("__EVENTVALIDATION", evenValidation));
            parameters.add(new BasicNameValuePair("__VIEWSTATE", viewState));

            Log.d(TAG, "logging in...");
            
            
            
            res = HttpHelper.post(httpClient, LOGIN_URL, parameters);
            
            if (res.contains("<li>Felaktig")) {
                //login failed.. bail
                throw new AuthenticationException("auth fail");
            }

            //ACCOUNTS
            Log.d(TAG, "getting account info...");
            res = HttpHelper.get(httpClient, ACCOUNT_URL);
            //Log.d(TAG, "accounts html dump:");
            //Log.d(TAG, res);*
             
            
           Pattern pattern = Pattern.compile(ACCOUNTS_REGEX);
           matcher = pattern.matcher(res);

           int ordinal=1;
           while (matcher.find()) {

            	String remoteId = String.valueOf(ordinal);
                ordinal = ordinal++;
                String name = "Preem";
                long balance = Long.parseLong(matcher.group(1).replaceAll("[^0-9\\-]", ""))/100;
                accounts.put(new AccountHashKey(remoteId, mBankLogin.getId()), new Account(remoteId, mBankLogin.getId(), ordinal, name, balance));
            }
                
                
         

        } catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new IcaException(e.getMessage(), e);
		} catch (HttpException e) {
			Log.e(TAG, e.getMessage(), e);
			throw new IcaException(e.getMessage(), e);
		} finally {
			httpClient.getConnectionManager().shutdown();
		}

        Log.d(TAG, "<- getAccounts()");

        return accounts;

    }
}

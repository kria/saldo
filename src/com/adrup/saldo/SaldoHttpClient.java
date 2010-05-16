package com.adrup.saldo;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.content.Context;
import android.os.Build;

import java.util.Locale;

public class SaldoHttpClient extends DefaultHttpClient {
	private Context mContext;
	private static String mUserAgent;
	private static Locale sLocale;
	private static Object sLockForLocaleSettings;

	public SaldoHttpClient(Context context) {
		this(context, null, null);
	}

	public SaldoHttpClient(Context context, HttpParams params) {
		this(context, null, params);
	}

	public SaldoHttpClient(Context context, ClientConnectionManager conman, HttpParams params) {
		super(conman, params);
		mContext = context;
		if (sLockForLocaleSettings == null) {
			sLockForLocaleSettings = new Object();
			sLocale = Locale.getDefault();
		}
		mUserAgent = getCurrentUserAgent();
		getParams().setParameter(HttpProtocolParams.USER_AGENT, mUserAgent);
	}

	/**
	 * Looks at sLocale and mContext and returns current UserAgent String.
	 * 
	 * @return Current UserAgent String.
	 */
	private synchronized String getCurrentUserAgent() {
		Locale locale;
		synchronized (sLockForLocaleSettings) {
			locale = sLocale;
		}
		StringBuffer buffer = new StringBuffer();
		// Add version
		final String version = Build.VERSION.RELEASE;
		if (version.length() > 0) {
			buffer.append(version);
		} else {
			// default to "1.0"
			buffer.append("1.0");
		}
		buffer.append("; ");
		final String language = locale.getLanguage();
		if (language != null) {
			buffer.append(language.toLowerCase());
			final String country = locale.getCountry();
			if (country != null) {
				buffer.append("-");
				buffer.append(country.toLowerCase());
			}
		} else {
			// default to "en"
			buffer.append("en");
		}

		final String model = Build.MODEL;
		if (model.length() > 0) {
			buffer.append("; ");
			buffer.append(model);
		}
		final String id = Build.ID;
		if (id.length() > 0) {
			buffer.append(" Build/");
			buffer.append(id);
		}
		final String base = mContext.getResources().getText(R.string.web_user_agent).toString();
		return String.format(base, Saldo.getVersionName(mContext), buffer);
	}

}

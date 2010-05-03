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

package com.adrup.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Convenience class for doing GET/POST HTTP requests. Use in own thread!
 * 
 * @author Kristian Adrup
 *
 */
public class HTTPHelper {
	private static final String TAG = "HTTPHelper";

	private static byte[] sBuffer = new byte[1024];

	public static String get(HttpClient httpClient, String url) throws IOException, HTTPException {
		HttpGet request = new HttpGet(url);
		return doRequest(httpClient, request, url, true);
	}

	public static String post(HttpClient httpClient, String url, List<? extends NameValuePair> parameters)
			throws IOException, HTTPException {
		return post(httpClient, url, parameters, true);
	}

	public static String post(HttpClient httpClient, String url, List<? extends NameValuePair> parameters,
			boolean readResponse) throws IOException, HTTPException {
		HttpPost request = new HttpPost(url);
		request.setEntity(new UrlEncodedFormEntity(parameters));
		return doRequest(httpClient, request, url, readResponse);
	}

	private static synchronized String doRequest(HttpClient httpClient, HttpUriRequest request, String url,
			boolean readResponse) throws IOException, HTTPException {
		Log.d(TAG, "doRequest()");
		if (httpClient == null)
			httpClient = new DefaultHttpClient();

		HttpResponse response = httpClient.execute(request);

		// Check if server response is valid
		StatusLine status = response.getStatusLine();
		int statusCode = status.getStatusCode();
		if (statusCode != HttpStatus.SC_OK) {
			throw new HTTPException(statusCode, status.toString());
		}

		if (!readResponse)
			return null;

		// Pull content stream from response
		HttpEntity entity = response.getEntity();
		InputStream in = entity.getContent();

		ByteArrayOutputStream content = new ByteArrayOutputStream();

		// Read response into a buffered stream
		int readBytes = 0;
		while ((readBytes = in.read(sBuffer)) != -1) {
			content.write(sBuffer, 0, readBytes);
		}

		// Return result from buffered stream
		return new String(content.toByteArray());

		// dispose of in perhaps?
	}
}

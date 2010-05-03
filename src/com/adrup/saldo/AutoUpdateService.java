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

package com.adrup.saldo;

import com.adrup.saldo.bank.AuthenticationException;
import com.adrup.saldo.bank.BankException;
import com.adrup.saldo.bank.BankLogin;
import com.adrup.saldo.bank.BankManager;
import com.adrup.saldo.bank.BankManagerFactory;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AutoUpdateService extends Service {
	private static final String TAG = "AutoUpdateService";

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate()");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(TAG, "-> onStart()");

		new UpdateTask().execute();
		Log.d(TAG, "<- onStart()");
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind()");
		return null;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
	}

	private class UpdateTask extends AsyncTask<Void, String, Void> {
		private static final String TAG = "AutoUpdateService.UpdateTask";

		@Override
		protected Void doInBackground(Void... progress) {
			Log.d(TAG, "doInBackground()");
			run();
			return null;
		}

		@Override
		protected void onCancelled() {
			Log.d(TAG, "onCancelled()");
		}

		@Override
		protected void onProgressUpdate(String... progress) {
			Log.d(TAG, "onProgressUpdate: " + progress[0]);
			Toast.makeText(AutoUpdateService.this, progress[0], Toast.LENGTH_LONG).show();
		}

		@Override
		protected void onPostExecute(Void result) {
			Log.d(TAG, "onPostExecute()");
		}

		private void run() {

			DatabaseAdapter dbAdapter = new DatabaseAdapter(AutoUpdateService.this);
			try {
				dbAdapter.open();

				Map<AccountHashKey, Account> localAccounts = dbAdapter.fetchAllAccountsMap();
				Map<AccountHashKey, Account> remoteAccounts = new LinkedHashMap<AccountHashKey, Account>();
				List<BankLogin> bankLogins = dbAdapter.fetchAllBankLogins();
				for (BankLogin bankLogin : bankLogins) {
					try {
						BankManager bankManager = BankManagerFactory.createBankManager(bankLogin);
						bankManager.getAccounts(remoteAccounts);

						for (Account acc : remoteAccounts.values()) {
							boolean result = dbAdapter.saveAccount(acc);
							Log.d(TAG, "createAccount result= " + result);
						}
					} catch (AuthenticationException e) {
						AutoUpdateReceiver.stopAlarm(AutoUpdateService.this);
						sendNotification(bankLogin.getName() + " authentication failed, disabling auto update.");
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this);
						prefs.edit().putInt(Constants.PREF_UPDATE_INTERVAL, 0).commit();
					} catch (BankException e) {
						Log.e(TAG, "Update failed for " + bankLogin.getName());
						Log.e(TAG, e.getMessage(), e);
						publishProgress(e.getMessage());
					} catch (SQLException e) {
						Log.e(TAG, e.getMessage(), e);
						publishProgress(e.getMessage());
					}
				}
				boolean hasChanges = false;
				for (AccountHashKey key : localAccounts.keySet()) {
					Account localAccount = localAccounts.get(key);
					Account remoteAccount = remoteAccounts.get(key);
					if (localAccount != null && remoteAccount != null
							&& localAccount.getBalance() != remoteAccount.getBalance()) {
						hasChanges = true;
						long diff = remoteAccount.getBalance() - localAccount.getBalance();
						String diffstr = Util.toCurrencyString(diff);
						if (diff > 0)
							diffstr = "+" + diffstr;
						String msg = String.format("%s (%s) = %s", localAccount.getName(), diffstr, Util
								.toCurrencyString(remoteAccount.getBalance()));
						sendNotification(msg);

						// publishProgress(msg);
						Log.d(TAG, msg);
					}
				}
				// TODO: this could be more directed..
				if (hasChanges) {
					sendWidgetRefresh();
				}

			} catch (SQLException e) {
				Log.e(TAG, e.getMessage(), e);
				publishProgress(e.getMessage());
			} catch (Exception e) {
				Log.e(TAG, e.getMessage(), e);
				publishProgress(e.getMessage());
			} finally {
				dbAdapter.close();
			}

		}
	}

	private void sendNotification(String msg) {
		NotificationManager nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.icon, msg, System.currentTimeMillis());
		notification.defaults |= Notification.DEFAULT_SOUND;

		CharSequence contentTitle = this.getString(R.string.app_name);
		CharSequence contentText = msg;
		Intent notificationIntent = new Intent(this, Saldo.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
		int id = new Random().nextInt(); // TODO: hrrm
		nm.notify(id, notification);
	}
	
    private void sendWidgetRefresh() {
        Intent updateIntent = new Intent(AutoUpdateReceiver.WIDGET_REFRESH);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getBaseContext(), 0, updateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
            try {
				pendingIntent.send();
			} catch (CanceledException e) {
				// TODO Auto-generated catch block
				Log.e(TAG, e.getMessage(), e);
			}
    }
}

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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class AutoUpdateReceiver extends BroadcastReceiver {
	private static final String TAG = "AutoUpdateReceiver";
	public static final String AUTO_UPDATE = "com.adrup.saldo.AUTO_UPDATE";
	public static final String WIDGET_REFRESH = "com.adrup.saldo.WIDGET_REFRESH";

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.d(TAG, "-> onReceive()");
		String action = intent.getAction();
		Log.d(TAG, "action:" + action);
		if (action.contentEquals(AUTO_UPDATE)) {
			Log.d(TAG, "AUTO UPDATE!");
			// TODO: wakelock?
			context.startService(new Intent(context, AutoUpdateService.class));
		} else if (action.contentEquals("android.intent.action.BOOT_COMPLETED")) {
			Log.d(TAG, "BOOT!");
			setAlarm(context);
		}
		Log.d(TAG, "<- onReceive()");
	}

	public static void setAlarm(Context context) {
		Log.d(TAG, "setAlarm()");

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		int interval_id = prefs.getInt(Constants.PREF_UPDATE_INTERVAL, 0);
		long interval;

		// TODO: magic numbers ftw!
		switch (interval_id) {
		case 0:
			stopAlarm(context);
			return;
		case 1:
			interval = AlarmManager.INTERVAL_DAY;
			break;
		case 2:
			interval = AlarmManager.INTERVAL_HALF_DAY;
			break;
		case 3:
			interval = AlarmManager.INTERVAL_HOUR;
			break;
		case 4:
			interval = AlarmManager.INTERVAL_HALF_HOUR;
			break;
		case 5:
			interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
			break;
		default:
			stopAlarm(context);
			return;
		}

		Intent updateIntent = new Intent(AUTO_UPDATE);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, updateIntent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		int triggerAtTime = 20 * 1000;
		alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, interval, pendingIntent);
	}

	public static void stopAlarm(Context context) {
		Log.d(TAG, "-> stopAlarm()");

		Intent updateIntent = new Intent(AUTO_UPDATE);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, updateIntent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		alarmManager.cancel(pendingIntent);

		Log.d(TAG, "<- stopAlarm()");
	}

}

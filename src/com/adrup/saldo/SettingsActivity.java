package com.adrup.saldo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

public class SettingsActivity extends Activity {
	private static final String TAG = "SettingsActivity";
	
	private Spinner mSpinner;
	private CheckBox mNotifyCheckBox;
	private CheckBox mSoundCheckBox;
	private SharedPreferences mPrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		mSpinner = (Spinner) findViewById(R.id.settings_autoupdate_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.update_interval_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinner.setAdapter(adapter);
		
		mNotifyCheckBox = (CheckBox) findViewById(R.id.settings_notifications_enable_checkbox);
		mSoundCheckBox = (CheckBox) findViewById(R.id.settings_notifications_sound_checkbox);
	}


	@Override
	protected void onResume() {
		Log.d(TAG, "onResume()");
		super.onResume();
		mSpinner.setSelection(mPrefs.getInt(Constants.PREF_UPDATE_INTERVAL, 0));
		mNotifyCheckBox.setChecked(mPrefs.getBoolean(Constants.PREF_NOTIFY, true));
		mSoundCheckBox.setChecked(mPrefs.getBoolean(Constants.PREF_SOUND, true));
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause()");
		super.onPause();
		int interval = mSpinner.getSelectedItemPosition();
		int prevInterval = mPrefs.getInt(Constants.PREF_UPDATE_INTERVAL, 0);
		if (interval != prevInterval) {
			mPrefs.edit().putInt(Constants.PREF_UPDATE_INTERVAL, interval).commit();
			AutoUpdateReceiver.setAlarm(this);
		}
		mPrefs.edit().putBoolean(Constants.PREF_NOTIFY, mNotifyCheckBox.isChecked()).commit();
		mPrefs.edit().putBoolean(Constants.PREF_SOUND, mSoundCheckBox.isChecked()).commit();
	}

}

package com.pyler.xinternalsd;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.preferences);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		PreferenceCategory appSettings = (PreferenceCategory) findPreference("app_settings");
		Preference sdCardFullAccess = getPreferenceScreen().findPreference(
				"sdcard_full_access");
		EditTextPreference internalSdPath = (EditTextPreference) getPreferenceScreen()
				.findPreference("internal_sd_path");

		String extStorage = System.getenv("SECONDARY_STORAGE");
		if (extStorage != null && !extStorage.isEmpty()) {
			String externalSd = extStorage.split(":")[0];
			internalSdPath.setSummary(externalSd);
			internalSdPath.setText(externalSd);
			prefs.edit().putString("internal_sd_path", externalSd).apply();
		}

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			appSettings.removePreference(sdCardFullAccess);
		}
	}
}
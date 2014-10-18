package com.pyler.xinternalsd;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class Preferences extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener {
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.prefs);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		boolean isEnabledModule = prefs.getBoolean("custom_internal_sd", true);
		boolean enabledForAllApps = prefs.getBoolean("enable_for_all_apps",
				true);
		@SuppressLint("SdCardPath")
		String mInternalSDCard = prefs.getString("internal_sd_path", "/sdcard");
		PreferenceScreen preferences = getPreferenceScreen();
		Preference internalSD = preferences.findPreference("internal_sd_path");
		Preference enableForApps = preferences
				.findPreference("enable_for_apps");
		Preference enableForAllApps = preferences
				.findPreference("enable_for_all_apps");
		Preference disableForApps = preferences
				.findPreference("disable_for_apps");
		Preference changeDownloadPath = preferences
				.findPreference("change_download_path");
		Preference sdCardFullAccess = preferences
				.findPreference("sdcard_full_access");
		boolean isKitKatOrNewer = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) ? true
				: false;
		if (!isKitKatOrNewer) {
			sdCardFullAccess.setEnabled(false);
		}
		internalSD.setEnabled(isEnabledModule);
		if (isEnabledModule) {
			enableForApps.setEnabled(!enabledForAllApps);
			disableForApps.setEnabled(enabledForAllApps);
		} else {
			enableForApps.setEnabled(isEnabledModule);
			disableForApps.setEnabled(isEnabledModule);

		}
		internalSD.setSummary(mInternalSDCard);
		enableForAllApps.setEnabled(isEnabledModule);
		changeDownloadPath.setEnabled(isEnabledModule);
	}
}
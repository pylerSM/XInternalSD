package com.pyler.xinternalsd;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

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

	@SuppressWarnings("deprecation")
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		boolean isEnabledModule = prefs.getBoolean("custom_internal_sd", true);
		boolean enabledForAllApps = prefs.getBoolean("enable_for_all_apps",
				true);
		@SuppressLint("SdCardPath")
		String mInternalSDCard = prefs.getString("internal_sd_path", "/sdcard");
		Preference internalSD = getPreferenceScreen().findPreference(
				"internal_sd_path");
		Preference enableForApps = getPreferenceScreen().findPreference(
				"enable_for_apps");
		Preference enableForAllApps = getPreferenceScreen().findPreference(
				"enable_for_all_apps");
		Preference disableForApps = getPreferenceScreen().findPreference(
				"disable_for_apps");
		Preference changeDownloadPath = getPreferenceScreen().findPreference(
				"change_download_path");
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
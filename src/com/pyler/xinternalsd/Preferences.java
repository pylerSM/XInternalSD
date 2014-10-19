package com.pyler.xinternalsd;

import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity  {
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.prefs);
		Preference sdCardFullAccess = getPreferenceScreen()
				.findPreference("sdcard_full_access");
		boolean isKitKatOrNewer = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) ? true
				: false;
		sdCardFullAccess.setEnabled(isKitKatOrNewer);
	}
}
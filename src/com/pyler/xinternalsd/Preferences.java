package com.pyler.xinternalsd;

import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;

@SuppressWarnings("deprecation")
public class Preferences extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.preferences);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		PreferenceCategory appSettings = (PreferenceCategory) findPreference("app_settings");
		Preference sdCardFullAccess = findPreference("sdcard_full_access");
		EditTextPreference internalSdPath = (EditTextPreference) findPreference("internal_sd_path");

		new LoadApps().execute();

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

	public class LoadApps extends AsyncTask<Void, Void, Void> {
		MultiSelectListPreference enabledApps = (MultiSelectListPreference) findPreference("enable_for_apps");
		MultiSelectListPreference disabledApps = (MultiSelectListPreference) findPreference("disable_for_apps");
		List<CharSequence> apps = new ArrayList<CharSequence>();
		List<CharSequence> packageNames = new ArrayList<CharSequence>();

		@Override
		protected void onPreExecute() {
			enabledApps.setEnabled(false);
			disabledApps.setEnabled(false);
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			PackageManager pm = getPackageManager();
			List<ApplicationInfo> packages = pm
					.getInstalledApplications(PackageManager.GET_META_DATA);
			for (ApplicationInfo app : packages) {
				if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 1) {
					apps.add(app.loadLabel(getPackageManager()));
					packageNames.add(app.packageName);
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			CharSequence[] appsList = apps
					.toArray(new CharSequence[apps.size()]);
			CharSequence[] packageNamesList = packageNames
					.toArray(new CharSequence[packageNames.size()]);

			enabledApps.setEntries(appsList);
			enabledApps.setEntryValues(packageNamesList);
			disabledApps.setEntries(appsList);
			disabledApps.setEntryValues(packageNamesList);
			enabledApps.setEnabled(true);
			disabledApps.setEnabled(true);
		}
	}

}
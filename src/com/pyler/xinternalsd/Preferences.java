package com.pyler.xinternalsd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class Preferences extends Activity {
	public static Context context;
	public static SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getApplicationContext();
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new Settings()).commit();
	}

	@SuppressWarnings("deprecation")
	public static class Settings extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			getPreferenceManager()
					.setSharedPreferencesMode(MODE_WORLD_READABLE);
			addPreferencesFromResource(R.xml.preferences);
			prefs = PreferenceManager.getDefaultSharedPreferences(context);
			PreferenceCategory appSettings = (PreferenceCategory) findPreference("app_settings");
			Preference externalSdCardFullAccess = findPreference("external_sdcard_full_access");
			EditTextPreference internalSdPath = (EditTextPreference) findPreference("internal_sdcard_path");
			Preference includeSystemApps = findPreference("include_system_apps");
			includeSystemApps
					.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
						@Override
						public boolean onPreferenceChange(
								Preference preference, Object newValue) {
							reloadAppsList();
							return true;
						}
					});

			reloadAppsList();

			String customInternalSd = prefs.getString("internal_sdcard_path",
					"");
			if (!customInternalSd.isEmpty()) {
				internalSdPath.setSummary(customInternalSd);
			}

			String externalStorage = System.getenv("SECONDARY_STORAGE");
			if (externalStorage != null && !externalStorage.isEmpty()
					&& customInternalSd.isEmpty()) {
				String externalSd = externalStorage.split(":")[0];
				internalSdPath.setSummary(externalSd);
				internalSdPath.setText(externalSd);
				prefs.edit().putString("internal_sdcard_path", externalSd)
						.apply();
			}

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
				appSettings.removePreference(externalSdCardFullAccess);
			}
		}

		public void reloadAppsList() {
			new LoadApps().execute();
		}

		public boolean isAllowedApp(ApplicationInfo appInfo) {
			boolean isAllowedApp = true;
			boolean includeSystemApps = prefs.getBoolean("include_system_apps",
					false);
			if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
					&& !includeSystemApps) {
				isAllowedApp = false;
			}
			return isAllowedApp;
		}

		public class LoadApps extends AsyncTask<Void, Void, Void> {
			MultiSelectListPreference enabledApps = (MultiSelectListPreference) findPreference("enable_for_apps");
			MultiSelectListPreference disabledApps = (MultiSelectListPreference) findPreference("disable_for_apps");
			List<CharSequence> appNames = new ArrayList<CharSequence>();
			List<CharSequence> packageNames = new ArrayList<CharSequence>();
			PackageManager pm = context.getPackageManager();
			List<ApplicationInfo> packages = pm
					.getInstalledApplications(PackageManager.GET_META_DATA);

			@Override
			protected void onPreExecute() {
				enabledApps.setEnabled(false);
				disabledApps.setEnabled(false);
			}

			@Override
			protected Void doInBackground(Void... arg0) {
				List<String[]> sortedApps = new ArrayList<String[]>();

				for (ApplicationInfo app : packages) {
					if (isAllowedApp(app)) {
						sortedApps.add(new String[] {
								app.packageName,
								app.loadLabel(context.getPackageManager())
										.toString() });
					}
				}

				Collections.sort(sortedApps, new Comparator<String[]>() {
					@Override
					public int compare(String[] entry1, String[] entry2) {
						return entry1[1].compareToIgnoreCase(entry2[1]);
					}
				});

				for (int i = 0; i < sortedApps.size(); i++) {
					appNames.add(sortedApps.get(i)[1]);
					packageNames.add(sortedApps.get(i)[0]);
				}

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				CharSequence[] appNamesList = appNames
						.toArray(new CharSequence[appNames.size()]);
				CharSequence[] packageNamesList = packageNames
						.toArray(new CharSequence[packageNames.size()]);

				enabledApps.setEntries(appNamesList);
				enabledApps.setEntryValues(packageNamesList);
				enabledApps.setEnabled(true);
				disabledApps.setEntries(appNamesList);
				disabledApps.setEntryValues(packageNamesList);
				disabledApps.setEnabled(true);
			}
		}

	}
}

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
			Preference sdCardFullAccess = findPreference("sdcard_full_access");
			EditTextPreference internalSdPath = (EditTextPreference) findPreference("internal_sd_path");
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

			String customInternalSD = prefs.getString("internal_sd_path", "");
			if (!customInternalSD.isEmpty()) {
				internalSdPath.setSummary(customInternalSD);
			}

			String extStorage = System.getenv("SECONDARY_STORAGE");
			if (extStorage != null && !extStorage.isEmpty()
					&& customInternalSD.isEmpty()) {
				String externalSd = extStorage.split(":")[0];
				internalSdPath.setSummary(externalSd);
				internalSdPath.setText(externalSd);
				prefs.edit().putString("internal_sd_path", externalSd).apply();
			}

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
				appSettings.removePreference(sdCardFullAccess);
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
			List<CharSequence> apps = new ArrayList<CharSequence>();
			List<CharSequence> packageNames = new ArrayList<CharSequence>();
			PackageManager pm = context.getPackageManager();
			List<ApplicationInfo> packages = pm
					.getInstalledApplications(PackageManager.GET_META_DATA);

			@Override
			protected void onPreExecute() {
				enabledApps.setEnabled(false);
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
					packageNames.add(sortedApps.get(i)[0]);
					apps.add(sortedApps.get(i)[1]);
				}

				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				CharSequence[] appsList = apps.toArray(new CharSequence[apps
						.size()]);
				CharSequence[] packageNamesList = packageNames
						.toArray(new CharSequence[packageNames.size()]);

				enabledApps.setEntries(appsList);
				enabledApps.setEntryValues(packageNamesList);
				enabledApps.setEnabled(true);
			}
		}

	}
}

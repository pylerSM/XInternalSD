package com.pyler.xinternalsd;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

			reloadSystemDirs();
			reloadAppsList();
			
			String customInternalSD = prefs.getString("internal_sd_path", "");
			if (!customInternalSD.isEmpty()) {
				internalSdPath.setSummary(customInternalSD);
			}
			
			String extStorage = System.getenv("SECONDARY_STORAGE");
			if (extStorage != null && !extStorage.isEmpty() && customInternalSD.isEmpty()) {
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

		public void reloadSystemDirs() {
			new LoadSystemDirs().execute();
		}

		public boolean isAllowedApp(ApplicationInfo appInfo) {
			boolean isAllowedApp = false;
			boolean includeSystemApps = prefs.getBoolean("include_system_apps",
					false);
			if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1
					|| includeSystemApps) {
				isAllowedApp = true;
			}
			if ((appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
				isAllowedApp = true;
			}
			return isAllowedApp;
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
				PackageManager pm = context.getPackageManager();
				List<ApplicationInfo> packages = pm
						.getInstalledApplications(PackageManager.GET_META_DATA);
				for (ApplicationInfo app : packages) {
					if (isAllowedApp(app)) {
						apps.add(app.loadLabel(context.getPackageManager()));
						packageNames.add(app.packageName);
					}
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
				disabledApps.setEntries(appsList);
				disabledApps.setEntryValues(packageNamesList);
				enabledApps.setEnabled(true);
				disabledApps.setEnabled(true);
			}
		}

		@SuppressLint("NewApi")
		public class LoadSystemDirs extends AsyncTask<Void, Void, Void> {
			MultiSelectListPreference changeSystemDirsPath = (MultiSelectListPreference) findPreference("change_system_dirs_path");
			List<CharSequence> systemDirNames = new ArrayList<CharSequence>();
			List<CharSequence> systemDirs = new ArrayList<CharSequence>();

			@Override
			protected void onPreExecute() {
				changeSystemDirsPath.setEnabled(false);
			}

			@Override
			protected Void doInBackground(Void... arg0) {
				String space = " ";
				systemDirNames.add(getString(R.string.alarms) + space + "(Alarms)");
				systemDirs.add(Environment.DIRECTORY_ALARMS);
				systemDirNames.add(getString(R.string.dcim) + space + "(DCIM)");
				systemDirs.add(Environment.DIRECTORY_DCIM);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					systemDirNames.add(getString(R.string.documents  + space + "(Documents)"));
					systemDirs.add(Environment.DIRECTORY_DOCUMENTS);
				}
				systemDirNames.add(getString(R.string.downloads) + space + "(Downloads)");
				systemDirs.add(Environment.DIRECTORY_DOWNLOADS);
				systemDirNames.add(getString(R.string.movies) + space + "(Movies)");
				systemDirs.add(Environment.DIRECTORY_MOVIES);
				systemDirNames.add(getString(R.string.music) + space + "(Musc)");
				systemDirs.add(Environment.DIRECTORY_MUSIC);
				systemDirNames.add(getString(R.string.notifications) + space + "(Notifications)");
				systemDirs.add(Environment.DIRECTORY_NOTIFICATIONS);
				systemDirNames.add(getString(R.string.pictures) + space + "(Pictures)");
				systemDirs.add(Environment.DIRECTORY_PICTURES);
				systemDirNames.add(getString(R.string.podcasts) + space + "(Podcasts)");
				systemDirs.add(Environment.DIRECTORY_PODCASTS);
				systemDirNames.add(getString(R.string.ringtones) + space + "(Ringtones)");
				systemDirs.add(Environment.DIRECTORY_RINGTONES);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				CharSequence[] systemDirNamesList = systemDirNames
						.toArray(new CharSequence[systemDirNames.size()]);
				CharSequence[] systemDirsList = systemDirs
						.toArray(new CharSequence[systemDirs.size()]);

				changeSystemDirsPath.setEntries(systemDirNamesList);
				changeSystemDirsPath.setEntryValues(systemDirsList);
				changeSystemDirsPath.setEnabled(true);
			}
		}

	}
}

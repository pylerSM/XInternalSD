package com.pyler.xinternalsd;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.os.Environment;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

public class XInternalSD implements IXposedHookZygoteInit {
	public XSharedPreferences prefs;
	public boolean changeDownloadDirPath;
	public boolean isDownloadDir;
	public File internalSdPath;
	public File appFilesPath;
	public File obbDirPath;
	public File downloadDirPath;
	public Class<?> contextImpl = XposedHelpers.findClass(
			"android.app.ContextImpl", null);
	public Class<?> packageManagerService = XposedHelpers.findClass(
			"com.android.server.pm.PackageManagerService", null);
	public XC_MethodHook getExternalStorageDirectoryHook;
	public XC_MethodHook getExternalFilesDirHook;
	public XC_MethodHook getObbDirHook;
	public XC_MethodHook getDownloadDirHook;
	public XC_MethodHook externalSdCardAccessHook;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		prefs = new XSharedPreferences(XInternalSD.class.getPackage().getName());
		prefs.makeWorldReadable();

		getExternalStorageDirectoryHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				prefs.reload();
				String intSd = prefs.getString("internal_sd_path", "");
				internalSdPath = new File(intSd);
			}

			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				if (isAppEnabled()) {
					param.setResult(internalSdPath);
				}
			}

		};

		getExternalFilesDirHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				prefs.reload();
				String arg = (String) param.args[0];
				if (arg == null) {
					String intSd = prefs.getString("internal_sd_path", "");
					String appFiles = intSd + "/Android/data/"
							+ AndroidAppHelper.currentPackageName() + "/files";
					appFilesPath = new File(appFiles);

				}
			}

			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				if (isAppEnabled()) {
					param.setResult(appFilesPath);
				}
			}

		};

		getObbDirHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				prefs.reload();
				String intSd = prefs.getString("internal_sd_path", "");
				String obbDir = intSd + "/Android/obb/"
						+ AndroidAppHelper.currentPackageName() + "/";
				obbDirPath = new File(obbDir);

			}

			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				if (isAppEnabled()) {
					param.setResult(obbDirPath);
				}
			}

		};

		getDownloadDirHook = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				prefs.reload();
				changeDownloadDirPath = prefs.getBoolean(
						"change_download_path", true);
				String type = (String) param.args[0];
				isDownloadDir = Environment.DIRECTORY_DOWNLOADS.equals(type);
				String intSd = prefs.getString("internal_sd_path",
							"");
				String downloadDir = intSd + "/Download";
				downloadDirPath = new File(downloadDir);
			}

			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				if (isAppEnabled() && isDownloadDir && changeDownloadDirPath) {
					param.setResult(downloadDirPath);
				}
			}

		};

		externalSdCardAccessHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				prefs.reload();
				String permission = (String) param.args[1];
				boolean sdCardFullAccess = prefs.getBoolean(
						"sdcard_full_access", true);
				if (sdCardFullAccess
						&& (permission
								.equals("android.permission.WRITE_EXTERNAL_STORAGE") || permission
								.equals("android.permission.ACCESS_ALL_EXTERNAL_STORAGE"))) {
					Class<?> process = XposedHelpers.findClass(
							"android.os.Process", null);
					int gid = (Integer) XposedHelpers.callStaticMethod(process,
							"getGidForName", "media_rw");
					Object settings = XposedHelpers.getObjectField(
							param.thisObject, "mSettings");
					Object permissions = XposedHelpers.getObjectField(settings,
							"mPermissions");
					Object bp = XposedHelpers.callMethod(permissions, "get",
							permission);
					int[] bpGids = (int[]) XposedHelpers.getObjectField(bp,
							"gids");
					XposedHelpers.setObjectField(bp, "gids",
							appendInt(bpGids, gid));
				}
			}
		};

		XposedHelpers.findAndHookMethod(Environment.class,
				"getExternalStorageDirectory", getExternalStorageDirectoryHook);
		XposedHelpers.findAndHookMethod(contextImpl, "getExternalFilesDir",
				String.class, getExternalFilesDirHook);
		XposedHelpers
				.findAndHookMethod(contextImpl, "getObbDir", getObbDirHook);
		XposedHelpers.findAndHookMethod(Environment.class,
				"getExternalStoragePublicDirectory", String.class,
				getDownloadDirHook);
		XposedHelpers.findAndHookMethod(packageManagerService,
				"readPermission", "org.xmlpull.v1.XmlPullParser", String.class,
				externalSdCardAccessHook);
	}

	public boolean isAppEnabled() {
		boolean isAppEnabled = true;
		boolean moduleEnabled = prefs.getBoolean("custom_internal_sd", true);
		if (!moduleEnabled) {
			return false;
		}
		Context context = AndroidAppHelper.currentApplication();
		if (context == null) {
			return false;
		}
		String packageName = AndroidAppHelper.currentPackageName();
		boolean enabledForAllApps = prefs.getBoolean("enable_for_all_apps",
				true);
		if (enabledForAllApps) {
			String disabledApps = prefs.getString("disable_for_apps", "");
			if (!disabledApps.isEmpty()) {
				isAppEnabled = !disabledApps.contains(packageName);
			}
		} else {
			String enabledApps = prefs.getString("enable_for_apps", "");
			if (!enabledApps.isEmpty()) {
				isAppEnabled = enabledApps.contains(packageName);
			}
		}
		return isAppEnabled;

	}

	public int[] appendInt(int[] cur, int val) {
		if (cur == null) {
			return new int[] { val };
		}
		final int N = cur.length;
		for (int i = 0; i < N; i++) {
			if (cur[i] == val) {
				return cur;
			}
		}
		int[] ret = new int[N + 1];
		System.arraycopy(cur, 0, ret, 0, N);
		ret[N] = val;
		return ret;
	}
}
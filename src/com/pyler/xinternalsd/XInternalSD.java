package com.pyler.xinternalsd;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;

import android.content.pm.ApplicationInfo;
import android.os.Environment;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XInternalSD implements IXposedHookZygoteInit,
		IXposedHookLoadPackage {
	public XSharedPreferences prefs;
	public String internalSd;
	public XC_MethodHook getExternalStorageDirectoryHook;
	public XC_MethodHook getExternalFilesDirHook;
	public XC_MethodHook getObbDirHook;
	public XC_MethodHook getExternalStoragePublicDirectoryHook;
	public XC_MethodHook externalSdCardAccessHook;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		prefs = new XSharedPreferences(XInternalSD.class.getPackage().getName());
		prefs.makeWorldReadable();

		getExternalStorageDirectoryHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				File path = (File) param.getResult();
				String customInternalSd = path.toString().replaceFirst(
						getInternalSd(), getCustomInternalSd());
				File customInternalSdPath = new File(customInternalSd);
				param.setResult(customInternalSdPath);
			}

		};

		getExternalFilesDirHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				String arg = (String) param.args[0];
				boolean isAppFilesDir = (arg == null);
				File path = (File) param.getResult();
				String appFilesDir = path.toString().replaceFirst(
						getInternalSd(), getCustomInternalSd());
				File appFilesDirPath = new File(appFilesDir);
				if (isAppFilesDir) {
					param.setResult(appFilesDirPath);
				}
			}

		};

		getObbDirHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				File path = (File) param.getResult();
				String obbDir = path.toString().replaceFirst(getInternalSd(),
						getCustomInternalSd());
				File obbDirPath = new File(obbDir);
				param.setResult(obbDirPath);
			}

		};

		getExternalStoragePublicDirectoryHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				prefs.reload();
				String dirType = (String) param.args[0];
				Set<String> changeSystemDirsPath = prefs.getStringSet(
						"change_system_dirs_path", new HashSet<String>());
				boolean isAllowedDir = changeSystemDirsPath.contains(dirType);
				File path = (File) param.getResult();
				String systemDir = path.toString().replaceFirst(
						getInternalSd(), getCustomInternalSd());
				File systemDirPath = new File(systemDir);
				if (isAllowedDir) {
					param.setResult(systemDirPath);
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

		internalSd = Environment.getExternalStorageDirectory().getPath();
		XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
				"com.android.server.pm.PackageManagerService", null),
				"readPermission", XmlPullParser.class, String.class,
				externalSdCardAccessHook);
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!isEnabledApp(lpparam)) {
			return;
		}

		XposedHelpers.findAndHookMethod(Environment.class,
				"getExternalStorageDirectory", getExternalStorageDirectoryHook);
		XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
				"android.app.ContextImpl", lpparam.classLoader),
				"getExternalFilesDir", String.class, getExternalFilesDirHook);
		XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
				"android.app.ContextImpl", lpparam.classLoader), "getObbDir",
				getObbDirHook);
		XposedHelpers.findAndHookMethod(Environment.class,
				"getExternalStoragePublicDirectory", String.class,
				getExternalStoragePublicDirectoryHook);
	}

	public boolean isEnabledApp(LoadPackageParam lpparam) {
		boolean isEnabledApp = true;
		prefs.reload();
		boolean enabledModule = prefs.getBoolean("custom_internal_sd", true);
		boolean includeSystemApps = prefs.getBoolean("include_system_apps",
				false);
		if (!enabledModule) {
			return false;
		}
		if ("android".equals(lpparam.packageName) && includeSystemApps) {
			return true;
		}
		if (lpparam.appInfo == null) {
			return false;
		}
		if (!isAllowedApp(lpparam.appInfo)) {
			return false;
		}
		String packageName = lpparam.appInfo.packageName;
		boolean enabledForAllApps = prefs.getBoolean("enable_for_all_apps",
				false);
		if (enabledForAllApps) {
			Set<String> disabledApps = prefs.getStringSet("disable_for_apps",
					new HashSet<String>());
			if (!disabledApps.isEmpty()) {
				isEnabledApp = !disabledApps.contains(packageName);
			}
		} else {
			Set<String> enabledApps = prefs.getStringSet("enable_for_apps",
					new HashSet<String>());
			if (!enabledApps.isEmpty()) {
				isEnabledApp = enabledApps.contains(packageName);
			} else {
				isEnabledApp = !isEnabledApp;
			}
		}
		return isEnabledApp;

	}

	public String getCustomInternalSd() {
		prefs.reload();
		String customInternalSd = prefs.getString("internal_sd_path",
				getInternalSd());
		return customInternalSd;
	}

	public String getInternalSd() {
		return internalSd;
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
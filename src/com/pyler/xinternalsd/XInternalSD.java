package com.pyler.xinternalsd;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Environment;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
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
	public XC_MethodHook getExternalFilesDirsHook;
	public XC_MethodHook externalSdCardAccessHook;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		prefs = new XSharedPreferences(XInternalSD.class.getPackage().getName());
		prefs.makeWorldReadable();

		getExternalStorageDirectoryHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				changeDirPath(param);
			}
		};

		getExternalFilesDirHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				changeDirPath(param);

			}
		};

		getObbDirHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				changeDirPath(param);
			}
		};

		getExternalStoragePublicDirectoryHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				changeDirPath(param);
			}
		};

		getExternalFilesDirsHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				appendDirPath(param);
			}
		};

		externalSdCardAccessHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				prefs.reload();
				String permission = (String) param.args[1];
				boolean externalSdCardFullAccess = prefs.getBoolean(
						"external_sdcard_full_access", true);
				if (!externalSdCardFullAccess) {
					return;
				}
				if (permission
						.equals("android.permission.WRITE_EXTERNAL_STORAGE")
						|| permission
								.equals("android.permission.ACCESS_ALL_EXTERNAL_STORAGE")) {
					Class<?> process = XposedHelpers.findClass(
							"android.os.Process", null);
					int gid = (Integer) XposedHelpers.callStaticMethod(process,
							"getGidForName", "media_rw");
					Object permissions = null;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						permissions = XposedHelpers.getObjectField(
								param.thisObject, "mPermissions");
					} else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
						Object settings = XposedHelpers.getObjectField(
								param.thisObject, "mSettings");
						permissions = XposedHelpers.getObjectField(settings,
								"mPermissions");
					}
					Object bp = XposedHelpers.callMethod(permissions, "get",
							permission);
					int[] bpGids = (int[]) XposedHelpers.getObjectField(bp,
							"gids");
					XposedHelpers.setObjectField(bp, "gids",
							appendInt(bpGids, gid));
				}
			}
		};

		File internalSdPath = Environment.getExternalStorageDirectory();
		internalSd = internalSdPath.getPath();
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if ("android".equals(lpparam.packageName)
				&& "android".equals(lpparam.processName)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				XposedHelpers.findAndHookMethod(
						XposedHelpers.findClass(
								"com.android.server.SystemConfig",
								lpparam.classLoader), "readPermission",
						XmlPullParser.class, String.class,
						externalSdCardAccessHook);
			} else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
				XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
						"com.android.server.pm.PackageManagerService",
						lpparam.classLoader), "readPermission",
						XmlPullParser.class, String.class,
						externalSdCardAccessHook);
			}
		}
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

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
					"android.app.ContextImpl", lpparam.classLoader),
					"getExternalFilesDirs", String.class,
					getExternalFilesDirsHook);
		}
	}

	public boolean isEnabledApp(LoadPackageParam lpparam) {
		boolean isEnabledApp = true;
		prefs.reload();
		boolean enabledModule = prefs.getBoolean("custom_internal_sd", true);
		if (!enabledModule) {
			return false;
		}
		if (!isAllowedApp(lpparam.appInfo)) {
			return false;
		}
		String packageName = lpparam.packageName;
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

	public void changeDirPath(MethodHookParam param) {
		File oldDirPath = (File) param.getResult();
		String customInternalSd = getCustomInternalSd();
		if (customInternalSd.isEmpty()) {
			return;
		}
		String newDir = oldDirPath.getPath().replaceFirst(getInternalSd(),
				customInternalSd);
		File newDirPath = new File(newDir);
		if (!newDirPath.exists()) {
			newDirPath.mkdirs();
		}
		param.setResult(newDirPath);
	}

	public void appendDirPath(MethodHookParam param) {
		File[] oldDirPaths = (File[]) param.getResult();
		ArrayList<File> newDirPaths = new ArrayList<File>();
		for (File oldDirPath : oldDirPaths) {
			if (oldDirPath != null) {
				newDirPaths.add(oldDirPath);
			}
		}
		String customInternalSd = getCustomInternalSd();
		if (customInternalSd.isEmpty()) {
			return;
		}
		String newDir = oldDirPaths[0].getPath().replaceFirst(getInternalSd(),
				customInternalSd);
		File newDirPath = new File(newDir);
		if (newDirPaths.contains(newDirPath)) {
			newDirPaths.add(newDirPath);
		}
		if (!newDirPath.exists()) {
			newDirPath.mkdirs();
		}
		File[] appendedDirPaths = newDirPaths.toArray(new File[newDirPaths
				.size()]);
		param.setResult(appendedDirPaths);
	}

	public String getCustomInternalSd() {
		prefs.reload();
		String customInternalSd = prefs.getString("internal_sdcard_path",
				getInternalSd());
		return customInternalSd;
	}

	public String getInternalSd() {
		return internalSd;
	}

	public boolean isAllowedApp(ApplicationInfo appInfo) {
		prefs.reload();
		boolean includeSystemApps = prefs.getBoolean("include_system_apps",
				false);
		if (appInfo == null) {
			return includeSystemApps;
		} else {
			if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
					&& !includeSystemApps) {
				return false;
			}
		}
		return true;
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
package com.pyler.xinternalsd;

import java.io.File;
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
	public XC_MethodHook externalSdCardAccessHook;
	public XC_MethodHook grantStoragePermissionsHook;

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

		grantStoragePermissionsHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				prefs.reload();
				boolean externalSdCardFullAccess = prefs.getBoolean(
						"external_sdcard_full_access", true);
				if (!externalSdCardFullAccess) {
					return;
				}
				final Object extras = XposedHelpers.getObjectField(
						param.args[0], "mExtras");
				@SuppressWarnings("unchecked")
				final HashSet<String> grantedPermissions = (HashSet<String>) XposedHelpers
						.getObjectField(extras, "grantedPermissions");
				final Object settings = XposedHelpers.getObjectField(
						param.thisObject, "mSettings");
				final Object permissions = XposedHelpers.getObjectField(
						settings, "mPermissions");
				if (!grantedPermissions
						.contains("android.permission.WRITE_MEDIA_STORAGE")) {
					final Object pWriteMediaStorage = XposedHelpers.callMethod(
							permissions, "get",
							"android.permission.WRITE_MEDIA_STORAGE");
					grantedPermissions
							.add("android.permission.WRITE_MEDIA_STORAGE");
					int[] gpGids = (int[]) XposedHelpers.getObjectField(extras,
							"gids");
					int[] bpGids = (int[]) XposedHelpers.getObjectField(
							pWriteMediaStorage, "gids");
					XposedHelpers.callStaticMethod(param.thisObject.getClass(),
							"appendInts", gpGids, bpGids);
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
				XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
						"com.android.server.pm.PackageManagerService",
						lpparam.classLoader), "grantPermissionsLPw",
						"android.content.pm.PackageParser.Package",
						boolean.class, String.class,
						grantStoragePermissionsHook);
			} else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
				XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
						"com.android.server.pm.PackageManagerService",
						lpparam.classLoader), "readPermission",
						XmlPullParser.class, String.class,
						externalSdCardAccessHook);
				XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
						"com.android.server.pm.PackageManagerService",
						lpparam.classLoader), "grantPermissionsLPw",
						"android.content.pm.PackageParser.Package",
						boolean.class, grantStoragePermissionsHook);
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

	public void changeDirPath(MethodHookParam param) {
		File oldDirPath = (File) param.getResult();
		String newDir = oldDirPath.getPath().replaceFirst(getInternalSd(),
				getCustomInternalSd());
		File newDirPath = new File(newDir);
		if (!newDirPath.exists()) {
			newDirPath.mkdirs();
		}
		param.setResult(newDirPath);
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
		boolean isAllowedApp = true;
		prefs.reload();
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
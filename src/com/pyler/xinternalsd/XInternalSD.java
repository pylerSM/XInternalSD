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
	XSharedPreferences mPrefs;
	boolean mEnabledModule;
	boolean mEnabledForAllApps;
	String mEnabledApps;
	String mDisabledApps;
	File mInternalSDCard;
	File mAppFiles;
	File mObbDir;
	File mDownloadDir;
	Context mContext;
	String mPackageName;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		mPrefs = new XSharedPreferences(XInternalSD.class.getPackage()
				.getName());
		mPrefs.makeWorldReadable();
		XposedHelpers.findAndHookMethod(Environment.class,
				"getExternalStorageDirectory", new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param)
							throws Throwable {
						mPrefs.reload();
						mEnabledModule = mPrefs.getBoolean(
								"custom_internal_sd", true);
						mEnabledForAllApps = mPrefs.getBoolean(
								"enable_for_all_apps", true);
						@SuppressLint("SdCardPath")
						String internalSD = mPrefs.getString(
								"internal_sd_path", "/sdcard");
						mInternalSDCard = new File(internalSD);
						mContext = AndroidAppHelper.currentApplication();
						if (mContext == null) {
							mEnabledModule = false;
						} else {
							mPackageName = mContext.getPackageName();
							if (mEnabledForAllApps) {

								mDisabledApps = mPrefs.getString(
										"disable_for_apps", "");
								if (!mDisabledApps.isEmpty() && mEnabledModule) {
									mEnabledModule = mDisabledApps
											.contains(mPackageName) ? false
											: true;

								}
							} else {
								mEnabledApps = mPrefs.getString(
										"enable_for_apps", "");
								if (!mEnabledApps.isEmpty() && mEnabledModule) {
									mEnabledModule = mEnabledApps
											.contains(mPackageName) ? true
											: false;

								}
							}
						}

					}

					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						if (mEnabledModule) {
							param.setResult(mInternalSDCard);
						}
					}

				});
		Class<?> mContextClass = XposedHelpers.findClass(
				"android.app.ContextImpl", null);
		XposedHelpers.findAndHookMethod(mContextClass, "getExternalFilesDir",
				String.class, new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param)
							throws Throwable {
						mPrefs.reload();
						mEnabledModule = mPrefs.getBoolean(
								"custom_internal_sd", true);
						mEnabledForAllApps = mPrefs.getBoolean(
								"enable_for_all_apps", true);
						String arg = (String) param.args[0];
						if (arg != null) {
							mEnabledModule = false;
						} else {
							@SuppressLint("SdCardPath")
							String internalSD = mPrefs.getString(
									"internal_sd_path", "/sdcard");
							mContext = AndroidAppHelper.currentApplication();
							if (mContext == null) {
								mEnabledModule = false;
							} else {
								String appFiles = internalSD + "/Android/data/"
										+ AndroidAppHelper.currentPackageName()
										+ "/files";
								mAppFiles = new File(appFiles);
								mPackageName = mContext.getPackageName();
								if (mEnabledForAllApps) {

									mDisabledApps = mPrefs.getString(
											"disable_for_apps", "");
									if (!mDisabledApps.isEmpty()
											&& mEnabledModule) {
										mEnabledModule = mDisabledApps
												.contains(mPackageName) ? false
												: true;

									}
								} else {
									mEnabledApps = mPrefs.getString(
											"enable_for_apps", "");
									if (!mEnabledApps.isEmpty()
											&& mEnabledModule) {
										mEnabledModule = mEnabledApps
												.contains(mPackageName) ? true
												: false;

									}
								}
							}
						}

					}

					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						if (mEnabledModule) {
							param.setResult(mAppFiles);
						}
					}

				});
		XposedHelpers.findAndHookMethod(mContextClass, "getObbDir",
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param)
							throws Throwable {
						mPrefs.reload();
						mEnabledModule = mPrefs.getBoolean(
								"custom_internal_sd", true);
						mEnabledForAllApps = mPrefs.getBoolean(
								"enable_for_all_apps", true);
						@SuppressLint("SdCardPath")
						String internalSD = mPrefs.getString(
								"internal_sd_path", "/sdcard");
						mContext = AndroidAppHelper.currentApplication();
						if (mContext == null) {
							mEnabledModule = false;
						} else {
							String obbDir = internalSD + "/Android/obb/"
									+ AndroidAppHelper.currentPackageName()
									+ "/";
							mObbDir = new File(obbDir);
							mEnabledApps = mPrefs.getString("enable_for_apps",
									"");
							mPackageName = mContext.getPackageName();
							if (mEnabledForAllApps) {

								mDisabledApps = mPrefs.getString(
										"disable_for_apps", "");
								if (!mDisabledApps.isEmpty() && mEnabledModule) {
									mEnabledModule = mDisabledApps
											.contains(mPackageName) ? false
											: true;

								}
							} else {
								mEnabledApps = mPrefs.getString(
										"enable_for_apps", "");
								if (!mEnabledApps.isEmpty() && mEnabledModule) {
									mEnabledModule = mEnabledApps
											.contains(mPackageName) ? true
											: false;

								}
							}
						}

					}

					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						if (mEnabledModule) {
							param.setResult(mObbDir);
						}
					}

				});
		XposedHelpers.findAndHookMethod(Environment.class,
				"getExternalStoragePublicDirectory", String.class,
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param)
							throws Throwable {
						mPrefs.reload();
						String dirType = (String) param.args[0];
						boolean isDownloadDir = Environment.DIRECTORY_DOWNLOADS
								.equals(dirType) ? true : false;
						mEnabledModule = mPrefs.getBoolean(
								"custom_internal_sd", true);
						boolean changeDownloadPath = mPrefs.getBoolean(
								"change_download_path", true);
						mEnabledForAllApps = mPrefs.getBoolean(
								"enable_for_all_apps", true);
						@SuppressLint("SdCardPath")
						String internalSD = mPrefs.getString(
								"internal_sd_path", "/sdcard");
						mContext = AndroidAppHelper.currentApplication();
						if (mContext == null || !isDownloadDir
								|| !changeDownloadPath) {
							mEnabledModule = false;
						} else {
							String downloadDir = internalSD + "/Download/";
							mDownloadDir = new File(downloadDir);
							mPackageName = mContext.getPackageName();
							if (mEnabledForAllApps) {

								mDisabledApps = mPrefs.getString(
										"disable_for_apps", "");
								if (!mDisabledApps.isEmpty() && mEnabledModule) {
									mEnabledModule = mDisabledApps
											.contains(mPackageName) ? false
											: true;

								}
							} else {
								mEnabledApps = mPrefs.getString(
										"enable_for_apps", "");
								if (!mEnabledApps.isEmpty() && mEnabledModule) {
									mEnabledModule = mEnabledApps
											.contains(mPackageName) ? true
											: false;

								}
							}
						}

					}

					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						if (mEnabledModule) {
							param.setResult(mDownloadDir);
						}
					}

				});
		Class<?> packageManagerService = XposedHelpers.findClass(
				"com.android.server.pm.PackageManagerService", null);
		XposedHelpers.findAndHookMethod(packageManagerService,
				"readPermission", "org.xmlpull.v1.XmlPullParser",
				"java.lang.String", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						mPrefs.reload();

						String permission = (String) param.args[1];
						boolean sdCardFullAccess = mPrefs.getBoolean(
								"sdcard_full_access", true);
						if (sdCardFullAccess
								&& (permission
										.equals("android.permission.WRITE_EXTERNAL_STORAGE") || permission
										.equals("android.permission.ACCESS_ALL_EXTERNAL_STORAGE"))) {
							Class<?> process = XposedHelpers.findClass(
									"android.os.Process", null);
							int gid = (Integer) XposedHelpers.callStaticMethod(
									process, "getGidForName", "media_rw");
							Object mSettings = XposedHelpers.getObjectField(
									param.thisObject, "mSettings");
							Object mPermissions = XposedHelpers.getObjectField(
									mSettings, "mPermissions");
							Object bp = XposedHelpers.callMethod(mPermissions,
									"get", permission);
							int[] bpGids = (int[]) XposedHelpers
									.getObjectField(bp, "gids");
							XposedHelpers.setObjectField(bp, "gids",
									appendInt(bpGids, gid));
						}
					}
				});
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
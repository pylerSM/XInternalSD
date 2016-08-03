package com.pyler.xinternalsd;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Environment;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
    public XC_MethodHook getObbDirsHook;
    public XC_MethodHook externalSdCardAccessHook; // 4.4 - 5.0
    public XC_MethodHook externalSdCardAccessHook2; // 6.0 and up
    boolean detectedSdPath = false;

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
                changeDirsPath(param);
            }
        };

        getObbDirsHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param)
                    throws Throwable {
                changeDirsPath(param);
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
                if (Common.PERM_WRITE_EXTERNAL_STORAGE
                        .equals(permission)
                        || Common.PERM_ACCESS_ALL_EXTERNAL_STORAGE
                        .equals(permission)) {
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


        externalSdCardAccessHook2 = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param)
                    throws Throwable {
                prefs.reload();
                boolean externalSdCardFullAccess = prefs.getBoolean(
                        "external_sdcard_full_access", true);
                if (!externalSdCardFullAccess) {
                    return;
                }
                Object extras = XposedHelpers.getObjectField(param.args[0], "mExtras");
                Object ps = XposedHelpers.callMethod(extras, "getPermissionsState");
                Object settings = XposedHelpers.getObjectField(param.thisObject, "mSettings");
                Object permissions = XposedHelpers.getObjectField(settings, "mPermissions");
                boolean hasPermission = (boolean) XposedHelpers.callMethod(ps, "hasInstallPermission", Common.PERM_WRITE_MEDIA_STORAGE);
                if (!hasPermission) {
                    Object permWriteMediaStorage = XposedHelpers.callMethod(permissions, "get",
                            Common.PERM_WRITE_MEDIA_STORAGE);
                    XposedHelpers.callMethod(ps, "grantInstallPermission", permWriteMediaStorage);
                }

            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

        if ("android".equals(lpparam.packageName)
                && "android".equals(lpparam.processName)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
                        "com.android.server.pm.PackageManagerService",
                        lpparam.classLoader), "grantPermissionsLPw",
                        Common.CLASS_PACKAGE_PARSER_PACKAGE, boolean.class, String.class, externalSdCardAccessHook2);
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP || Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1) {
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

        if (!detectedSdPath) {
            try {
                File internalSdPath = Environment.getExternalStorageDirectory();
                internalSd = internalSdPath.getPath();
                detectedSdPath = true;
            } catch (Exception e) {
                // nothing
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
            XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
                    "android.app.ContextImpl", lpparam.classLoader),
                    "getObbDirs", getObbDirsHook);
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
        if (oldDirPath == null) {
            return;
        }
        String customInternalSd = getCustomInternalSd();
        if (customInternalSd.isEmpty()) {
            return;
        }
        String internalSd = getInternalSd();
        if (internalSd.isEmpty()) {
            return;
        }

        String dir = Common.appendFileSeparator(oldDirPath.getPath());
        String newDir = dir.replaceFirst(internalSd,
                customInternalSd);
        File newDirPath = new File(newDir);
        if (!newDirPath.exists()) {
            newDirPath.mkdirs();
        }
        param.setResult(newDirPath);
    }

    public void changeDirsPath(MethodHookParam param) {
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

        String internalSd = getInternalSd();
        if (internalSd.isEmpty()) {
            return;
        }

        String dir = Common.appendFileSeparator(oldDirPaths[0].getPath());
        String newDir = dir.replaceFirst(internalSd, customInternalSd);
        File newDirPath = new File(newDir);

        if (!newDirPaths.contains(newDirPath)) {
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
        customInternalSd = Common.appendFileSeparator(customInternalSd);
        return customInternalSd;
    }

    public String getInternalSd() {
        internalSd = Common.appendFileSeparator(internalSd);
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
            if (Arrays.asList(Common.MTP_APPS).contains(appInfo.packageName)) {
                return false;
            }
        }
        return true;
    }

    public int[] appendInt(int[] cur, int val) {
        if (cur == null) {
            return new int[]{val};
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

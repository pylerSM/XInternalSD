package com.pyler.xinternalsd;


import java.io.File;

public class Common {

    public static final String CLASS_PACKAGE_PARSER_PACKAGE = "android.content.pm.PackageParser.Package";
    public static final String[] MTP_APPS = {"com.android.MtpApplication", "com.samsung.android.MtpApplication"};
    public static final String PERM_WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    public static final String PERM_ACCESS_ALL_EXTERNAL_STORAGE = "android.permission.ACCESS_ALL_EXTERNAL_STORAGE";
    public static final String PERM_WRITE_MEDIA_STORAGE = "android.permission.WRITE_MEDIA_STORAGE";

    private Common() {
    }

    public static String appendFileSeparator(String path) {
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        return path;
    }
}

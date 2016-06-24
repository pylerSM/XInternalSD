package com.pyler.xinternalsd;


import java.io.File;

public class Common {

    public static final String[] MTP_APPS = {"com.android.MtpApplication", "com.samsung.android.MtpApplication"};

    private Common() {
    }

    public static String appendFileSeparator(String path) {
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        return path;
    }
}

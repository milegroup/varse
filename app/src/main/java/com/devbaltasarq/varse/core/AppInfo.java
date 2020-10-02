// Varse (c) 2019/20 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.core;


/** The version information for this app. */
public class AppInfo {
    public static final String NAME = "VARSE";
    public static final String VERSION = "v1.6.0 20200901";
    public static final String AUTHOR = "MILE Group";
    public static final String EDITION = "Selfsteem";
    public static final String APP_EMAIL = "varse.milegroup@gmx.com";

    public static String asShortString()
    {
        return NAME + ' ' + VERSION;
    }

    public static String asString()
    {
        return NAME + ' ' + VERSION
                + " \"" + EDITION + "\" - " + AUTHOR;
    }
}

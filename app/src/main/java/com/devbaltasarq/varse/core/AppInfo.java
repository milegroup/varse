// Varse (c) 2019/20 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.core;


/** The version information for this app. */
public class AppInfo {
    public static final String NAME = "VARSE";
    public static final String VERSION = "v1.8.5 20231019";
    public static final String AUTHOR = "MILE Group";
    public static final String EDITION = "Raging Phoenix";
    public static final String APP_EMAIL = "varse.mile@gmail.com";

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

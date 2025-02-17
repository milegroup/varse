// Varse (c) 2019/20 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.core;


/** The version information for this app. */
public class AppInfo {
    public static final String NAME = "VARSE";
    public static final String VERSION = "v1.9.0 20250214";
    public static final String AUTHOR = "MILE Group";
    public static final String EDITION = "Hard crab";

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

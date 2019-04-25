package com.devbaltasarq.varse.core;

/** The version information for this app. */
public class AppInfo {
    public static final String NAME = "VARSE";
    public static final String VERSION = "v1.5.4 20190402";
    public static final String AUTHOR = "MILE Group";
    public static final String EDITION = "Tantric";

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

package com.devbaltasarq.varse.core;

/** The version information for this app. */
public class AppInfo {
    public static final String NAME = "VARSE";
    public static final String VERSION = "v1.4 20181018";
    public static final String AUTHOR = "MILE Group";

    public static String asShortString()
    {
        return NAME + ' ' + VERSION;
    }

    public static String asString()
    {
        return NAME + ' ' + VERSION + " - " + AUTHOR;
    }
}

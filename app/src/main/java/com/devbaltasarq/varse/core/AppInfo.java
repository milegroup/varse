// Varse (c) 2019/25 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.core;


/** The version information for this app. */
public class AppInfo {
    public static final String NAME = "VARSE";
    public static final String VERSION = "v1.9.3 20260711";
    public static final String AUTHOR = "MILE Group";
    public static final String EDITION = "Pez Mapache";

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

package com.devbaltasarq.varse.core.experiment;

import java.io.File;
import java.util.HashMap;

/** Tools related to multimedia MIME types. */
public final class MimeTools {
    /** Prepares the class to work. */
    private static void init()
    {
        if ( extensionForMime != null ) {
            return;
        }

        extensionForMime = new HashMap<>();
        mimeForExtension = new HashMap<>();

        // Videos
        add( "video/3gpp", "3gpp" );
        add( "video/3gpp", "3gp" );
        add( "video/3gpp", "3g2" );
        add( "video/dl", "dl" );
        add( "video/dv", "dif" );
        add( "video/dv", "dv" );
        add( "video/fli", "fli" );
        add( "video/m4v", "m4v" );
        add( "video/mpeg", "mpeg" );
        add( "video/mpeg", "mpg" );
        add( "video/mpeg", "mpe" );
        add( "video/mp4", "mp4" );
        add( "video/mpeg", "vob" );
        add( "video/ogg", "ogg" );
        add( "video/quicktime", "qt" );
        add( "video/quicktime", "mov" );
        add( "video/x-ms-asf", "asf" );
        add( "video/x-ms-asf", "asx" );
        add( "video/x-ms-wm", "wm" );
        add( "video/x-ms-wmv", "wmv" );
        add( "video/x-ms-wmx", "wmx" );
        add( "video/x-ms-wvx", "wvx" );
        add( "video/x-msvideo", "avi" );
        add( "video/x-sgi-movie", "movie" );

        // Images
        add( "image/bmp", "bmp" );
        add( "image/gif", "gif" );
        add( "image/ico", "cur" );
        add( "image/ico", "ico" );
        add( "image/ief", "ief" );
        add( "image/jpeg", "jpeg" );
        add( "image/jpeg", "jpg" );
        add( "image/jpeg", "jpe" );
        add( "image/pcx", "pcx" );
        add( "image/png", "png" );
        add( "image/svg+xml", "svg" );
        add( "image/svg+xml", "svgz" );
        add( "image/tiff", "tiff" );
        add( "image/tiff", "tif" );
        add( "image/vnd.djvu", "djvu" );
        add( "image/vnd.djvu", "djv" );
        add( "image/vnd.wap.wbmp", "wbmp" );
        add( "image/x-cmu-raster", "ras" );
        add( "image/x-coreldraw", "cdr" );
        add( "image/x-coreldrawpattern", "pat" );
        add( "image/x-coreldrawtemplate", "cdt" );
        add( "image/x-corelphotopaint", "cpt" );
        add( "image/x-icon", "ico" );
        add( "image/x-jg", "art" );
        add( "image/x-jng", "jng" );
        add( "image/x-ms-bmp", "bmp" );
        add( "image/x-photoshop", "psd" );
        add( "image/x-portable-anymap", "pnm" );
        add( "image/x-portable-bitmap", "pbm" );
        add( "image/x-portable-graymap", "pgm" );
        add( "image/x-portable-pixmap", "ppm" );
        add( "image/x-rgb", "rgb" );
        add( "image/x-xbitmap", "xbm" );
        add( "image/x-xpixmap", "xpm" );
        add( "image/x-xwindowdump", "xwd" );
    }

    private static void add(String k, String v)
    {
        init();
        extensionForMime.put( k, v );
        mimeForExtension.put( v, k );
    }

    public static String getMimeFor(File f)
    {
        init();
        final String toret = mimeForExtension.get( extractFileExt( f ) );

        if ( toret == null ) {
            throw new IllegalArgumentException( "unknown mime type for: " + f.toString() );
        }

        return toret;
    }

    public static String getExtFor(String mime)
    {
        init();
        final String toret = extensionForMime.get( mime );

        if ( toret == null ) {
            throw new IllegalArgumentException( "unknown mime: " + mime );
        }

        return toret;
    }

    public static boolean isPicture(File f)
    {
        init();
        return ( getMimeFor( f ).contains( "image" ) );
    }

    public static boolean isVideo(File f)
    {
        return ( getMimeFor( f ).contains( "video" ) );
    }

    public static String extractFileExt(File f)
    {
        String toret = "";

        if ( f != null ) {
            final String name = f.getName();
            int pos = name.lastIndexOf( '.' );

            if ( pos >= 0 ) {
                toret = name.substring( pos + 1 );
            }
        }

        return toret.toLowerCase();
    }

    private static HashMap<String, String> extensionForMime;
    private static HashMap<String, String> mimeForExtension;
}

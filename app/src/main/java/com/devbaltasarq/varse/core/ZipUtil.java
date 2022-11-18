package com.devbaltasarq.varse.core;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Zip management utils. */
public final class ZipUtil {
    private static final String LOG_TAG_ZIP = ZipUtil.class.getSimpleName() + ".zip";
    private static final String LOG_TAG_UNZIP = ZipUtil.class.getSimpleName() + "ZipUtil.unzip";
    private static final int MAX_BUFFER = 40960;
    private static final byte[] BUFFER = new byte[ MAX_BUFFER ];

    /** Zips a set of files in a zip archive.
      *
      * @param files the set of files, as an array.
      * @param zipFileName the destination file.
      * @throws IOException if something goes wrong with the file system.
      */
    public static void zip(File[] files, File zipFileName) throws IOException
    {
        try {
            Log.i( LOG_TAG_ZIP, "starting zipping to: " + zipFileName.getAbsolutePath() );
            BufferedInputStream origin;
            FileOutputStream dest = new FileOutputStream( zipFileName );
            ZipOutputStream out = new ZipOutputStream( new BufferedOutputStream( dest ) );

            for (File file: files) {
                Log.i( LOG_TAG_ZIP, "Adding: " + file );

                FileInputStream fi = new FileInputStream( file );
                origin = new BufferedInputStream( fi, MAX_BUFFER );

                ZipEntry entry = new ZipEntry( file.getName() );
                out.putNextEntry( entry );
                int count;

                while ( ( count = origin.read( BUFFER, 0, MAX_BUFFER ) ) != -1 ) {
                    out.write( BUFFER, 0, count );
                }

                origin.close();
            }

            out.close();
            Log.i( LOG_TAG_ZIP, "finished zipping" );
        } catch (IOException e) {
            final String MSG_ERROR = "error zipping: " + e.getMessage();

            Log.e( LOG_TAG_ZIP, MSG_ERROR );
            throw new IOException( MSG_ERROR );
        }
    }

    /** Unzips a file to a directory.
     *
     * @param zipFile the zip file to unzip.
     * @param targetDir the target directory.
     * @throws IOException if something goes wrong with the file system.
     */
    public static void unzip(File zipFile, File targetDir) throws IOException
    {
        Log.i( LOG_TAG_UNZIP, "starting unzipping of: " + zipFile.getAbsolutePath() );

        FileInputStream fin = new FileInputStream( zipFile );
        unzip( fin, targetDir );
    }

    /** Unzips a file to a directory.
     *
     * @param zipFileIn an input stream from the zip file to unzip.
     * @param targetDir the target directory.
     * @throws IOException if something goes wrong with the file system.
     */
    public static void unzip(InputStream zipFileIn, File targetDir) throws IOException
    {
        final String EXPECTED_TARGET_PATH = targetDir.getCanonicalPath() + File.separator;
        Log.i( LOG_TAG_UNZIP, "starting unzipping from stream" );

        targetDir.mkdirs();

        if ( targetDir.exists() ) {
            ZipInputStream zin = new ZipInputStream( zipFileIn );
            ZipEntry ze = null;

            while ( ( ze = zin.getNextEntry() ) != null ) {
                // Create dir if required while unzipping
                if ( ze.isDirectory() ) {
                    if ( !new File( ze.getName() ).mkdirs() ) {
                        final String MSG_ERROR = "unzip: could not create directory: " + ze.getName();

                        Log.e( LOG_TAG_UNZIP, MSG_ERROR );
                        throw new IOException( MSG_ERROR );
                    }
                } else {
                    // Chk CVE Zip traversal
                    final File OUT_FILE = new File( targetDir, ze.getName() );
                    final String OUT_PATH = OUT_FILE.getCanonicalPath();

                    if ( !OUT_PATH.startsWith( EXPECTED_TARGET_PATH ) ) {
                        final String MSG_ERROR = "unzip: illegal name: " + OUT_FILE;

                        Log.e( LOG_TAG_UNZIP, MSG_ERROR );
                        throw new IOException( MSG_ERROR );
                    }

                    // Unzip
                    final FileOutputStream FOUT = new FileOutputStream( OUT_FILE );
                    int count;

                    while ( ( count = zin.read( BUFFER, 0, MAX_BUFFER ) ) != -1 ) {
                        FOUT.write( BUFFER, 0, count );
                    }

                    zin.closeEntry();
                    FOUT.close();
                }
            }

            zin.close();
            Log.i( LOG_TAG_UNZIP, "finished unzipping." );
        } else {
            final String MSG_ERROR = "could not create directory: " + targetDir.getAbsolutePath();

            Log.e( LOG_TAG_UNZIP, MSG_ERROR );
            throw new IOException( MSG_ERROR );
        }

        return;
    }
}

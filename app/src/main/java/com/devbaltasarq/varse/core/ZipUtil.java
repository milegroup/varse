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
    private static final String LogTagZip = ZipUtil.class.getSimpleName() + ".zip";
    private static final String LogTagUnzip = ZipUtil.class.getSimpleName() + "ZipUtil.unzip";
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
            Log.i( LogTagZip, "starting zipping to: " + zipFileName.getAbsolutePath() );
            BufferedInputStream origin;
            FileOutputStream dest = new FileOutputStream( zipFileName );
            ZipOutputStream out = new ZipOutputStream( new BufferedOutputStream( dest ) );

            for (File file: files) {
                Log.i( LogTagZip, "Adding: " + file );

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
            Log.i( LogTagZip, "finished zipping" );
        } catch (IOException e) {
            final String msg = "error zipping: " + e.getMessage();

            Log.e( LogTagZip, msg );
            throw new IOException( msg );
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
        Log.i(LogTagUnzip, "starting unzipping of: " + zipFile.getAbsolutePath() );

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
        Log.i(LogTagUnzip, "starting unzipping from stream" );

        targetDir.mkdirs();

        if ( targetDir.exists() ) {
            try {
                ZipInputStream zin = new ZipInputStream( zipFileIn );
                ZipEntry ze = null;

                while ( ( ze = zin.getNextEntry() ) != null ) {
                    // Create dir if required while unzipping
                    if ( ze.isDirectory() ) {
                        if ( !new File( ze.getName() ).mkdirs() ) {
                            final String msg = "could not create directory: " + ze.getName();

                            Log.e( LogTagUnzip, msg );
                            throw new IOException( msg );
                        }
                    } else {
                        FileOutputStream fout = new FileOutputStream(
                                                    new File( targetDir, ze.getName() ) );
                        int count;

                        while ( ( count = zin.read( BUFFER, 0, MAX_BUFFER ) ) != -1 ) {
                            fout.write( BUFFER, 0, count );
                        }

                        zin.closeEntry();
                        fout.close();
                    }
                }

                zin.close();
                Log.i( LogTagUnzip, "finished unzipping." );
            } catch (IOException exc) {
                final String msg = "error unzipping: " + exc.getMessage();

                Log.e( LogTagUnzip, msg );
                throw new IOException( msg );
            }
        } else {
            final String msg = "could not create directory: " + targetDir.getAbsolutePath();

            Log.e( LogTagUnzip, msg );
            throw new IOException( msg );
        }

        return;
    }
}

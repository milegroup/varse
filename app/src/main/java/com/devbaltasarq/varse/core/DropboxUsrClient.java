// VARSE (c) 2019 Baltasar for MILEGroup MIT License <jbgarcia@uvigo.es>

package com.devbaltasarq.varse.core;

import android.app.Activity;
import android.util.Log;
import android.util.Pair;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.ofmcache.EntitiesCache;
import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This class automates access to the Dropbox API.
 *
 * Each user has its own directory, direct subdirectory of root.
 * So, for example, if John Doe has registered the email john@doe.com,
 * then he will have a complete path of /john@doe.com/, which
 * DropBox will transform in /application/varse/john@doe.com.
 */
public class DropboxUsrClient {
    public static final String TAG = DropboxUsrClient.class.getSimpleName();
    private static final String RES_DIR_NAME = Ofm.DIR_RES;

    private enum ListMode{ NO_DIRS, INCLUDE_DIRS }

    /** Creates a new Dropbox client for the user of the given email.
      * @param OWNER The activity this client is going to be used in.
      * @param USR_EMAIL The user id (the email).
      */
    public DropboxUsrClient(final Activity OWNER, final String USR_EMAIL)
    {
        this.USR_EMAIL = USR_EMAIL;

        // Create the access credentials and the client
        String appPackage = OWNER.getPackageName();

        DbxRequestConfig config =
                DbxRequestConfig.newBuilder( appPackage ).build();

        this.DBOX_CLIENT = new DbxClientV2( config,
                                            OWNER.getString( R.string.dropbox_token ) );
    }

    /** Lists all files in the user directory.
     * @return an array of file names.
     * @throws DbxException if something goes wrong, connecting to Dropbox.
     */
    public String[] listUsrFiles() throws DbxException
    {
        final List<String> TORET = new ArrayList<>( Arrays.asList(
                                            listFilesInDir( this.buildBaseDirUsr(), ListMode.NO_DIRS ) ) );

        sortFiles( TORET );
        return TORET.toArray( new String[ 0 ] );
    }

    /** Lists all user's resource file.
      * @return A list of pairs (subdir, array of files inside it).
      * @throws DbxException if comms go wrong with remote.
      */
    public List<Pair<String, String[]>> listUsrResFiles() throws DbxException
    {
        final List<Pair<String, String[]>> TORET = new ArrayList<>();
        final String[] SUBDIRS = this.listFilesInDir( this.buildResDirUsr(), ListMode.INCLUDE_DIRS );

        // Run all over subdirs
        for(String subDir: SUBDIRS) {
            final String[] RES_FILES = this.listFilesInDir( this.buildResDirUsr() + subDir, ListMode.NO_DIRS );

            TORET.add( Pair.create( subDir, RES_FILES ) );
        }

        return TORET;
    }


    /** Gets all the files in a given remote directory.
     * @param dir The remote directory to list files of.
     * @return An array of String[] with the name (without paths) of found files.
     * @throws DbxException if comms with remote go wrong.
     */
    private String[] listFilesInDir(String dir, ListMode listMode) throws DbxException
    {
        final List<String> TORET = new ArrayList<>();
        ListFolderResult folderResult = DBOX_CLIENT.files().listFolder( dir );

        do {
            for(Metadata metadata: folderResult.getEntries()) {
                if ( metadata instanceof FileMetadata
                  || ( listMode == ListMode.INCLUDE_DIRS
                    && metadata instanceof FolderMetadata ) )
                {
                    TORET.add( metadata.getName() );
                }
            }

            folderResult = DBOX_CLIENT.files().listFolderContinue( folderResult.getCursor() );
        } while( folderResult.getHasMore() );

        return TORET.toArray( new String[ 0 ] );
    }

    /** Sort list of files in so experiments go always first,
      * and only then, the results.
      * The sorting is really simple:
      * - put experiments at the beginning,
      * - put results at the end.
      */
    private static void sortFiles(List<String> files)
    {
        final String EXPERIMENT_EXTENSION = EntitiesCache.getFileExtFor( Persistent.TypeId.Experiment );
        final String RESULT_EXTENSION = EntitiesCache.getFileExtFor( Persistent.TypeId.Result );

        final ArrayList<String> EXPERIMENT_FILES = new ArrayList<>( files.size() );
        final ArrayList<String> RESULT_FILES = new ArrayList<>( files.size() );
        final ArrayList<String> OTHER_FILES = new ArrayList<>( files.size() );

        // Classify files by extension
        for(String fileName: files) {
            if ( fileName.endsWith( EXPERIMENT_EXTENSION ) ) {
                EXPERIMENT_FILES.add( fileName );
            }
            else
            if ( fileName.endsWith( RESULT_EXTENSION ) ) {
                RESULT_FILES.add( fileName );
            } else {
                OTHER_FILES.add( fileName );
            }
        }

        // Put them back in sorted order
        files.clear();
        files.addAll( EXPERIMENT_FILES );
        files.addAll( RESULT_FILES );
        files.addAll( OTHER_FILES );
    }

    /** Download a file from dropbox's user directory.
      * @param subDir the subdirectory inside the res dir in which the file exists.
      * @param fileName a res file without path, which should exist.
      * @param OFM the data store to send the file to.
      * @throws DbxException when dropbox comms go wrong.
      */
    public void downloadResFileTo(String subDir, String fileName, final Ofm OFM) throws DbxException
    {
        final String FILE_PATH = this.buildPathForResFile( subDir, fileName );

        try {
            OFM.copyResFileAs( subDir,
                               this.downloadFileTo( fileName, FILE_PATH ),
                               fileName );
        } catch(IOException exc)
        {
            Log.e( TAG, "error copying to orm the downloaded res file:" + exc.getMessage() );
        }
    }

    /** Download a file from dropbox's user directory.
      * @param fileName a data file without path, which should exist.
      * @param OFM the data store to send the file to.
      * @throws DbxException when dropbox comms go wrong.
      */
    public void downloadDataFileTo(String fileName, final Ofm OFM) throws DbxException
    {
        final String FILE_PATH = this.buildPathForDataFile( fileName );

        try {
            OFM.copyDataFileAs( this.downloadFileTo( fileName, FILE_PATH ), fileName );
        } catch(IOException exc)
        {
            Log.e( TAG, "error copying to orm the downloaded data file:" + exc.getMessage() );
        }
    }

    /** Downloads a given file into a local Ofm.
      * @param fileName the name of the file, and only its name (in order to save in local).
      * @param FILE_PATH the path of the file, which should exist.
      * @throws DbxException when dropbox comms go wrong.
      */
    private File downloadFileTo(String fileName, String FILE_PATH) throws DbxException
    {
        DbxDownloader<FileMetadata> downloader = DBOX_CLIENT.files().download( FILE_PATH );
        File f;
        OutputStream out = null;

        try {
            // Download file to a temporary
            f = File.createTempFile( TAG, fileName );
            out = new FileOutputStream( f );

            downloader.download( out );
        } catch (IOException exc) {
            throw new DbxException( exc.getMessage() );
        } finally {
            if ( out != null ) {
                try {
                    out.close();
                } catch(IOException e) {
                    Log.e( TAG, "error closing stream downloading: " + e.getMessage() );
                }
            }
        }

        return f;
    }

    /** Uploads a resource file.
      * @param f the file to upload, containing a resource (media...)
      * @throws DbxException if the comms go wrong.
      */
    public void uploadResFile(String subDir, File f) throws DbxException
    {
        this.uploadFile( f, this.buildPathForResFile( subDir, f.getName() ) );
    }

    /** Uploads a data file.
      * @param f the file to upload, containing data (experiment, result...)
      * @throws DbxException if the comms go wrong
      */
    public void uploadDataFile(File f) throws DbxException
    {
        this.uploadFile( f, this.buildPathForDataFile( f.getName() ) );
    }

    /** Uploads a file.
     * @param fin The file to upload.
     * @param toPath the absolute path of the file in the cloud.
     * @throws DbxException when something goes wrong uploading.
     */
    private void uploadFile(File fin, String toPath) throws DbxException
    {
        // Upload file to Dropbox
        try (InputStream in = new FileInputStream( fin ) ) {
            this.DBOX_CLIENT.files().uploadBuilder( toPath ).uploadAndFinish( in );
        } catch(IOException exc)
        {
            throw new DbxException( exc.getMessage() );
        }
    }

    /** @return the complete path to the data file, given the user id in dropbox. */
    private String buildPathForDataFile(String fileName)
    {
        return this.buildBaseDirUsr() + fileName;
    }

    /** @return the complete path to the resource file, given the user id in dropbox. */
    private String buildPathForResFile(String subDir, String fileName)
    {
        return this.buildResDirUsr()
                    + subDir
                    + DropboxDirectorySeparator
                    + fileName;
    }

    /** @return the base directory for this user, with an ending slash. */
    private String buildResDirUsr()
    {
        return DropboxDirectorySeparator
                + this.USR_EMAIL
                + DropboxDirectorySeparator
                + RES_DIR_NAME
                + DropboxDirectorySeparator;
    }

    /** @return the base directory for this user, with an ending slash. */
    private String buildBaseDirUsr()
    {
        return DropboxDirectorySeparator
                + this.USR_EMAIL
                + DropboxDirectorySeparator;
    }

    private final String USR_EMAIL;
    private final DbxClientV2 DBOX_CLIENT;

    private static final String DropboxDirectorySeparator = "/";
}

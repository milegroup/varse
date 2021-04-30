package com.devbaltasarq.varse.core;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.Log;
import android.util.Pair;

import com.devbaltasarq.varse.core.experiment.Tag;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;


/** Relates the database of JSON files to objects. */
public final class Orm {
    private static final String LOG_TAG = Orm.class.getSimpleName();

    private static final String FIELD_ID = Id.FIELD;
    public static final String FIELD_EXPERIMENT_ID = "experiment_id";
    public static final String FIELD_EXPERIMENT = "experiment";
    public static final String FIELD_USER_ID = "user_id";
    public static final String FIELD_USER = "user";
    public static final String FIELD_DATE = "date";
    public static final String FIELD_ELAPSED_TIME = "elapsed_time";
    public static final String FIELD_HEART_BEAT_AT = "heart_beat_at";
    public static final String FIELD_EVENTS = "events";
    public static final String FIELD_EVENT_TYPE = "event_type";
    public static final String FIELD_EVENT_HEART_BEAT = "event_heart_beat";
    public static final String FIELD_EVENT_ACTIVITY_CHANGE = "event_activity_change";
    public static final String FIELD_RANDOM = "random";
    public static final String FIELD_TAG = Tag.FIELD;
    public static final String FIELD_TIME = Duration.FIELD;
    public static final String FIELD_FILE = "file";
    public static final String FIELD_GROUPS = "groups";
    public static final String FIELD_TYPE_ID = Persistent.TypeId.FIELD;
    public static final String FIELD_ACTIVITIES = "activities";

    private static final String SETTINGS_FILE_NAME = "settings.json";
    private static final String DIR_DB = "db";
    static final String DIR_RES = "res";
    private static final String DIR_MEDIA_PREFIX = "media";
    private static final String FILE_NAME_PART_SEPARATOR = "-";
    private static final String FIELD_EXT = "ext";
    public static final String FIELD_NAME = "name";
    private static final String REGULAR_FILE_FORMAT =
            "id" + FILE_NAME_PART_SEPARATOR + "$" + FIELD_ID + ".$" + FIELD_EXT;
    private static final String RESULT_FILE_FORMAT =
            "id" + FILE_NAME_PART_SEPARATOR + "$" + FIELD_ID
            + FILE_NAME_PART_SEPARATOR + FIELD_USER
            + FILE_NAME_PART_SEPARATOR + "$" + FIELD_USER
            + FILE_NAME_PART_SEPARATOR + FIELD_EXPERIMENT
            + FILE_NAME_PART_SEPARATOR + "$" + FIELD_EXPERIMENT
            + FILE_NAME_PART_SEPARATOR + FIELD_USER_ID
            + FILE_NAME_PART_SEPARATOR + "$" + FIELD_USER_ID
            + FILE_NAME_PART_SEPARATOR + FIELD_EXPERIMENT_ID
            + FILE_NAME_PART_SEPARATOR + "$" + FIELD_EXPERIMENT_ID
            + ".$" + FIELD_EXT;

    /** Prepares the ORM to operate. */
    private Orm(Context context)
    {
        this.context = context;
        DIR_DOWNLOADS = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOWNLOADS );
        this.reset();
    }

    /** Forces a reset of the ORM, so the contents of the store is reflected
      *  in the internal data structures.
      */
    public final void reset()
    {
        Log.i(LOG_TAG, "Preparing store..." );

        this.dirFiles = this.context.getFilesDir();

        this.createDirectories();
        this.removeCache();
        this.createCaches();

        Log.i(LOG_TAG, "Store ready at: " + this.dirDb.getAbsolutePath() );
        Log.i(LOG_TAG, "    #user files: " + this.filesUser.size() );
        Log.i(LOG_TAG, "    #experiment files: " + this.filesExperiment.size() );
        Log.i(LOG_TAG, "    #result files: " + this.filesResult.size() );
    }

    /** Creates the needed directories, if do not exist. */
    private void createDirectories()
    {
        this.dirDb = this.context.getDir( DIR_DB,  Context.MODE_PRIVATE );
        this.dirRes = this.context.getDir( DIR_RES,  Context.MODE_PRIVATE );
        this.dirTmp = this.context.getCacheDir();
    }

    /** Removes all files in cache. */
    public void removeCache()
    {
        if ( this.dirTmp != null ) {
            removeTreeAt( this.dirTmp );
        }

        this.dirTmp = this.context.getCacheDir();
    }

    /** Create the cache of files. */
    private void createCaches()
    {
        this.filesUser = new HashSet<>();
        this.filesExperiment = new HashSet<>();
        this.filesResult = new HashSet<>();
        this.resultsPerExperiment = new HashMap<>();

        for (final File F: this.dirDb.listFiles()) {
            this.updateCaches( F );
        }

        return;
    }

    /** Updates all file caches.
      * @param p The persistent object to update.
      * @param f The corresponding file to the object, calculated if null.
      */
    private void updateCaches(Persistent p, File f)
    {
        if ( f == null ) {
            f = new File( this.dirDb, this.getFileNameFor( p ) );
        }

        this.addToGeneralCache( f );

        if ( p.getTypeId() == Persistent.TypeId.Result ) {
            this.addToResultsCache( f, ( (Result) p ).getExperiment().getId() );
        }
    }

    /** Updates all caches.
      * @param f The file to include in all caches.
      */
    private void updateCaches(File f)
    {
        final Persistent.TypeId TYPE_ID = getTypeIdForExt( f );

        this.addToGeneralCache( f );

        if ( TYPE_ID == Persistent.TypeId.Result ) {
            final Id EXPR_ID = new Id( this.parseExperimentIdFromResultFile( f ) );
            this.addToResultsCache( f, EXPR_ID );
        }
    }

    /** Adds a result file to the results cache.
      * @param f The file corresponding to a Result object.
      * @param exprId The experiment id this results was obtained from.
      */
    private void addToResultsCache(File f, Id exprId)
    {
        ArrayList<File> resFileList = this.resultsPerExperiment.get( exprId );

        if ( resFileList == null ) {
            resFileList = new ArrayList<>();
            this.resultsPerExperiment.put( exprId, resFileList );
        }

        resFileList.add( f );
    }

    /** @return the general cache corresponding to the object of the type. */
    private HashSet<File> getCacheForType(Persistent.TypeId typeId)
    {
        HashSet<File> toret;

        if ( typeId == Persistent.TypeId.User ) {
            toret = this.filesUser;
        }
        else
        if ( typeId == Persistent.TypeId.Experiment ) {
            toret = this.filesExperiment;
        }
        else
        if ( typeId == Persistent.TypeId.Result ) {
            toret = this.filesResult;
        } else {
            throw new IllegalArgumentException( "no cache for type: " + typeId.toString() );
        }

        return toret;
    }

    /** Removes the object from all caches.
      * @param p The object to remove from all caches.
      * @param f The file corresponding to that object, calculated if null.
      */
    private void removeFromAllCaches(Persistent p, File f)
    {
        if ( f == null ) {
            f = new File( this.dirDb, this.getFileNameFor( p ) );
        }

        if ( p.getTypeId() == Persistent.TypeId.Result ) {
            this.removeFromResultsCache( (Result) p, f );
        }

        this.removeFromGeneralCache( f );

    }

    /** Removes the result from the results cache.
      * @param res The result object itself.
      * @param f The corresponding file to the results object, recalculated if null.
      */
    private void removeFromResultsCache(Result res, File f)
    {
        if ( f == null ) {
            f = new File( this.dirDb, this.getFileNameFor( res ) );
        }

        // Look for it in the results by experiment cache
        ArrayList<File> fileList = this.resultsPerExperiment.get( res.getExperiment().getId() );

        if ( fileList != null ) {
            fileList.remove( f );
        }

        return;
    }

    /** Remove a file from the general cache.
      *  @param f Tje file to remove.
      */
    private void removeFromGeneralCache(File f)
    {
        this.getCacheForType( getTypeIdForExt( f ) ).remove( f );
    }

    /** Add a file to the general cache.
      * @param f The file to add.
      */
    private void addToGeneralCache(File f)
    {
        final HashSet<File> CACHED_LIST = this.getCacheForType( getTypeIdForExt( f ) );

        if ( !CACHED_LIST.contains( f ) ) {
            CACHED_LIST.add( f );
        }

        return;
    }

    /** @return true if the file exists in the cache, false otherwise. */
    private boolean existsInCache(File f)
    {
        return this.getCacheForType( getTypeIdForExt( f ) ).contains( f );
    }

    /** Decide the type for a file, following the extension.
     *
     * @param f the file to decide the type for.
     * @return the typeid for that file.
     * @see Persistent
     */
    private static Persistent.TypeId getTypeIdForExt(File f)
    {
        final String EXT_USR = getFileExtFor( Persistent.TypeId.User );
        final String EXT_EXPERIMENT = getFileExtFor( Persistent.TypeId.Experiment );
        final String EXT_RESULT = getFileExtFor( Persistent.TypeId.Result );
        Persistent.TypeId toret = null;

        if ( f.getName().endsWith( EXT_USR ) ) {
            toret = Persistent.TypeId.User;
        }
        else
        if ( f.getName().endsWith( EXT_EXPERIMENT ) ) {
            toret = Persistent.TypeId.Experiment;
        }
        else
        if ( f.getName().endsWith( EXT_RESULT ) ) {
            toret = Persistent.TypeId.Result;
        } else {
            final String ERROR_MSG = "File ext not found for: "
                        + f.getName();
            Log.e( LOG_TAG, ERROR_MSG );
            throw new Error( ERROR_MSG );
        }

        return toret;
    }

    /** @return the appropriate data file name for this object. */
    private String getFileNameFor(Id id, Persistent.TypeId typeId)
    {
        String toret = "";

        if ( typeId == Persistent.TypeId.Result ) {
            try {
                for(PartialObject po: this.enumerateObjects( Persistent.TypeId.Result ))
                {
                    if ( po.getId().equals( id ) ) {
                        final String RESULT_INFO = po.getName();
                        final Id EXPR_ID = new Id( Result.parseExperimentIdFromName( RESULT_INFO ) );
                        final Id USER_ID = new Id( Result.parseUserIdFromName( RESULT_INFO ) );
                        final User USR = this.createOrRetrieveUserById( USER_ID );
                        final Experiment EXPR = (Experiment) this.retrieve( EXPR_ID, Persistent.TypeId.Experiment );

                        toret = getFileNameForResult( id, EXPR_ID, USER_ID, USR.getName(), EXPR.getName() );
                        break;
                    }
                }
            } catch(IOException exc) {
                final String ERROR_MESSAGE = "error retrieving result with id: " + id.get() + ": "
                                        + exc.getMessage();
                Log.e(LOG_TAG, ERROR_MESSAGE );
                throw new Error( ERROR_MESSAGE );
            }
        } else {
            toret = REGULAR_FILE_FORMAT
                    .replace( "$" + FIELD_ID, id.toString() )
                    .replace( "$" + FIELD_EXT, getFileExtFor( typeId ) );
        }

        return toret;
    }

    /** @return the name for a the corresponding result file.
      * @param id The id of the result file.
      * @param exprId The experiment id for the result data set file.
      * @param userId The user id for the result data set file.
      * @param userName The name of the user.
      * @param experimentName the name of the experiment.
      */
    private static String getFileNameForResult(Id id, Id exprId, Id userId, String userName, String experimentName)
    {
        return RESULT_FILE_FORMAT
                .replace( "$" + FIELD_EXT, getFileExtFor( Persistent.TypeId.Result ) )
                .replace( "$" + FIELD_ID, id.toString() )
                .replace( "$" + FIELD_USER_ID, userId.toString() )
                .replace( "$" + FIELD_EXPERIMENT_ID, exprId.toString() )
                .replace( "$" + FIELD_USER, FileNameAdapter.get().encode( userName ) )
                .replace( "$" + FIELD_EXPERIMENT, FileNameAdapter.get().encode( experimentName ) );
    }

    /** @return the appropriate data file name for this object. */
    public static String getFileNameFor(Persistent p)
    {
        final Persistent.TypeId TYPE_ID = p.getTypeId();
        String toret = "";

        if ( TYPE_ID == Persistent.TypeId.Result ) {
            final Result RES = (Result) p;

            toret = getFileNameForResult( p.getId(),
                                               RES.getExperiment().getId(),
                                               RES.getUser().getId(),
                                               RES.getUser().getName(),
                                               RES.getExperiment().getName() );
        } else {
            toret = REGULAR_FILE_FORMAT
                    .replace( "$" + FIELD_ID, p.getId().toString() )
                    .replace( "$" + FIELD_EXT, getFileExtFor( TYPE_ID ) );
        }

        return toret;
    }

    /** Extracts the id from the file name.
      * Whatever this file is, the id between the '.' for the ext and the '-' separator.
      * @param file the file to extract the id from.
      * @return A long with the id.
      * @throws Error if the file name does not contain an id.
      */
    private long parseIdFromFile(File file)
    {
        final String FILE_NAME = file.getName();
        final int SEPARATOR_POS = FILE_NAME.indexOf( FILE_NAME_PART_SEPARATOR );
        long toret;

        if ( SEPARATOR_POS >= 0 ) {
            int extSeparatorPos = FILE_NAME.lastIndexOf( '.' );

            if ( extSeparatorPos < 0 ) {
                extSeparatorPos = FILE_NAME.length();
            }

            final String ID = FILE_NAME.substring( SEPARATOR_POS + 1, extSeparatorPos );

            try {
                toret = Long.parseLong( ID );
            } catch(NumberFormatException exc) {
                throw new Error( "parseIdFromFile: malformed id: " + ID );
            }
        } else {
            throw new Error( "parseIdFromFile: separator not found in file name: " + FILE_NAME );
        }

        return toret;
    }

    /** Any result file name contains the user's and experiment's ids.
      * Format: id-xxx-user_id-yyy-experiment_id-zzz.res
     * @param f The file to extract the experiment id from.
     * @return A long number corresponding to the id.
     */
    private long parseExperimentIdFromResultFile(File f)
    {
        final String FILE_NAME = f.getName();
        int exprIdPos = FILE_NAME.indexOf( FIELD_EXPERIMENT_ID );
        long toret;

        if ( exprIdPos >= 0 ) {
            int extSeparatorPos = FILE_NAME.lastIndexOf( '.' );

            if ( extSeparatorPos < 0 ) {
                extSeparatorPos = FILE_NAME.length();
            }

            exprIdPos += 1 + FIELD_EXPERIMENT_ID.length();
            final String ID = FILE_NAME.substring( exprIdPos, extSeparatorPos );

            try {
                toret = Long.parseLong( ID );
            } catch(NumberFormatException exc) {
                throw new Error( "parseExperimentIdFromResultFile: malformed id: " + ID );
            }
        } else {
            throw new Error( "parseExperimentIdFromResultFile: separator not found in file name: " + FILE_NAME );
        }

        return toret;
    }

    /** Removes object 'p' from the database.
      * @param p The object to remove.
      */
    public void remove(Persistent p)
    {
        if ( p instanceof Experiment ) {
            // Remove the whole directory for media
            removeTreeAt( this.buildMediaDirectoryFor( p ) );

            // Remove all related results
            ArrayList<File> resultFiles = this.resultsPerExperiment.get( p.getId() );

            if ( resultFiles != null ) {
                for(final File RESULT_FILE: resultFiles) {
                    if ( !RESULT_FILE.delete() ) {
                        Log.e(LOG_TAG, "unable to remove file: " + RESULT_FILE );
                    }

                    this.filesResult.remove( RESULT_FILE );
                }
            }

            // Completely remove cache for this experiment
            this.resultsPerExperiment.remove( p.getId() );
        }

        // Remove main object
        final File REMOVE_FILE = new File( this.dirDb, this.getFileNameFor( p ) );

        this.removeFromAllCaches( p, REMOVE_FILE );

        if ( !REMOVE_FILE.delete() ) {
            Log.e(LOG_TAG, "Error removing file: " + REMOVE_FILE );
        }

        Log.d(LOG_TAG, "Object deleted: " + p.getId() );
    }

    /** Builds the name of the experiment's media directory.
      * @param p The experiment itself.
      * @return the name of the directory, as a string.
      */
    private static String buildMediaDirectoryNameFor(@NonNull Persistent p)
    {
        final Experiment OWNER = p.getExperimentOwner();
        final Id ID = OWNER != null ? OWNER.getId() : p.getId();

        return DIR_MEDIA_PREFIX + FILE_NAME_PART_SEPARATOR + ID.get();
    }

    /** @returns a File pointing to the settings file. */
    public File getSettingsPath()
    {
        return new File( this.dirFiles, SETTINGS_FILE_NAME );
    }

    /** Builds the directory for the experiment's media.
     * @param p The experiment itself.
     * @return a File object for the experiment's media directory.
     */
    public File buildMediaDirectoryFor(@NonNull Persistent p)
    {
        return new File(
                this.dirRes,
                buildMediaDirectoryNameFor( p ) );
    }

    /** Stores a media file in the app's file system.
      * @param expr the experiment this media file belongs to.
      * @param inMedia an input stream the existing media file.
      * @return the new file, to the media file copied to the experiment's dir.
      * @throws IOException if something goes wrong copying, or if the dir could not be created.
      */
    public File storeMedia(@NonNull Experiment expr, @NonNull String fileName, @NonNull InputStream inMedia) throws IOException
    {
        // Time to make the id persistent, if needed.
        // Otherwise, the experiment will have a fake id, and the media file, a different, true id.
        if ( expr.getId().isFake() ) {
            expr.updateIds();
        }

        // Do not allow spaces in file names.
        fileName = buildMediaFileNameForDbFromMediaFileName( fileName );

        final File DIR_EXPERIMENTS = this.buildMediaDirectoryFor( expr );
        final File MEDIA_FILE = new File( DIR_EXPERIMENTS, fileName );

        DIR_EXPERIMENTS.mkdirs();
        if ( DIR_EXPERIMENTS.exists() ) {
            copy( inMedia, MEDIA_FILE );
        } else {
            final String MSG_ERROR = "unable to create directory: " + DIR_EXPERIMENTS;
            throw new IOException( MSG_ERROR );
        }

        return MEDIA_FILE;
    }

    /** Determines the existence of given media.
      * @param expr The experiment this media would pertain to.
      * @param fileName The file name of this media.
      * @return true if the media is already stored, false otherwise.
      */
    public boolean existsMedia(@NonNull Experiment expr, @NonNull String fileName)
    {
        final File DIR_EXPERIMENT = this.buildMediaDirectoryFor( expr );
        final File MEDIA_FILE = new File( DIR_EXPERIMENT, fileName );

        return MEDIA_FILE.exists() && ( expr.locateMediaActivity( fileName ) != null );
    }

    /** Deletes a media file in the app's file system.
     * @param expr the experiment this media file belongs to.
     * @param fileName The name of the file to remove.
     * @throws IOException if something goes wrong deleting, or if the file does not exist.
     */
    public void deleteMedia(@NonNull Experiment expr, @NonNull String fileName) throws IOException
    {
        final File DIR_EXPERIMENT = this.buildMediaDirectoryFor( expr );
        final File MEDIA_FILE = new File( DIR_EXPERIMENT, fileName );

        if ( !MEDIA_FILE.delete() ) {
            throw new IOException( "unable to delete media file: " + fileName );
        }

        return;
    }

    /** Collects the media files associated to a given experiment.
     * @param expr the experiment to collect the files from.
     * @return A new vector of File with the complete, absolute path.
     */
    private File[] collectMediaFilesFor(Experiment expr)
    {
        final File DIR_MEDIA = this.buildMediaDirectoryFor( expr );
        final File[] ASSOC_FILES = expr.enumerateMediaFiles();
        final File[] TORET = new File[ ASSOC_FILES.length ];

        // Build the complete path of the associated files
        for(int i = 0; i < TORET.length; ++i) {
            TORET[ i ] = new File( DIR_MEDIA, ASSOC_FILES[ i ].getName() );
        }

        return TORET;
    }

    /** Checks the associated files (media files) to an experiment and discards
      * the remaining ones.
      * @param expr The experiment to purge media files from.
      */
    public void purgeOrphanMediaFor(Experiment expr)
    {
        try {
            final File DIR_MEDIA = this.buildMediaDirectoryFor( expr );
            final File[] ALL_FILES = DIR_MEDIA.listFiles();

            // Look for each registered file in the actual file list
            if ( ALL_FILES != null ) {
                final ArrayList<File> REG_MEDIA_FILES = new ArrayList<>(
                        Arrays.asList( this.collectMediaFilesFor( expr ) ) );

                for(File f: ALL_FILES) {
                    if ( !REG_MEDIA_FILES.contains( f ) ) {
                        if ( !f.delete() ) {
                            Log.e(LOG_TAG, "Error deleting file: " + f );
                        }
                    }
                }
            }
        } catch(Exception exc) {
            Log.e(LOG_TAG, "Error purging orphan media for '" + expr.getName()
                            + "': " + exc.getMessage() );
        }

        return;
    }

    /** Removes all media that does not pertain to an existing Experiment. */
    public void purgeOrphanMedia()
    {
        try {
            final File[] DIRS_MEDIA = this.dirRes.listFiles();

            for(File mediaDir: DIRS_MEDIA) {
                final Id ID = new Id( parseIdFromFile( mediaDir ) );
                File exprFile = new File( this.dirDb,
                                          this.getFileNameFor( ID, Persistent.TypeId.Experiment ) );

                if ( !this.existsInCache( exprFile ) ) {
                    removeTreeAt( mediaDir );
                }
            }
        } catch(Exception exc) {
            Log.e(LOG_TAG, "Error purging orphan media: " + exc.getMessage() );
        }

        return;
    }

    /** @return a newly created temp file. */
    public File createTempFile(String prefix, String suffix) throws IOException
    {
        return File.createTempFile( prefix, suffix, this.dirTmp );
    }

    /** Stores any data object.
     * @param p The persistent object to store.
     */
    public void store(Persistent p) throws IOException
    {
        this.store( this.dirDb, p );
    }

    private void store(File dir, Persistent p) throws IOException
    {
        // Assign a real id
        if ( p.getId().isFake() ) {
            p.updateIds();
        }

        // Store the data
        final File TEMP_FILE = this.createTempFile(
                                    p.getTypeId().toString(),
                                    p.getId().toString() );
        final File DATA_FILE = new File( dir, this.getFileNameFor( p ) );
        Writer writer = null;

        try {
            Log.i(LOG_TAG, "Storing: " + p.toString() + " to: " + DATA_FILE.getAbsolutePath() );
            writer = openWriterFor( TEMP_FILE );
            p.toJSON( writer );
            close( writer );
            if ( !TEMP_FILE.renameTo( DATA_FILE ) ) {
                Log.d(LOG_TAG, "Unable to move: " + DATA_FILE );
                Log.d(LOG_TAG, "Trying to copy: " + TEMP_FILE + " to: " + DATA_FILE );
                copy( TEMP_FILE, DATA_FILE );
            }
            this.updateCaches( p, DATA_FILE );
            Log.i(LOG_TAG, "Finished storing." );
        } catch(IOException exc) {
            final String ERROR_MSG = "I/O error writing: "
                            + DATA_FILE.toString() + ": " + exc.getMessage();
            Log.e(LOG_TAG, ERROR_MSG );
            throw new IOException( ERROR_MSG );
        } catch(JSONException exc) {
            final String ERROR_MSG = "error creating JSON for: "
                            + DATA_FILE.toString() + ": " + exc.getMessage();
            Log.e(LOG_TAG, ERROR_MSG );
            throw new IOException( ERROR_MSG );
        } finally {
          close( writer );
          if ( !TEMP_FILE.delete() ) {
              Log.e(LOG_TAG, "Error removing file: " + TEMP_FILE );
          }
        }
    }

    /** Imports an experiment result from a JSON file, previously exported.
     * @param jsonFileIn An input stream to the zip file.
     * @throws IOException if something goes wrong, like not enough space.
     */
    public void importResult(InputStream jsonFileIn) throws IOException
    {
        try {
            final Reader FILE_READER = openReaderFor( jsonFileIn );
            final Result TORET = Result.fromJSON( FILE_READER );
            close( FILE_READER );

            // Store the result data set
            TORET.updateIds();
            this.store( TORET );
        } catch(JSONException exc) {
            final String ERROR_MSG = "unable to import result file: " + exc.getMessage();

            Log.e(LOG_TAG, ERROR_MSG );
            throw new IOException( ERROR_MSG );
        }

        return;
    }

    /** Imports an experiment from a zip file, previously exported.
      * @param zipFileIn An input stream to the zip file.
      * @return The retrieved experiment.
      * @throws IOException if something goes wrong, like not enough space.
      */
    public Experiment importExperiment(InputStream zipFileIn) throws IOException
    {
        Experiment toret;
        final File TEMP_DIR = new File( this.dirTmp,
                                    "zip" + FILE_NAME_PART_SEPARATOR
                                        + Long.toString( new Random().nextLong() ) );

        try {
            TEMP_DIR.mkdir();
            ZipUtil.unzip( zipFileIn, TEMP_DIR );

            final File[] ALL_FILES = TEMP_DIR.listFiles();
            File experimentFile = null;
            final ArrayList<File> MEDIA_FILES = new ArrayList<>( ALL_FILES.length );
            final String EXPERIMENT_EXTENSION = getFileExtFor( Persistent.TypeId.Experiment );

            // Classify files
            for(File f: ALL_FILES) {
                if ( extractFileExt( f ).equals( EXPERIMENT_EXTENSION ) ) {
                    experimentFile = f;
                } else {
                    MEDIA_FILES.add( f );
                }
            }

            // Chk
            if ( experimentFile == null ) {
                throw new IOException( "fatal: experiment file was not found" );
            }

            // Load the experiment
            try {
                toret = (Experiment) Persistent.fromJSON(
                                Persistent.TypeId.Experiment,
                                openReaderFor( experimentFile ) );
                this.chkIds( toret );
            } catch(JSONException exc)
            {
                throw new IOException( "error reading JSON: " + exc.getMessage() );
            }

            // Store the experiment
            this.store( toret );
            for(File f: MEDIA_FILES) {
                this.storeMedia( toret,
                                    f.getName(),
                                    new FileInputStream( f ) );
            }

        } finally {
            removeTreeAt( TEMP_DIR );
        }

        return toret;
    }

    /** Check that the id's inside the experiment are not repeated in the store.
      * @param expr the experiment to check.
      * @throws JSONException when the id's are repeated.
      */
    private void chkIds(Experiment expr) throws JSONException
    {
        final Id ID = expr.getId();

        try {
            if ( this.lookForObjById( ID, Persistent.TypeId.Experiment ) != null ) {
                throw new JSONException( "already existing as experiment id:" + ID );
            }

            if ( this.lookForObjById( ID, Persistent.TypeId.Result ) != null ) {
                throw new JSONException( "already existing as a result id:" + ID );
            }
        } catch(IOException exc) {
            throw new JSONException( exc.getMessage() );
        }

        return;
    }

    /** Exports a given result set.
     * @param dir the directory to export the result set to.
     *             If null, then Downloads is chosen.
     * @param res the result to export.
     * @throws IOException if something goes wrong, like a write fail.
     */
    public void exportResult(File dir, Result res) throws IOException
    {
        final String RES_FILE_NAME = this.getFileNameFor( res );

        if ( dir == null ) {
            dir = DIR_DOWNLOADS;
        }

        try {
            final String BASE_FILE_NAME = removeFileExt( RES_FILE_NAME );
            final String TAGS_FILE_NAME = "tags-"
                                            + res.getUser().getName()
                                            + "-" + BASE_FILE_NAME + ".tags.txt";
            final String RR_FILE_NAME = "rr-"
                                            + res.getUser().getName()
                                            + "-" + BASE_FILE_NAME + ".rr.txt";

            // Org
            final File ORG_FILE = new File( this.dirDb, RES_FILE_NAME );
            this.store( res );

            // Dest
            final File OUTPUT_FILE = new File( dir, RES_FILE_NAME );
            final File TAGS_OUTPUT_FILE = new File( dir, TAGS_FILE_NAME );
            final File RR_OUTPUT_FILE = new File( dir, RR_FILE_NAME );
            final Writer TAGS_STREAM = openWriterFor( TAGS_OUTPUT_FILE );
            final Writer BEATS_STREAM = openWriterFor( RR_OUTPUT_FILE );

            dir.mkdirs();
            copy( ORG_FILE, OUTPUT_FILE );
            res.exportToStdTextFormat( TAGS_STREAM, BEATS_STREAM );

            close( TAGS_STREAM );
            close( BEATS_STREAM );
        } catch(IOException exc) {
            throw new IOException(
                    "exporting: '"
                            + RES_FILE_NAME
                            + "' to '" + dir
                            + "': " + exc.getMessage() );
        }

        return;
    }

    /** Exports a given experiment.
      * @param dir the directory to export the experiment to.
     *             If null, then Downloads is chosen.
      * @param expr the experiment to export.
      * @throws IOException if something goes wrong, like a write fail.
      */
    public void exportExperiment(File dir, Experiment expr) throws IOException
    {
        final ArrayList<File> FILES = new ArrayList<>();
        final File TEMP_FILE = this.createTempFile(
                expr.getTypeId().toString(),
                expr.getId().toString() );

        if ( dir == null ) {
            dir = DIR_DOWNLOADS;
        }

        try {
            this.store( expr );
            FILES.addAll( Arrays.asList( this.collectMediaFilesFor( expr ) ) );
            FILES.add( new File( this.dirDb, this.getFileNameFor( expr ) ) );

            ZipUtil.zip(
                    FILES.toArray( new File[ 0 ] ),
                    TEMP_FILE );

            dir.mkdirs();
            final File OUTPUT_FILE = new File( dir, this.getFileNameFor( expr ) + ".zip" );
            copy( TEMP_FILE, OUTPUT_FILE );
        } catch(IOException exc) {
            throw new IOException(
                            "exporting: '"
                            + expr.getName()
                            + "': " + exc.getMessage() );
        } finally {
            if ( !TEMP_FILE.delete() ) {
                Log.e(LOG_TAG, "Error removing: " + TEMP_FILE );
            }
        }

        return;
    }

    /** Retrieves the user from the database or just creates (and stores) it.
     * @param usrName The name of the user to retrieve.
     * @return The user (retrieved or created).
     * @throws IOException IF something goes wrong retrieving or creating the user.
     */
    public User createOrRetrieveUserByName(String usrName) throws IOException
    {
        User toret = null;

        // Retrieve it, if possible
        try {
            toret = this.lookForUserByName( usrName );
        } catch(IOException exc)  {
            Log.d(LOG_TAG, "unable to find user: " + usrName + ", creating it." );
        }

        // Create it
        if ( toret == null ) {
            toret = this.createNewUser( Id.create(), usrName );
        }

        return toret;
    }

    public User createOrRetrieveUserById(Id userId) throws IOException
    {
        User toret = null;

        // Retrieve it, if possible
        try {
            toret = (User) this.retrieve( userId, Persistent.TypeId.User );
        } catch(IOException exc)
        {
            Log.d(LOG_TAG, "unable to find user: " + userId + ", creating it." );
        }

        // Create it
        if ( toret == null ) {
            toret = this.createNewUser( userId, null );
        }

        return toret;
    }

    /** Call by the createOrRetrieveUser... *Important* should not exist yet.
      * @param userId
      */
    private User createNewUser(Id userId, String name) throws IOException
    {
        if ( name == null
          || name.trim().isEmpty() )
        {
            name = FIELD_USER_ID + "-" + userId;
        }

        final User USR = new User( userId, name );

        this.store( USR );
        return USR;
    }

    public File getFileById(Id id, Persistent.TypeId typeId)
    {
        return new File( this.dirDb, this.getFileNameFor( id, typeId ) );
    }

    /** Retrieves a single object from a table, given its id.
     * @param id The id of the object to retrieve.
     * @param typeId The id of the type of the object.
     * @return A Persistent object.
     * @see Persistent
     */
    public Persistent retrieve(Id id, Persistent.TypeId typeId) throws IOException
    {
        Persistent toret;
        final File DATA_FILE = this.getFileById( id, typeId );
        Reader reader = null;

        try {
            reader = openReaderFor( DATA_FILE );

            toret = Persistent.fromJSON( typeId, reader );
            Log.i(LOG_TAG, "Retrieved: " + toret.toString() + " from: "
                            + DATA_FILE.getAbsolutePath()  );
        } catch(IOException exc) {
            final String ERROR_MSG = "I/O error reading: "
                            + DATA_FILE.toString() + ": " + exc.getMessage();
            Log.e(LOG_TAG, ERROR_MSG );
            throw new IOException( ERROR_MSG );
        } catch(JSONException exc) {
            final String ERROR_MSG = "error reading JSON for: "
                    + DATA_FILE.toString() + ": " + exc.getMessage();
            Log.e(LOG_TAG, ERROR_MSG );
            throw new IOException( ERROR_MSG );
        } finally {
            close( reader );
        }

        return toret;
    }

    /** @return a list of the files in the resource's directory.
      *         Remember that files for each experiment are saved under subdir. */
    public List<Pair<String, File[]>> enumerateResFiles()
    {
        File[] SUB_DIRS = this.dirRes.listFiles();
        final ArrayList<Pair<String, File[]>> TORET = new ArrayList<>();

        for(File subDir: SUB_DIRS) {
            TORET.add( Pair.create( subDir.getName(), subDir.listFiles() ) );
        }

        return TORET;
    }

    /** Enumerates all Result objects for a given experiment.
     *  @return An array of pairs of Id's and Strings.
     */
    @SuppressWarnings("unchecked")
    public PartialObject[] enumerateResultsForExperiment(Id id) throws IOException
    {
        return enumerateObjects( this.resultsPerExperiment.get( id ) );
    }

    /** Enumerates all objects of a given type.
     *  @return An array of pairs of Id's and Strings.
     */
    public File[] enumerateFiles(Persistent.TypeId typeId)
    {
        HashSet<File> toret = this.filesExperiment;

        // Decide cache file list
        if ( typeId == Persistent.TypeId.Result ) {
            toret = this.filesResult;
        }
        else
        if ( typeId == Persistent.TypeId.User ) {
            toret = this.filesUser;
        }

        return toret.toArray( new File[ 0 ] );
    }

    /** Enumerates all objects of a given type.
     *  @return An array of pairs of Id's and Strings.
     */
    @SuppressWarnings("unchecked")
    private PartialObject[] enumerateObjects(Persistent.TypeId typeId) throws IOException
    {
        HashSet<File> toret = this.filesExperiment;

        // Decide cache file list
        if ( typeId == Persistent.TypeId.Result ) {
            toret = this.filesResult;
        }
        else
        if ( typeId == Persistent.TypeId.User ) {
            toret = this.filesUser;
        }

        return this.enumerateObjects( toret );
    }

    /** Enumerates all objects of a given file list.
     *  @return An array of pairs of Id's and Strings.
     */
    @SuppressWarnings("unchecked")
    private PartialObject[] enumerateObjects(Collection<File> fileList) throws IOException
    {
        if ( fileList == null ) {
            fileList = new ArrayList<>( 0 );
        }

        // Convert to partial objects
        final ArrayList<PartialObject> TORET = new ArrayList<>( fileList.size() );

        for(File f: fileList) {
            TORET.add( retrievePartialObject( f ) );
        }

        return TORET.toArray( new PartialObject[ TORET.size() ] );
    }

    /** @return A persistent object, provided its id and the type of object. */
    private PartialObject lookForObjById(Id id, Persistent.TypeId typeId) throws IOException
    {
        HashSet<File> fileList = this.filesExperiment;

        // Decide cache file list
        if ( typeId == Persistent.TypeId.Result ) {
            fileList = this.filesResult;
        }
        else
        if ( typeId == Persistent.TypeId.User ) {
            fileList = this.filesUser;
        }

        PartialObject toret = null;

        for(File f: fileList) {
            final PartialObject OBJ = retrievePartialObject( f );

            if ( OBJ.getId().equals( id ) ) {
                toret = OBJ;
                break;
            }
        }

        return toret;
    }

    /** @return A persistent object, provided its name and the type of object. */
    private PartialObject lookForObjByName(String name, Persistent.TypeId typeId) throws IOException
    {
        HashSet<File> fileList = this.filesExperiment;

        // Decide cache file list
        if ( typeId == Persistent.TypeId.Result ) {
            fileList = this.filesResult;
        }
        else
        if ( typeId == Persistent.TypeId.User ) {
            fileList = this.filesUser;
        }

        PartialObject toret = null;

        for(File f: fileList) {
            final PartialObject OBJ = retrievePartialObject( f );

            if ( OBJ.getName().equals( name ) ) {
                toret = OBJ;
                break;
            }
        }

        return toret;
    }

    public User lookForUserByName(String name) throws IOException
    {
        final PartialObject USR = this.lookForObjByName( name, Persistent.TypeId.User );
        User toret = null;

        if ( USR != null ) {
            toret = (User) this.retrieve( USR.getId(), Persistent.TypeId.User );
        }

        return toret;
    }

    public Experiment lookForExperimentByName(String name) throws IOException
    {
        final PartialObject EXPR = this.lookForObjByName( name, Persistent.TypeId.Experiment );
        Experiment toret = null;

        if ( EXPR != null ) {
            toret = (Experiment) this.retrieve( EXPR.getId(), Persistent.TypeId.Experiment );
        }

        return toret;
    }

    /** Enumerates all users, getting a vector containing their id's and names. */
    public PartialObject[] enumerateUsers() throws IOException
    {
        return enumerateObjects( Persistent.TypeId.User );
    }

    /** Enumerates all experiments, getting a vector containing their id's and names. */
    public PartialObject[] enumerateExperiments() throws IOException
    {
        return enumerateObjects( Persistent.TypeId.Experiment );
    }

    /** Enumerates all object names, getting an array containing their names. */
    private String[] enumerateObjNames(Persistent.TypeId tid) throws IOException
    {
        return enumerateObjNames( enumerateObjects( tid ) );
    }

    /** Enumerates all object names, given an array of partial object. */
    public String[] enumerateObjNames(PartialObject[] objs)
    {
        final String[] TORET = new String[ objs.length ];

        for(int i = 0; i < objs.length; ++i) {
            TORET[ i ] = objs[ i ].getName();
        }

        return TORET;
    }

    /** Enumerates all experiments, getting an array containing their names. */
    public String[] enumerateExperimentNames() throws IOException
    {
        return enumerateObjNames( Persistent.TypeId.Experiment );
    }

    /** Enumerates all users, getting an array containing their names. */
    public String[] enumerateUserNames() throws IOException
    {
        return enumerateObjNames( Persistent.TypeId.User );
    }

    File getResDir()
    {
        return this.dirRes;
    }

    /** @return the partial object loaded from file f. */
    private static PartialObject retrievePartialObject(File f) throws IOException
    {
        PartialObject toret;
        Reader reader = null;

        try {
            reader = openReaderFor( f );
            toret = PartialObject.fromJSON( reader );
        } catch(IOException|JSONException exc)
        {
            final String MSG = "retrievePartialObject(f) reading JSON: " + exc.getMessage();
            Log.e(LOG_TAG, MSG );

            throw new IOException( MSG );
        } finally {
            close( reader );
        }

        return toret;
    }

    public static Writer openWriterFor(File f) throws IOException
    {
        BufferedWriter toret;

        try {
            final OutputStreamWriter OUTPUT_STREAM_WRITER = new OutputStreamWriter(
                    new FileOutputStream( f ),
                    Charset.forName( "UTF-8" ).newEncoder() );

            toret = new BufferedWriter( OUTPUT_STREAM_WRITER );
        } catch (IOException exc) {
            Log.e(LOG_TAG,"Error creating writer for file: " + f );
            throw exc;
        }

        return toret;
    }

    public static BufferedReader openReaderFor(File f) throws IOException
    {
        BufferedReader toret;

        try {
            toret = openReaderFor( new FileInputStream( f ) );
        } catch (IOException exc) {
            Log.e(LOG_TAG,"Error creating reader for file: " + f.getName() );
            throw exc;
        }

        return toret;
    }

    private static BufferedReader openReaderFor(InputStream inStream)
    {
        final InputStreamReader INPUT_STREAM_READER = new InputStreamReader(
                inStream,
                Charset.forName( "UTF-8" ).newDecoder() );

        return new BufferedReader( INPUT_STREAM_READER );
    }

    /** Closes a writer stream. */
    public static void close(Writer writer)
    {
        try {
            if ( writer != null ) {
                writer.close();
            }
        } catch(IOException exc)
        {
            Log.e(LOG_TAG, "closing writer: " + exc.getMessage() );
        }
    }

    /** Closes a reader stream. */
    public static void close(Reader reader)
    {
        try {
            if ( reader != null ) {
                reader.close();
            }
        } catch(IOException exc)
        {
            Log.e(LOG_TAG, "closing reader: " + exc.getMessage() );
        }
    }

    /** Closes a JSONReader stream. */
    private static void close(JsonReader jsonReader)
    {
        try {
            if ( jsonReader != null ) {
                jsonReader.close();
            }
        } catch(IOException exc)
        {
            Log.e(LOG_TAG, "closing json reader: " + exc.getMessage() );
        }
    }

    /** Copies a given file to a destination, overwriting if necessary.
      * @param source The File object of the source file.
      * @param dest The File object of the destination file.
      * @throws IOException if something goes wrong while copying.
      */
    private static void copy(File source, File dest) throws IOException
    {
        final String MSG_ERROR = "error copying: " + source + " to: " + dest + ": ";
        InputStream is;
        OutputStream os;

        try {
            is = new FileInputStream( source );
            os = new FileOutputStream( dest );

            copy( is, os );
        } catch(IOException exc)
        {
            Log.e(LOG_TAG, MSG_ERROR + exc.getMessage() );
            throw new IOException( MSG_ERROR );
        }

        return;
    }

    /** Copies data from a given stream to a destination File, overwriting if necessary.
     * @param is The input stream object to copy from.
     * @param dest The File object of the destination file.
     * @throws IOException if something goes wrong while copying.
     */
    private static void copy(InputStream is, File dest) throws IOException
    {
        final String MSG_ERROR = "error copying input stream -> " + dest + ": ";
        OutputStream os;

        try {
            os = new FileOutputStream( dest );

            copy( is, os );
        } catch(IOException exc)
        {
            Log.e(LOG_TAG, MSG_ERROR + exc.getMessage() );
            throw new IOException( MSG_ERROR );
        }

        return;
    }

    /** Copies from a stream to another one.
     * @param is The input stream object to copy from.
     * @param os The output stream object of the destination.
     * @throws IOException if something goes wrong while copying.
     */
    private static void copy(InputStream is, OutputStream os) throws IOException
    {
        final byte[] BUFFER = new byte[1024];
        int length;

        try {
            while ( ( length = is.read( BUFFER ) ) > 0 ) {
                os.write( BUFFER, 0, length );
            }
        } finally {
            try {
                if ( is != null ) {
                    is.close();
                }

                if ( os != null ) {
                    os.close();
                }
            } catch(IOException exc) {
                Log.e(LOG_TAG, "Copying file: error closing streams: " + exc.getMessage() );
            }
        }

        return;
    }

    /** Saves the given file in the store, with the given file name.
      * This is especially used when recovering from the cloud.
      * Caution: the copied file is not yet reflected in the data structures.
      * Call reset() in order to restore the balance of the force once all files
      * are copied.
      * @param f the file
      * @param fileName
      */
    void copyDataFileAs(File f, String fileName) throws IOException
    {
        copy( f, new File( this.dirDb, fileName ) );
    }

    /** Saves the given file in the res dir, with the given file name.
     * This is especially used when recovering from the cloud.
     * Caution: the copied file is not yet reflected in the data structures.
     * Call reset() in order to restore the balance of the force once all files
     * are copied.
     * @param resSubDir the subdir inside the resource directory.
     * @param f the file
     * @param fileName
     */
    void copyResFileAs(String resSubDir, File f, String fileName) throws IOException
    {
        File subDir = new File( this.dirRes, resSubDir );

        subDir.mkdir();
        copy( f, new File( subDir, fileName ) );
    }

    /** Removes a whole dir and all its subdirectories, recursively.
      * @param dir The directory to remove.
      */
    private static void removeTreeAt(File dir)
    {
        if ( dir != null
          && dir.isDirectory() )
        {
            final String[] ALL_FILES = dir.list();

            if ( ALL_FILES != null ) {
                for(String fileName: ALL_FILES) {
                    File f = new File( dir, fileName );

                    if ( f.isDirectory() ) {
                        removeTreeAt( f );
                    }

                    if ( !f.delete() ) {
                        Log.e(LOG_TAG, "Error deleting directory: " + f );
                    }
                }

                if ( !dir.delete() ) {
                    Log.e(LOG_TAG, "Error deleting directory: " + dir );
                }
            }
        } else {
            Log.d(LOG_TAG, "removeTreeAt: directory null or not a directory?" );
        }

        return;
    }

    /** @return the file extension, extracted from param.
     * @param file The file, as a File.
     */
    public static String extractFileExt(File file)
    {
        return extractFileExt( file.getPath() );
    }

    /** @return the file extension, extracted from param.
      * @param fileName The file name, as a String.
      */
    public static String extractFileExt(String fileName)
    {
        String toret = "";

        if ( fileName != null
          && !fileName.trim().isEmpty() )
        {
            final int POS_DOT = fileName.trim().lastIndexOf( "." );


            if ( POS_DOT >= 0
              && POS_DOT < ( fileName.length() - 1 ) )
            {
                toret = fileName.substring( POS_DOT + 1 );
            }
        }

        return toret;
    }

    /** @return the given file name, after extracting the extension.
      * @param fileName the file name to remove the extension from.
      * @return the file name without extension.
      */
    public static String removeFileExt(String fileName)
    {
        final int POS_DOT = fileName.lastIndexOf( '.' );
        String toret = fileName;

        if ( POS_DOT > -1 ) {
            toret = fileName.substring(0, POS_DOT);
        }

        return toret;
    }

    /** Returns the extension for the corresponding data file name.
     * Note that it will be of three chars, lowercase.
     *
     * @param typeId the typeId id of the object.
     * @return the corresponding extension, as a string.
     * @see Persistent
     */
    public static String getFileExtFor(Persistent.TypeId typeId)
    {
        String toret = typeId.toString().substring( 0, 3 ).toLowerCase();

        if ( typeId == Persistent.TypeId.User ) {
            toret = "usr";
        }

        return toret;
    }

    /** Returns a media file name suitable for storing in the database.
      * Media files are stored in the db with the same name they have outside,
      * but with an important caveat: it cannot have spaces inside.
      * @param fileName The name of the file.
      * @return A suitable file name.
      */
    public static String buildMediaFileNameForDbFromMediaFileName(String fileName)
    {
        // This is an extra indirection to avoid calling directly Tag.encode( fileName ).
        return fileNameAdapter.encode( fileName );
    }

    /** Gets the already open database.
     * @return The Orm singleton object.
     */
    public static Orm get()
    {
        if ( instance == null ) {
            final String ERROR_MSG = "Orm database manager not created yet.";

            Log.e(LOG_TAG, ERROR_MSG );
            throw new IllegalArgumentException( ERROR_MSG );
        }

        return instance;
    }

    /** Initialises the already open database.
     * @param context the application context this database will be working against.
     * @param fileNameAdapter A lambda or referenced method of the signature: (String x) -> x
     *                        this will convert file names to the needed standard, which is
     *                        lowercase and no spaces ('_' instead).
     * @see Function
     */
    public static void init(Context context, FileNameAdapter fileNameAdapter)
    {
        if ( instance == null ) {
            instance = new Orm( context );
        }

        Orm.fileNameAdapter = fileNameAdapter;
        return;
    }

    private HashSet<File> filesUser;
    private HashSet<File> filesExperiment;
    private HashSet<File> filesResult;
    private Map<Id, ArrayList<File>> resultsPerExperiment;
    private File dirFiles;
    private File dirDb;
    private File dirRes;
    private File dirTmp;
    private Context context;

    private static FileNameAdapter fileNameAdapter;
    private static Orm instance;
    private static File DIR_DOWNLOADS;
}

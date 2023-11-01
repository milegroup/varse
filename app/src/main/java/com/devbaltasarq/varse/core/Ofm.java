package com.devbaltasarq.varse.core;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.annotation.NonNull;

import android.provider.MediaStore;
import android.util.JsonReader;
import android.util.Log;
import android.util.Pair;

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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import com.devbaltasarq.varse.core.experiment.Tag;
import com.devbaltasarq.varse.core.ofmcache.EntitiesCache;
import com.devbaltasarq.varse.core.ofmcache.FileCache;
import com.devbaltasarq.varse.core.ofmcache.PartialObject;
import com.devbaltasarq.varse.core.ofmcache.ResultsPerExperiment;

import javax.activation.MimeType;


/** OFM (Object-File Mapper): Relates the database of JSON files to objects. */
public final class Ofm {
    private static final String LOG_TAG = Ofm.class.getSimpleName();

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
    public static final String FIELD_NAME = "name";
    public static final String FIELD_REC = "rec";

    private static final String SETTINGS_FILE_NAME = "settings.json";
    private static final String DIR_DB = "db";
    static final String DIR_RES = "res";
    private static final String TEXT_MIME_TYPE = "text/plain";
    private static final String ZIP_MIME_TYPE = "application/zip";


    /** Prepares the ORM to operate. */
    private Ofm(Context context)
    {
        this.context = context;
        this.cache = new EntitiesCache();
        this.resultsPerExperiment = new ResultsPerExperiment();

        DIR_DOWNLOADS = null;
        if ( android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q )
        {
            DIR_DOWNLOADS = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOWNLOADS );
        }

        this.reset();
    }

    /** Forces a reset of the ORM, so the contents of the store is reflected
      *  in the internal data structures.
      */
    public void reset()
    {
        Log.i( LOG_TAG, "Preparing store..." );

        this.dirFiles = this.context.getFilesDir();

        this.createDirectories();
        this.removeCache();
        this.createCaches();

        Log.i( LOG_TAG, "Store ready at: " + this.dirDb.getAbsolutePath() );
        Log.i( LOG_TAG, "    #experiment files: " + this.cache.getExperiments().count() );
        Log.i( LOG_TAG, "    #result files: " + this.cache.getResults().count() );
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
        this.cache.clear();
        this.resultsPerExperiment.clear();

        for (final File F: getAllFilesInDir( dirDb )) {
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
            f = new File( this.dirDb, buildFileNameFor( p ) );
        }

        this.cache.get( p.getTypeId() ).add( f );

        if ( p.getTypeId() == Persistent.TypeId.Result ) {
            this.resultsPerExperiment.add( ( (Result) p ).getExperiment().getId(), f );
        }

        return;
    }

    /** Updates all caches.
      * @param f The file to include in all caches.
      */
    private void updateCaches(File f)
    {
        final Persistent.TypeId TYPE_ID = EntitiesCache.getTypeIdForExt( f );

        if ( TYPE_ID != null ) {
            this.cache.get( TYPE_ID ).add( f );

            if ( TYPE_ID == Persistent.TypeId.Result ) {
                final Id EXPR_ID = new Id( EntitiesCache.parseExperimentIdFromResultFileName( f ) );
                this.resultsPerExperiment.add( EXPR_ID, f );
            }
        }
    }

    /** @return the file name this object will be saved with.
      * @param p The data object to build the file name for.
      */
    public static String buildFileNameFor(Persistent p)
    {
        final Persistent.TypeId TYPE_ID = p.getTypeId();
        String toret;

        if ( TYPE_ID == Persistent.TypeId.Result ) {
            final Result RES = (Result) p;

            toret = EntitiesCache.buildFileNameForResult(
                    RES.getId(),
                    RES.getExperiment().getId(),
                    RES.getExperiment().getName(),
                    RES.getRec() );
        } else {
            toret = EntitiesCache.buildFileNameFor( p.getId(), p.getTypeId() );
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
            final File[] RESULT_FILES = this.resultsPerExperiment.get( p.getId() );

            if ( RESULT_FILES != null ) {
                for(final File RESULT_FILE: RESULT_FILES) {
                    if ( !RESULT_FILE.delete() ) {
                        Log.e(LOG_TAG, "unable to remove file: " + RESULT_FILE );
                    }

                    this.cache.getResults().remove( RESULT_FILE );
                }
            }

            // Completely remove cache for this experiment
            this.resultsPerExperiment.remove( p.getId() );
        }

        // Remove main object
        final File REMOVE_FILE = new File( this.dirDb, buildFileNameFor( p ) );

        this.cache.get( p.getTypeId() ).remove( REMOVE_FILE );

        if ( p instanceof Result ) {
            final Id EXPR_ID = ( (Result) p ).getExperiment().getId();
            this.resultsPerExperiment.removeFor( EXPR_ID, REMOVE_FILE );
        }

        if ( !REMOVE_FILE.delete() ) {
            Log.e( LOG_TAG, "Error removing file: " + REMOVE_FILE );
        }

        Log.d( LOG_TAG, "Object deleted: " + p.getId() );
    }

    /** Builds the name of the experiment's media directory.
      * @param p The experiment itself.
      * @return the name of the directory, as a string.
      */
    private static String buildMediaDirectoryNameFor(@NonNull Persistent p)
    {
        final Experiment OWNER = p.getExperimentOwner();
        final Id ID = OWNER != null ? OWNER.getId() : p.getId();

        return FileCache.FILE_NAME_PART_MEDIA + FileCache.FILE_NAME_PART_SEPARATOR + ID.get();
    }

    /** @return a File pointing to the settings file. */
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

        if ( createDir( DIR_EXPERIMENTS ) ) {
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

            // Look for each registered file in the actual file list
            final ArrayList<File> REG_MEDIA_FILES = new ArrayList<>(
                    Arrays.asList( this.collectMediaFilesFor( expr ) ) );

            for(File f: getAllFilesInDir( DIR_MEDIA )) {
                if ( !REG_MEDIA_FILES.contains( f ) ) {
                    if ( !f.delete() ) {
                        Log.e( LOG_TAG, "Error deleting file: " + f );
                    }
                }
            }
        } catch(Exception exc) {
            Log.e( LOG_TAG, "Error purging orphan media for '" + expr.getName()
                            + "': " + exc.getMessage() );
        }

        return;
    }

    /** Removes all media that does not pertain to an existing Experiment. */
    public void purgeOrphanMedia()
    {
        try {
            for(File mediaDir: getAllFilesInDir( this.dirRes )) {
                final Id ID = new Id( FileCache.parseIdFromMediaDir( mediaDir ) );
                final File EXPR_FILE = new File( this.dirDb,
                                          EntitiesCache.buildFileNameFor(
                                                  ID, Persistent.TypeId.Experiment ) );

                if ( !this.cache.get( Persistent.TypeId.Experiment ).contains( EXPR_FILE ) )
                {
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
        final File DATA_FILE = new File( dir, buildFileNameFor( p ) );
        Writer writer = null;

        try {
            Log.i( LOG_TAG, "Storing: " + p + " to: " + DATA_FILE.getAbsolutePath() );
            writer = openWriterFor( TEMP_FILE );
            p.toJSON( writer );
            close( writer );
            if ( !TEMP_FILE.renameTo( DATA_FILE ) ) {
                Log.d( LOG_TAG, "Unable to move: " + DATA_FILE );
                Log.d( LOG_TAG, "Trying to copy: " + TEMP_FILE + " to: " + DATA_FILE );
                copy( TEMP_FILE, DATA_FILE );
            }
            this.updateCaches( p, DATA_FILE );
            Log.i( LOG_TAG, "Finished storing." );
        } catch(IOException exc) {
            final String ERROR_MSG = "I/O error writing: "
                            + DATA_FILE + ": " + exc.getMessage();
            Log.e( LOG_TAG, ERROR_MSG );
            throw new IOException( ERROR_MSG );
        } catch(JSONException exc) {
            final String ERROR_MSG = "error creating JSON for: "
                            + DATA_FILE + ": " + exc.getMessage();
            Log.e( LOG_TAG, ERROR_MSG );
            throw new IOException( ERROR_MSG );
        } finally {
          close( writer );
          if ( !TEMP_FILE.delete() ) {
              Log.e( LOG_TAG, "Error removing file: " + TEMP_FILE );
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
                                    "zip" + FileCache.FILE_NAME_PART_SEPARATOR
                                        + new Random().nextLong() );

        try {
            if ( !createDir( TEMP_DIR ) ) {
                throw new IOException( "fatal: unable to create temp dir" );
            }

            ZipUtil.unzip( zipFileIn, TEMP_DIR );

            final File[] ALL_FILES = getAllFilesInDir( TEMP_DIR );
            File experimentFile = null;
            final ArrayList<File> MEDIA_FILES = new ArrayList<>( ALL_FILES.length );
            final String EXPERIMENT_EXTENSION = EntitiesCache.getFileExtFor( Persistent.TypeId.Experiment );

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
     * @param res the result to export.
     * @throws IOException if something goes wrong, like a write fail.
     */
    public void exportResultToDownloads(Result res) throws IOException
    {
        final PlainStringEncoder ENC = PlainStringEncoder.get();
        final String ENCODED_REC = ENC.encode( res.getRec() );
        final String RES_FILE_NAME = buildFileNameFor( res );
        final String TAGS_FILE_NAME = ENCODED_REC + ".tags.txt";
        final String RR_FILE_NAME = ENCODED_REC + ".rr.txt";
        final File ORG_FILE = new File( this.dirDb, RES_FILE_NAME );
        final File ORG_FILE_COPY = new File( this.dirTmp,
                                        ENCODED_REC + "."
                                         + EntitiesCache.getFileExtFor( Persistent.TypeId.Result ) );

        this.store( res );                  // Ensure it is in the db

        try {
            final File TAGS_TEMP_FILE = new File( this.dirTmp, TAGS_FILE_NAME );
            final File RR_TEMP_FILE = new File( this.dirTmp, RR_FILE_NAME );
            final Writer TAGS_STREAM = openWriterFor( TAGS_TEMP_FILE );
            final Writer BEATS_STREAM = openWriterFor( RR_TEMP_FILE );

            // Create tag and beat files in temporary dir
            res.exportToStdTextFormat( TAGS_STREAM, BEATS_STREAM );
            close( TAGS_STREAM );
            close( BEATS_STREAM );
            copy( ORG_FILE, ORG_FILE_COPY );

            // Now save to downloads
            this.saveToDownloads( ORG_FILE_COPY, TEXT_MIME_TYPE );
            this.saveToDownloads( TAGS_TEMP_FILE, TEXT_MIME_TYPE );
            this.saveToDownloads( RR_TEMP_FILE, TEXT_MIME_TYPE );

            // Clean
            TAGS_TEMP_FILE.delete();
            RR_TEMP_FILE.delete();
        } catch(IOException exc) {
            throw new IOException( "exporting: '"
                            + RES_FILE_NAME
                            + "' to '/Download/': "
                            + exc.getMessage() );
        }

        return;
    }

    /** Exports a given experiment.
      * @param expr the experiment to export.
      * @throws IOException if something goes wrong, like a write fail.
      */
    public void exportExperimentToDownloads(Experiment expr) throws IOException
    {
        final String OUTPUT_NAME = PlainStringEncoder.get().encode( expr.getName() );
        final File OUTPUT_FILE = new File( this.dirTmp, OUTPUT_NAME + ".zip" );
        final File TEMP_FILE = this.createTempFile(
                expr.getTypeId().toString(),
                expr.getId().toString() );

        try {
            this.store( expr );
            final ArrayList<File> FILES =
                    new ArrayList<>(
                            Arrays.asList( this.collectMediaFilesFor( expr ) ) );

            FILES.add( new File( this.dirDb, buildFileNameFor( expr ) ) );

            ZipUtil.zip(
                    FILES.toArray( new File[ 0 ] ),
                    TEMP_FILE );
            copy( TEMP_FILE, OUTPUT_FILE );

            this.saveToDownloads( OUTPUT_FILE, ZIP_MIME_TYPE );
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

    /** Retrieves a file name from the cache of data files.
     * @param typeId the type of the object in the file (result, experiment...)
     * @param id the id of the object in the file.
     * @return the File object, null if not found.
     */
    public File getFileFor(Persistent.TypeId typeId, Id id)
    {
        return this.cache.get( typeId ).get( id );
    }

    /** Retrieves a single object from a table, given its id.
     * @param id The id of the object to retrieve.
     * @param typeId The id of the type of the object.
     * @return A Persistent object.
     * @see Persistent
     */
    public Persistent retrieve(Id id, Persistent.TypeId typeId) throws IOException
    {
        final File DATA_FILE = this.getFileFor( typeId, id );
        Reader reader = null;
        Persistent toret;

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
        final ArrayList<Pair<String, File[]>> TORET = new ArrayList<>();

        for(File subDir: getAllFilesInDir( this.dirRes )) {
            TORET.add( Pair.create( subDir.getName(), getAllFilesInDir( subDir ) ) );
        }

        return TORET;
    }

    /** Enumerates all Result objects for a given experiment.
     *  @return An array of partial objects.
     * @see PartialObject
     */
    public PartialObject[] enumerateResultsForExperiment(Id id) throws IOException
    {
        return enumerateObjects( Arrays.asList( this.resultsPerExperiment.get( id ) ) );
    }

    /** Enumerates all objects of a given type.
     *  @return An array of files.
     */
    public File[] enumerateFiles(Persistent.TypeId typeId)
    {
        return this.cache.get( typeId ).getValues();
    }

    /** Enumerates all objects of a given type.
     *  @return An array of partial objects.
     *  @see PartialObject
     */
    private PartialObject[] enumerateObjects(Persistent.TypeId typeId) throws IOException
    {
        final File[] FILES = this.cache.get( typeId ).getValues();
        return this.enumerateObjects( Arrays.asList( FILES ) );
    }

    /** Enumerates all objects of a given file list.
     *  @return An array of partial objects.
     *  @see PartialObject
     */
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

        return TORET.toArray( new PartialObject[ 0 ] );
    }

    /** @return A persistent object, given its id and the type of object. */
    private PartialObject lookForObjById(Id id, Persistent.TypeId typeId) throws IOException
    {
        return retrievePartialObject( this.cache.get( typeId ).get( id ) );
    }

    /** @return A persistent object, provided its name and the type of object. */
    private PartialObject lookForObjByName(String name, Persistent.TypeId typeId) throws IOException
    {
        final File[] FILES = this.cache.get( typeId ).getValues();

        PartialObject toret = null;

        for(File f: FILES) {
            final PartialObject OBJ = retrievePartialObject( f );

            if ( OBJ.getName().equals( name ) ) {
                toret = OBJ;
                break;
            }
        }

        return toret;
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

    /** @return the partial object loaded from file f. */
    private static PartialObject retrievePartialObject(File f) throws IOException
    {
        PartialObject toret;
        Reader reader = null;

        if ( f == null ) {
            throw new IOException( "retrievePartialObject(null): cannot do from null file" );
        }

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
                    StandardCharsets.UTF_8.newEncoder() );

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
                StandardCharsets.UTF_8.newDecoder() );

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
      * @param fileName the name of the target file
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
     * @param fileName the name of the destination file
     */
    void copyResFileAs(String resSubDir, File f, String fileName) throws IOException
    {
        File subDir = new File( this.dirRes, resSubDir );

        if ( createDir( subDir ) ) {
            copy( f, new File( subDir, fileName ) );
        } else {
            throw new IOException( "unable to create target dir: " + resSubDir );
        }

        return;
    }

    public void saveToDownloads(final File INPUT_FILE, String mimeTypeName)
    {
        final ContentValues VALUES = new ContentValues();
        final ContentResolver FINDER = this.context.getContentResolver();
        final InputStream IN;
        final OutputStream OUT;
        final Uri URI;

        VALUES.put( MediaStore.MediaColumns.DISPLAY_NAME, INPUT_FILE.getName() );
        VALUES.put( MediaStore.MediaColumns.MIME_TYPE, mimeTypeName );
        VALUES.put( MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS );

        if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q )
        {
            URI = FINDER.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    VALUES );
        } else {
            URI = FINDER.insert(
                    MediaStore.Files.getContentUri( "external" ),
                    VALUES );
        }

        if ( URI != null ) {
            try {
                IN = INPUT_FILE.toURI().toURL().openStream();
                OUT = FINDER.openOutputStream( URI );

                copy( IN, OUT );
            } catch(IOException exc) {
                Log.e( LOG_TAG, "saving to Downloads: " + exc.getMessage() );
            }
        } else {
            Log.e( LOG_TAG, "could not build MediaStore's URI" );
        }

        return;
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

    /** Returns a media file name suitable for storing in the database.
      * Media files are stored in the db with the same name they have outside,
      * but with an important caveat: it cannot have spaces inside.
      * @param fileName The name of the file.
      * @return A suitable file name.
      */
    public static String buildMediaFileNameForDbFromMediaFileName(String fileName)
    {
        // This is an extra indirection to avoid calling directly Tag.encode( fileName ).
        return plainStringEncoder.encode( fileName );
    }

    /** @return all files in the given dir, as an array.
      * @param dir The directory to inspect, as a File.
      */
    public static File[] getAllFilesInDir(File dir)
    {
        File[] toret = dir.listFiles();

        if ( toret == null ) {
            toret = new File[ 0 ];
        }

        return toret;
    }

    public static boolean createDir(File dir)
    {
        boolean toret = dir.mkdirs();

        if ( !toret ) {
            toret = dir.exists();
        }

        return toret;
    }

    /** Gets the already open database.
     * @return The Orm singleton object.
     */
    public static Ofm get()
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
    public static void init(Context context, PlainStringEncoder fileNameAdapter)
    {
        if ( instance == null ) {
            instance = new Ofm( context );
        }

        Ofm.plainStringEncoder = fileNameAdapter;
    }

    private final EntitiesCache cache;
    private final ResultsPerExperiment resultsPerExperiment;
    private File dirFiles;
    private File dirDb;
    private File dirRes;
    private File dirTmp;
    private final Context context;

    private static PlainStringEncoder plainStringEncoder;
    private static Ofm instance;
    private static File DIR_DOWNLOADS;
}

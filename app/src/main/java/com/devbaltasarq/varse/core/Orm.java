package com.devbaltasarq.varse.core;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.Log;

import com.devbaltasarq.varse.BuildConfig;
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
import java.util.Map;
import java.util.Random;


/** Relates the database of JSON files to objects. */
public final class Orm {
    private static final String LogTag = Orm.class.getSimpleName();

    private static final String FIELD_ID = Id.FIELD;
    public static final String FIELD_EXPERIMENT_ID = "experiment_id";
    public static final String FIELD_USER_ID = "user_id";
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

    private static final String DIR_DB = "db";
    private static final String DIR_RES = "res";
    private static final String DIR_MEDIA_PREFIX = "media";
    private static final String FILE_NAME_PART_SEPARATOR = "-";
    private static final String FIELD_EXT = "ext";
    public static final String FIELD_NAME = "name";
    private static final String REGULAR_FILE_FORMAT =
            "id" + FILE_NAME_PART_SEPARATOR + "$" + FIELD_ID + ".$" + FIELD_EXT;
    private static final String RESULT_FILE_FORMAT =
            "id" + FILE_NAME_PART_SEPARATOR + "$" + FIELD_ID
            + FILE_NAME_PART_SEPARATOR + FIELD_USER_ID
                    + FILE_NAME_PART_SEPARATOR + "$" + FIELD_USER_ID
            + FILE_NAME_PART_SEPARATOR + FIELD_EXPERIMENT_ID
                    + FILE_NAME_PART_SEPARATOR + "$" + FIELD_EXPERIMENT_ID
            + ".$" + FIELD_EXT;

    /** Prepares the ORM to operate. */
    private Orm(Context context) throws IOException
    {
        Log.i( LogTag, "Preparing store..." );

        this.createDirectories( context );
        this.createCache();

        Log.i( LogTag, "Store ready at: " + this.dirDb.getAbsolutePath() );
        Log.i( LogTag, "    #user files: " + this.filesUser.size() );
        Log.i( LogTag, "    #experiment files: " + this.filesExperiment.size() );
        Log.i( LogTag, "    #result files: " + this.filesResult.size() );
    }

    /** Creates the needed directories, if do not exist. */
    private void createDirectories(Context context)
    {
        this.dirDb = context.getDir( DIR_DB,  Context.MODE_PRIVATE );
        this.dirRes = context.getDir( DIR_RES,  Context.MODE_PRIVATE );
        this.dirTmp = context.getCacheDir();
    }

    /** Create the cache of files. */
    private void createCache()
    {
        final File[] fileList = this.dirDb.listFiles();
        this.filesUser = new HashSet<>();
        this.filesExperiment = new HashSet<>();
        this.filesResult = new HashSet<>();
        this.resultsPerExperiment = new HashMap<>();

        for (File f: fileList) {
            this.updateCache( f );
        }

        return;
    }

    private void updateCache(File f)
    {
        final Persistent.TypeId typeId = this.getTypeIdForExt( f );

        this.addToCache( f );

        if ( typeId == Persistent.TypeId.Result ) {
            final Id exprId = new Id( this.parseExperimentIdFromResultFile( f ) );
            ArrayList<File> resFileList = this.resultsPerExperiment.get( exprId );

            if ( resFileList == null ) {
                resFileList = new ArrayList<>();
                this.resultsPerExperiment.put( exprId, resFileList );
            }

            resFileList.add( f );
        }
    }

    private HashSet<File> getCacheForType(Persistent.TypeId typeId)
    {
        HashSet<File> toret = null;

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

    private void removeFromCache(File f)
    {
        this.getCacheForType( this.getTypeIdForExt( f ) ).remove( f );
    }

    private void addToCache(File f)
    {
        final HashSet<File> cachedList = this.getCacheForType( this.getTypeIdForExt( f ) );

        if ( !cachedList.contains( f ) ) {
            cachedList.add( f );
        }

        return;
    }

    /** @return true if the file exists in the cache, false otherwise. */
    private boolean existsInCache(File f)
    {
        return this.getCacheForType( this.getTypeIdForExt( f ) ).contains( f );
    }

    /** Decide the type for a file, following the extension.
     *
     * @param f the file to decide the type for.
     * @return the typeid for that file.
     * @see Persistent
     */
    private Persistent.TypeId getTypeIdForExt(File f)
    {
        final String ExtUser = this.getFileExtFor( Persistent.TypeId.User );
        final String ExtExperiment = this.getFileExtFor( Persistent.TypeId.Experiment );
        final String ExtResult = this.getFileExtFor( Persistent.TypeId.Result );
        Persistent.TypeId toret = null;

        if ( f.getName().endsWith( ExtUser ) ) {
            toret = Persistent.TypeId.User;
        }
        else
        if ( f.getName().endsWith( ExtExperiment ) ) {
            toret = Persistent.TypeId.Experiment;
        }
        else
        if ( f.getName().endsWith( ExtResult ) ) {
            toret = Persistent.TypeId.Result;
        }

        return toret;
    }

    /** @return the appropriate data file name for this object. */
    private String getFileNameFor(Persistent p)
    {
        return getFileNameFor( p, p.getTypeId() );
    }

    /** @return the appropriate data file name for this object. */
    private String getFileNameFor(Id id, Persistent.TypeId typeId)
    {
        if ( BuildConfig.DEBUG
          && typeId == Persistent.TypeId.Result )
        {
            throw new InternalError( "need more info building file name for result: " + id );
        }

        return REGULAR_FILE_FORMAT
                .replace( "$" + FIELD_ID, id.toString() )
                .replace( "$" + FIELD_EXT, this.getFileExtFor( typeId ) );
    }

    /** @return the appropriate data file name for this object. */
    private String getFileNameFor(Persistent p, Persistent.TypeId typeId)
    {
        String toret;

        if ( typeId == Persistent.TypeId.Result ) {
            Result res = (Result) p;

            toret = RESULT_FILE_FORMAT
                    .replace( "$" + FIELD_ID, p.getId().toString() )
                    .replace( "$" + FIELD_EXT, this.getFileExtFor( typeId ) )
                    .replace( "$" + FIELD_EXPERIMENT_ID, res.getExperiment().getId().toString() )
                    .replace( "$" + FIELD_USER_ID, res.getUser().getId().toString() );
        } else {
            toret = REGULAR_FILE_FORMAT
                    .replace( "$" + FIELD_ID, p.getId().toString() )
                    .replace( "$" + FIELD_EXT, this.getFileExtFor( typeId ) );
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
        final String fileName = file.getName();
        final int separatorPos = fileName.indexOf( FILE_NAME_PART_SEPARATOR );
        long toret = -1;

        if ( separatorPos >= 0 ) {
            int extSeparatorPos = fileName.lastIndexOf( '.' );

            if ( extSeparatorPos < 0 ) {
                extSeparatorPos = fileName.length();
            }

            final String id = fileName.substring( separatorPos + 1, extSeparatorPos );

            try {
                toret = Long.parseLong( id );
            } catch(NumberFormatException exc) {
                throw new Error( "parseIdFromFile: malformed id: " + id );
            }
        } else {
            throw new Error( "parseIdFromFile: separator not found in file name: " + fileName );
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
        final String fileName = f.getName();
        int exprIdPos = fileName.indexOf( FIELD_EXPERIMENT_ID );
        long toret = -1;

        if ( exprIdPos >= 0 ) {
            int extSeparatorPos = fileName.lastIndexOf( '.' );

            if ( extSeparatorPos < 0 ) {
                extSeparatorPos = fileName.length();
            }

            exprIdPos += 1 + FIELD_EXPERIMENT_ID.length();
            final String id = fileName.substring( exprIdPos, extSeparatorPos );

            try {
                toret = Long.parseLong( id );
            } catch(NumberFormatException exc) {
                throw new Error( "parseExperimentIdFromResultFile: malformed id: " + id );
            }
        } else {
            throw new Error( "parseExperimentIdFromResultFile: separator not found in file name: " + fileName );
        }

        return toret;
    }

    /** Removes object 'p' from the database.
      * @param p The object to remove.
      */
    public void remove(Persistent p)
    {
        Id id = p.getId();

        // Remove all associated objects
        for(File mediaFile: p.enumerateAssociatedFiles()) {
            final File f = new File( this.buildMediaDirectoryFor( p ),
                                mediaFile.getName() );

            f.delete();
            Log.d( LogTag, "Associated file deleted: " + mediaFile );
        }

        // Remove the whole directory for media, if it is an experiment.
        if ( p instanceof Experiment ) {
            final File mediaDir = this.buildMediaDirectoryFor( p );

            mediaDir.delete();
        }

        // Remove main object
        if ( id.isValid() ) {
            final File REMOVE_FILE = new File( this.dirDb, this.getFileNameFor( p ) );

            // Remove the main object
            this.removeFromCache( REMOVE_FILE );
            REMOVE_FILE.delete();

            Log.d( LogTag, "Object deleted: " + p.getId() );
        }

        return;
    }

    /** Determines the existence of an object in a table.
      * @param id The identifier of the object.
      * @param typeId The type id of the object.
      * @return true if exists, false otherwise.
      */
    public boolean exists(Id id, Persistent.TypeId typeId)
    {
        boolean toret = true;
        final File POSSIBLE_FILE = new File( this.dirDb, this.getFileNameFor( id, typeId ) );

        if ( this.existsInCache( POSSIBLE_FILE )
          || POSSIBLE_FILE.exists() )
        {
            Log.i( LogTag, "exists: found: " + POSSIBLE_FILE.getName() );
        } else {
            toret = false;
            Log.i( LogTag, "exists: not found: " + POSSIBLE_FILE.getName() );
        }

        return toret;
    }

    /** Builds the name of the experiment's media directory.
      * @param p The experiment itself.
      * @return the name of the directory, as a string.
      */
    public static String buildMediaDirectoryNameFor(@NonNull Persistent p)
    {
        final Experiment owner = p.getExperimentOwner();
        final Id id = owner != null ? owner.getId() : p.getId();

        return DIR_MEDIA_PREFIX + FILE_NAME_PART_SEPARATOR + id.get();
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
        // Time to make the id persistent, if needed
        // Otherwise, the experiment will have a true id, and the media file,
        // a different, fake id.
        if ( expr.getId().isFake() ) {
            expr.updateIds();
        }

        final File experimentDirectory = this.buildMediaDirectoryFor( expr );
        final File mediaFile = new File( experimentDirectory, fileName );

        experimentDirectory.mkdirs();
        if ( experimentDirectory.exists() ) {
            copy( inMedia, mediaFile );
        } else {
            final String errorMsg = "unable to create directory: " + experimentDirectory;
            throw new IOException( errorMsg );
        }

        return mediaFile;
    }

    /** Determines the existence of given media.
      * @param expr The experiment this media would pertain to.
      * @param fileName The file name of this media.
      * @return true if the media is already stored, false otherwise.
      */
    public boolean existsMedia(@NonNull Experiment expr, @NonNull String fileName)
    {
        final File experimentDirectory = this.buildMediaDirectoryFor( expr );
        final File mediaFile = new File( experimentDirectory, fileName );

        return mediaFile.exists() && ( expr.locateMediaActivity( fileName ) != null );
    }

    /** Deletes a media file in the app's file system.
     * @param expr the experiment this media file belongs to.
     * @param fileName The name of the file to remove.
     * @throws IOException if something goes wrong deleting, or if the file does not exist.
     */
    public void deleteMedia(@NonNull Experiment expr, @NonNull String fileName) throws IOException
    {
        final File experimentDirectory = this.buildMediaDirectoryFor( expr );
        final File mediaFile = new File( experimentDirectory, fileName );

        if ( mediaFile.exists() ) {
            mediaFile.delete();
        } else {
            final String errorMsg = "unable to find media file: " + fileName;
            throw new IOException( errorMsg );
        }

        return;
    }

    /** Collects the media files associated to a given experiment.
     * @param expr the experiment to collect the files from.
     * @return A new vector of File with the complete, absolute path.
     */
    private File[] collectMediaFilesFor(Experiment expr)
    {
        final File mediaDir = this.buildMediaDirectoryFor( expr );
        final File[] assocFiles = expr.enumerateAssociatedFiles();
        final File[] toret = new File[ assocFiles.length ];

        // Build the complete path of the associated files
        for(int i = 0; i < toret.length; ++i) {
            toret[ i ] = new File( mediaDir, assocFiles[ i ].getName() );
        }

        return toret;
    }

    /** Checks the associated files (media files) to an experiment and discards
      * the remaining ones.
      * @param expr The experiment to purge media files from.
      */
    public void purgeOrphanMediaFor(Experiment expr)
    {
        final File mediaDir = this.buildMediaDirectoryFor( expr );
        final File[] allFiles = mediaDir.listFiles();
        final ArrayList<File> registeredMediaFiles = new ArrayList<>(
                                        Arrays.asList( this.collectMediaFilesFor( expr ) ) );

        // Look for each registered file in the actual file list
        for(File f: allFiles) {
            if ( !registeredMediaFiles.contains( f ) ) {
                f.delete();
            }
        }

        return;
    }

    /** Removes all media that does not pertain to an existing Experiment. */
    public void purgeOrphanMedia()
    {
        final File[] mediaDirs = this.dirRes.listFiles();

        for(File mediaDir: mediaDirs) {
            final Id id = new Id( parseIdFromFile( mediaDir ) );
            File exprFile = new File( this.dirDb,
                                      this.getFileNameFor( id, Persistent.TypeId.Experiment ) );

            if ( !this.existsInCache( exprFile ) ) {
                removeTreeAt( mediaDir );
            }
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

    public void store(File dir, Persistent p) throws IOException
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
            Log.i( LogTag, "Storing: " + p.toString() + " to: " + DATA_FILE.getAbsolutePath() );
            writer = openWriterFor( TEMP_FILE );
            p.toJSON( writer );
            close( writer );
            TEMP_FILE.renameTo( DATA_FILE );
            this.updateCache( DATA_FILE );
            Log.i( LogTag, "Finished storing." );
        } catch(IOException exc) {
            final String msg = "I/O error writing: "
                            + DATA_FILE.toString() + ": " + exc.getMessage();
            Log.e( LogTag, msg );
            throw new IOException( msg );
        } catch(JSONException exc) {
            final String msg = "error creating JSON for: "
                            + DATA_FILE.toString() + ": " + exc.getMessage();
            Log.e( LogTag, msg );
            throw new IOException( msg );
        } finally {
          close( writer );
          TEMP_FILE.delete();
        }
    }

    /** Imports an experiment result from a JSON file, previously exported.
     * @param zipFileIn An input stream to the zip file.
     * @return The retrieved result.
     * @throws IOException if something goes wrong, like not enough space.
     */
    public Result importResult(InputStream zipFileIn) throws IOException
    {
        Result toret = null;

        try {
            toret = Result.fromJSON( openReaderFor( zipFileIn ) );

            // Store the result data set
            toret.updateIds();
            this.store( toret );
        } catch(JSONException exc) {
            Log.d( LogTag, "unable to import result file: " + exc.getMessage() );
        }

        return toret;
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

            final File[] allFiles = TEMP_DIR.listFiles();
            File experimentFile = null;
            final ArrayList<File> mediaFiles = new ArrayList<>( allFiles.length );
            final String EXPERIMENT_EXTENSION = this.getFileExtFor( Persistent.TypeId.Experiment );

            // Classify files
            for(File f: allFiles) {
                if ( extractFileExt( f ).equals( EXPERIMENT_EXTENSION ) ) {
                    experimentFile = f;
                } else {
                    mediaFiles.add( f );
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
            } catch(JSONException exc)
            {
                throw new IOException( "error reading JSON: " + exc.getMessage() );
            }

            // Store the experiment
            toret.updateIds();
            this.store( toret );

            // Prepare the media files
            for(File f: mediaFiles) {
                this.storeMedia( toret,
                                    f.getName(),
                                    new FileInputStream( f ) );
            }

        } finally {
            removeTreeAt( TEMP_DIR );
        }

        return toret;
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
            // Org
            final File ORG_FILE = new File( this.dirDb, RES_FILE_NAME );
            this.store( res );

            // Dest
            final File OUTPUT_FILE = new File( dir, RES_FILE_NAME + ".zip" );
            dir.mkdirs();
            copy( ORG_FILE, OUTPUT_FILE );
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
        final ArrayList<File> files = new ArrayList<>();
        final File TEMP_FILE = this.createTempFile(
                expr.getTypeId().toString(),
                expr.getId().toString() );

        if ( dir == null ) {
            dir = DIR_DOWNLOADS;
        }

        try {
            this.store( expr );
            files.addAll( Arrays.asList( this.collectMediaFilesFor( expr ) ) );
            files.add( new File( this.dirDb, this.getFileNameFor( expr ) ) );

            ZipUtil.zip(
                    files.toArray( new File[ 0 ] ),
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
            TEMP_FILE.delete();
        }

        return;
    }

    public User createOrRetrieveUser(String usrName) throws IOException
    {
        User toret = null;

        // Retrieve it, if possible
        try {
            toret = this.lookForUserByName( usrName );
        } catch(IOException exc)  {
            Log.d( LogTag, "unable to find user: " + usrName + ", creating it." );
        }

        // Create it
        if ( toret == null ) {
            toret = new User( Id.create(), usrName );
            this.store( toret );
        }

        return toret;
    }

    public User createOrRetrieveUser(Id userId)
    {
        User toret = null;

        // Retrieve it, if possible
        try {
            toret = (User) this.retrieve( userId, Persistent.TypeId.User );
        } catch(IOException exc)
        {
            Log.d( LogTag, "unable to find user: " + userId + ", creating it." );
        }

        // Create it
        if ( toret == null ) {
            toret = new User( userId, FIELD_USER_ID + "-" + userId );
        }

        return toret;
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
        final File DATA_FILE = new File( this.dirDb, this.getFileNameFor( id, typeId ) );
        Reader reader = null;

        try {
            reader = openReaderFor( DATA_FILE );

            toret = Persistent.fromJSON( typeId, reader );
            Log.i( LogTag, "Retrieved: " + toret.toString() + " from: "
                            + DATA_FILE.getAbsolutePath()  );
        } catch(IOException exc) {
            final String msg = "I/O error reading: "
                            + DATA_FILE.toString() + ": " + exc.getMessage();
            Log.e( LogTag, msg );
            throw new IOException( msg );
        } catch(JSONException exc) {
            final String msg = "error reading JSON for: "
                    + DATA_FILE.toString() + ": " + exc.getMessage();
            Log.e( LogTag, msg );
            throw new IOException( msg );
        } finally {
            this.close( reader );
        }

        return toret;
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
            fileList = new ArrayList<File>( 0 );
        }

        // Convert to partial objects
        final ArrayList<PartialObject> toret = new ArrayList<>( fileList.size() );

        for(File f: fileList) {
            toret.add( retrievePartialObject( f ) );
        }

        return toret.toArray( new PartialObject[ toret.size() ] );
    }

    /** @return A persistent object, provided its name and the type of object. */
    public PartialObject lookForObjByName(String name, Persistent.TypeId typeId) throws IOException
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
            final PartialObject obj = retrievePartialObject( f );

            if ( obj.getName().equals( name ) ) {
                toret = obj;
                break;
            }
        }

        return toret;
    }

    public User lookForUserByName(String name) throws IOException
    {
        final PartialObject user = this.lookForObjByName( name, Persistent.TypeId.User );

        return (User) this.retrieve( user.getId(), Persistent.TypeId.User );
    }

    public Experiment lookForExperimentByName(String name) throws IOException
    {
        final PartialObject expr = this.lookForObjByName( name, Persistent.TypeId.Experiment );

        return (Experiment) this.retrieve( expr.getId(), Persistent.TypeId.Experiment );

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
    public String[] enumerateObjNames(PartialObject[] objs) throws IOException
    {
        final String[] toret = new String[ objs.length ];

        for(int i = 0; i < objs.length; ++i) {
            toret[ i ] = objs[ i ].getName();
        }

        return toret;
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
            final String msg = "retrievePartialObject(f) reading JSON: " + exc.getMessage();
            Log.e( LogTag, msg );

            throw new IOException( msg );
        } finally {
            close( reader );
        }

        return toret;
    }

    public static Writer openWriterFor(File f) throws IOException
    {
        BufferedWriter toret = null;

        try {
            final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                    new FileOutputStream( f ),
                    Charset.forName( "UTF-8" ).newEncoder() );

            toret = new BufferedWriter( outputStreamWriter );
        } catch (IOException exc) {
            Log.e( LogTag,"Error creating writer for file: " + f );
            throw exc;
        }

        return toret;
    }

    public static Reader openReaderFor(File f) throws IOException
    {
        Reader toret = null;

        try {
            toret = openReaderFor( new FileInputStream( f ) );
        } catch (IOException exc) {
            Log.e( LogTag,"Error creating reader for file: " + f.getName() );
            throw exc;
        }

        return toret;
    }

    private static Reader openReaderFor(InputStream inStream)
    {
        final InputStreamReader inputStreamReader = new InputStreamReader(
                inStream,
                Charset.forName( "UTF-8" ).newDecoder() );

        return new BufferedReader( inputStreamReader );
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
            Log.e( LogTag, "closing writer" );
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
            Log.e( LogTag, "closing reader" );
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
            Log.e( LogTag, "closing json reader" );
        }
    }

    private static void copy(File source, File dest) throws IOException
    {
        final String errorMsg = "error copying " + source + " -> " + dest + ": ";
        InputStream is = null;
        OutputStream os = null;

        try {
            is = new FileInputStream( source );
            os = new FileOutputStream( dest );

            copy( is, os );
        } catch(IOException exc)
        {
            Log.e( LogTag, errorMsg + exc.getMessage() );
            throw new IOException( errorMsg );
        }

        return;
    }

    private static void copy(InputStream is, File dest) throws IOException
    {
        final String errorMsg = "error copying input stream -> " + dest + ": ";
        OutputStream os;

        try {
            os = new FileOutputStream( dest );

            copy( is, os );
        } catch(IOException exc)
        {
            Log.e( LogTag, errorMsg + exc.getMessage() );
            throw new IOException( errorMsg );
        }

        return;
    }

    private static void copy(InputStream is, OutputStream os) throws IOException
    {
        final byte[] buffer = new byte[1024];
        int length;

        try {
            while ( ( length = is.read( buffer ) ) > 0 ) {
                os.write( buffer, 0, length );
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
                Log.e( LogTag, "Copying file: error closing streams: " + exc.getMessage() );
            }
        }

        return;
    }

    private static void removeTreeAt(File dir)
    {
        if ( dir != null
          || !dir.isDirectory() )
        {
            final String[] allFiles = dir.list();

            for(String fileName: allFiles) {
                File f = new File( dir, fileName );

                if ( f.isDirectory() ) {
                    removeTreeAt( f );
                }

                f.delete();
            }

            dir.delete();
        } else {
            Log.d( LogTag, "removeTreeAt: directory null or not a directory?" );
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
        final int posDot = fileName.lastIndexOf(".");
        String toret = "";

        if ( posDot >= 0
          && posDot < ( fileName.length() - 1 ) )
        {
            toret = fileName.substring( posDot + 1 );
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

    /** Gets the already open database.
     * @return The Orm singleton object.
     */
    public static Orm get()
    {
        if ( instance == null ) {
            final String ERROR_MSG = "Orm database manager not created yet.";

            Log.e( LogTag, ERROR_MSG );
            throw new IllegalArgumentException( ERROR_MSG );
        }

        return instance;
    }

    /** Initialises the already open database.
     * @return The Orm singleton object.
     */
    public static void init(Context context) throws IOException
    {
        if ( instance == null ) {
            instance = new Orm( context );
        }

        return;
    }

    private HashSet<File> filesUser;
    private HashSet<File> filesExperiment;
    private HashSet<File> filesResult;
    private Map<Id, ArrayList<File>> resultsPerExperiment;
    private File dirDb;
    private File dirRes;
    private File dirTmp;
    private static Orm instance;
    private static File DIR_DOWNLOADS = Environment.getExternalStoragePublicDirectory(
                                                                Environment.DIRECTORY_DOWNLOADS );
}

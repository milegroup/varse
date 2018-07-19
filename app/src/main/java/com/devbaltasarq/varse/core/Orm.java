package com.devbaltasarq.varse.core;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.util.JsonReader;
import android.util.Log;

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
import java.util.HashSet;


/** Relates the database of JSON files to objects. */
public final class Orm {
    private static final String LogTag = Orm.class.getSimpleName();

    private static final String FIELD_ID = Id.FIELD;
    public static final String FIELD_EXPERIMENT_ID = "experiment_id";
    public static final String FIELD_USER_ID = "user_id";
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
    private static final String FIELD_EXT = "ext";
    public static final String FIELD_NAME = "name";
    private static final String FILE_FORMAT = "id-$" + FIELD_ID + ".$" + FIELD_EXT;

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


        for (File f: fileList) {
            this.getCacheForType( this.getTypeIdForExt( f ) ).add( f );
        }

        return;
    }

    private int calculateNumFiles()
    {
        return this.filesUser.size()
                + this.filesExperiment.size()
                + this.filesResult.size();
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

    /** Returns the extension for the corresponding data file name.
     * Note that it will be of three chars, lowercase.
     *
     * @param typeId the typeId id of the object.
     * @return the corresponding extension, as a string.
     * @see Persistent
     */
    private String getExtFor(Persistent.TypeId typeId)
    {
        String toret = typeId.toString().substring( 0, 3 ).toLowerCase();

        if ( typeId == Persistent.TypeId.User ) {
            toret = "usr";
        }

        return toret;
    }

    /** Decide the type for a file, following the extension.
     *
     * @param f the file to decide the type for.
     * @return the typeid for that file.
     * @see Persistent
     */
    private Persistent.TypeId getTypeIdForExt(File f)
    {
        final String ExtUser = this.getExtFor( Persistent.TypeId.User );
        final String ExtExperiment = this.getExtFor( Persistent.TypeId.Experiment );
        final String ExtResult = this.getExtFor( Persistent.TypeId.Result );
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
        return getFileNameFor( p.getId(), p.getTypeId() );
    }

    /** @return the appropriate data file name for this object. */
    private String getFileNameFor(Id id, Persistent.TypeId typeId)
    {
        return FILE_FORMAT
            .replace( "$" + FIELD_ID, id.toString() )
            .replace( "$" + FIELD_EXT, this.getExtFor( typeId ) );
    }

    /** Removes object 'p' from the database.
      * @param p The object to remove.
      */
    public void remove(Persistent p)
    {
        Id id = p.getId();

        if ( id.isValid() ) {
            try {
                final File REMOVE_FILE = new File( this.dirDb, this.getFileNameFor( p ) );

                this.removeFromCache( REMOVE_FILE );

                if ( !REMOVE_FILE.delete() ) {
                    throw new IOException( "could not delete: " + REMOVE_FILE.getName() );
                }

                Log.i( LogTag, "Object deleted: " + p.getId() );
            } catch(IOException exc) {
                Log.e( LogTag, "removing: " + id.toString() + ": " + exc.getMessage() );
            }
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
      * @param expr The experiment itself.
      * @return the name of the directory, as a string.
      */
    private static String buildExperimentMediaDirectoryNameFor(@NonNull Experiment expr)
    {
        return DIR_MEDIA_PREFIX + '-' + expr.getId().get();
    }

    /** Builds the directory for the experiment's media.
     * @param expr The experiment itself.
     * @return a File object for the experiment's media directory.
     */
    private File buildExperimentMediaDirectoryFor(@NonNull Experiment expr)
    {
        return new File(
                this.dirRes,
                buildExperimentMediaDirectoryNameFor( expr ) );
    }

    /** Stores a media file in the app's file system.
      * @param expr the experiment this media file belongs to.
      * @param inMedia an input stream the existing media file.
      * @return the new file, to the media file copied to the experiment's dir.
      * @throws IOException if something goes wrong copying, or if the dir could not be created.
      */
    public File storeMedia(@NonNull Experiment expr, @NonNull String fileName, @NonNull InputStream inMedia) throws IOException
    {
        final File experimentDirectory = this.buildExperimentMediaDirectoryFor( expr );
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
        final File experimentDirectory = this.buildExperimentMediaDirectoryFor( expr );
        final File mediaFile = new File( experimentDirectory, fileName );

        return mediaFile.exists() && ( expr.locateMediaActivity( fileName ) != null );
    }

    /** Deletes a media file in the app's file system.
     * @param expr the experiment this media file belongs to.
     * @param fileName The name of the file to remove.
     * @throws IOException if something goes wrong deleting, or if the file does not exist.
     */
    public File deleteMedia(@NonNull Experiment expr, @NonNull String fileName) throws IOException
    {
        final File experimentDirectory = this.buildExperimentMediaDirectoryFor( expr );
        final File mediaFile = new File( experimentDirectory, fileName );

        if ( mediaFile.exists() ) {
            mediaFile.delete();
        } else {
            final String errorMsg = "unable to find media file: " + fileName;
            throw new IOException( errorMsg );
        }

        return mediaFile;
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
            p.updateIds( this );
        }

        // Store the data
        final File TEMP_FILE = File.createTempFile(
                                    p.getTypeId().toString(),
                                    p.getId().toString(),
                                    this.dirTmp );
        final File DATA_FILE = new File( dir, this.getFileNameFor( p ) );
        Writer writer = null;

        try {
            Log.i( LogTag, "Storing: " + p.toString() + " to: " + DATA_FILE.getAbsolutePath() );
            writer = openWriterFor( TEMP_FILE );
            p.toJSON( writer );
            close( writer );
            copy( TEMP_FILE, DATA_FILE );
            this.addToCache( DATA_FILE );
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

    /** Exports a given experiment.
      * @param dir the directory to export the experiment to.
     *             If null, then Downloads is chosen.
      * @param expr the experiment to export.
      * @throws IOException if something goes wrong, like a write fail.
      */
    public void export(File dir, Experiment expr) throws IOException
    {
        final ArrayList<File> files = new ArrayList<>();
        final File TEMP_FILE = File.createTempFile(
                expr.getTypeId().toString(),
                expr.getId().toString(),
                this.dirTmp );

        if ( dir == null ) {
            dir = DIR_DOWNLOADS;
        }

        try {
            final File[] mediaFiles = expr.collectMediaFiles();

            this.store( expr );
            files.addAll( Arrays.asList( mediaFiles ) );
            files.add( new File( this.dirDb, this.getFileNameFor( expr ) ) );

            ZipUtil.zip(
                    files.toArray( new File[ 0 ] ),
                    TEMP_FILE );

            dir.mkdirs();
            copy( TEMP_FILE, new File( dir, this.getFileNameFor( expr ) + ".zip" ) );
        } catch(IOException exc)
        {
            throw new IOException(
                            "exporting: '"
                            + expr.getName()
                            + "': " + exc.getMessage() );
        } finally {
            TEMP_FILE.delete();
        }

        return;
    }

    /** Retrieves a single object from a table, given its id.
     * @param id The id of the object to retrieve.
     * @param typeId The id of the type of the object.
     * @return A Persistent object.
     * @see Persistent
     */
    public Persistent retrieve(Id id, Persistent.TypeId typeId) throws IOException
    {
        Persistent toret = null;
        final File DATA_FILE = new File( this.dirDb, this.getFileNameFor( id, typeId ) );
        Reader reader = null;

        try {
            reader = this.openReaderFor( DATA_FILE );

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

    /** Enumerates all objects of a given type.
     *  @return An array of pairs of Id's and Strings.
     *  @see Pair
     */
    @SuppressWarnings("unchecked")
    private PartialObject[] enumerateObjects(Persistent.TypeId typeId) throws IOException
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

        // Convert to partial objects
        final ArrayList<PartialObject> toret = new ArrayList<>( fileList.size() );

        for(File f: fileList) {
            toret.add( retrievePartialObject( f ) );
        }

        return toret.toArray( new PartialObject[ toret.size() ] );
    }

    /** @return A persistent object, provided its name and the type of object. */
    public Persistent lookForObjByName(String name, Persistent.TypeId typeId) throws IOException
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
        return (User) this.lookForObjByName( name, Persistent.TypeId.User );
    }

    public Experiment lookForExperimentByName(String name) throws IOException
    {
        return (Experiment) this.lookForObjByName( name, Persistent.TypeId.Experiment );
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
        return enumerateObjNames( Persistent.TypeId.Result );
    }

    /** @return the partial object loaded from file f. */
    private static PartialObject retrievePartialObject(File f) throws IOException
    {
        PartialObject toret = null;
        Reader reader = null;

        try {
            reader = openReaderFor( f );
            toret = PartialObject.fromJSON( reader );
        } catch(IOException exc)
        {
            final String msg = "retrievePartialObject(f) reading JSON: " + exc.getMessage();
            Log.e( LogTag, msg );

            throw new IOException( msg );
        } catch(JSONException exc)
        {
            final String msg = "retrievePartialObject(f) reading JSON: " + exc.getMessage();
            Log.e( LogTag, msg );

            throw new IOException( msg );
        } finally {
            close( reader );
        }

        return toret;
    }

    private static Writer openWriterFor(File f) throws IOException
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

    private static Reader openReaderFor(File f) throws IOException
    {
        BufferedReader toret = null;

        try {
            final InputStreamReader inputStreamReader = new InputStreamReader(
                    new FileInputStream( f ),
                    Charset.forName( "UTF-8" ).newDecoder() );

            toret = new BufferedReader( inputStreamReader );
        } catch (IOException exc) {
            Log.e( LogTag,"Error creating reader for file: " + f );
            throw exc;
        }

        return toret;
    }

    /** Closes a writer stream. */
    private static void close(Writer writer)
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
    private static void close(Reader reader)
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
    private File dirDb;
    private File dirRes;
    private File dirTmp;
    private static Orm instance;
    private static File DIR_DOWNLOADS = Environment.getExternalStoragePublicDirectory(
                                                                Environment.DIRECTORY_DOWNLOADS );
}

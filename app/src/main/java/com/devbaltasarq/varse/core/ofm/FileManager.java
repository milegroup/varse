// Varse (c) 2019/20 - 25 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.core.ofm;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.JsonReader;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Ofm;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.core.PlainStringEncoder;
import com.devbaltasarq.varse.core.Result;

import org.json.JSONException;


public class FileManager implements AutoCloseable {
    private static final String LOG_TAG = FileManager.class.getSimpleName();
    private static final String DIR_DB = "db";
    public static final String FIELD_EXT = "ext";
    public  static final String FILE_NAME_PART_SEPARATOR = "-";
    private static final String FILE_NAME_PART_MEDIA = "media";

    private FileManager(final Context CTX, final PlainStringEncoder ENCODER)
    {
        this.plainStringEncoder = ENCODER;
        this.ctx = CTX;
    }

    @Override
    public void close()
    {
        instance = null;
    }

    /** @return the tmp directory, as a File object. */
    private File getDirTmp()
    {
        return this.ctx.getCacheDir();
    }

    /**@return the datastore mapping directory, as a File object. */
    private File getDirDb()
    {
        return this.ctx.getDir( DIR_DB,  Context.MODE_PRIVATE );
    }

    /**@return the resources directory, as a File object. */
    private File getDirRes()
    {
        return this.ctx.getDir( DIR_DB,  Context.MODE_PRIVATE );
    }

    /** Removes all files in cache. */
    public void removeTempData()
    {
        if ( this.getDirTmp() != null ) {
            FileManager.removeTreeAt( this.getDirTmp() );
        }

        return;
    }


    /** Builds the directory for the experiment's media.
     * @param p The experiment itself.
     * @return a File object for the experiment's media directory.
     */
    public File buildMediaDirectoryFor(@NonNull Persistent p)
    {
        return new File(
                this.getDirRes(),
                buildMediaDirectoryNameFor( p ) );
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

            toret = Result.buildNameWith(
                                RES.getId(),
                                RES.getExperiment().getId(),
                                RES.getExperiment().getName(),
                                RES.getRec(),
                                RES.getTime() );
        } else {
            toret = buildFileNameFor( p.getTypeId(), p.getId() );
        }

        return toret;
    }

    public File getAbsolutePathFor(Persistent p)
    {
        return new File( this.getDirDb(), FileManager.buildFileNameFor( p ) );
    }

    /** Enumerates all Result entity ids for a given experiment.
      * @param EXPR the experiment to look results for.
      * @return A List of entity ids.
      * @see EntityId, enumResultsForExpr
      */
    public List<EntityId> enumerateResultsForExperiment(final Experiment EXPR)
    {
        return new ArrayList<>( this.enumResultsForExpr( EXPR ).keySet() );
    }

    /** Internal: enumerates all Result entity ids for a given experiment.
     * @param EXPR the experiment to look results for.
     * @return A List of entity ids.
     * @see EntityId
     */
    private Map<EntityId, File> enumResultsForExpr(final Experiment EXPR)
    {
        final Map<EntityId, File> ENTS = this.enumerateAllFiles( Persistent.TypeId.Result );
        final Map<EntityId, File> TORET = new HashMap<>( ENTS.size() / 2 );

        for(final EntityId RES_EID: ENTS.keySet()) {
            final File FILE = ENTS.get( RES_EID );

            if ( FILE != null ) {
                final String NAME = FILE.getName();
                final Id RES_EXPR_ID = new Id( Result.parseExprIdFromName( NAME ) );

                if ( RES_EXPR_ID.equals( EXPR.getId() ) ) {
                    TORET.put( RES_EID, FILE );
                }
            }
        }

        return TORET;
    }

    /** Removes object 'p' from the database.
     * @param p The object to remove.
     */
    public void remove(Persistent p)
    {
        if ( p instanceof Experiment expr ) {
            // Remove the whole directory for media
            removeTreeAt( this.buildMediaDirectoryFor( expr ) );

            // Remove all related results
            removeFiles( this.enumResultsForExpr( expr )
                            .values().stream().collect( Collectors.toList() ));
        }

        // Remove main object
        final File REMOVE_FILE = new File( this.getDirDb(), buildFileNameFor( p ) );

        if ( !REMOVE_FILE.delete() ) {
            Log.e( LOG_TAG, "Error removing file: " + REMOVE_FILE );
        }

        Log.d( LOG_TAG, "Object deleted: " + p.getId() );
    }

    private void removeFiles(final List<File> FILES)
    {
        for(final File FILE: FILES) {
            if ( FILE != null ) {
                if ( !FILE.delete() ) {
                    Log.e( LOG_TAG, "unable to remove file: " + FILE.getAbsolutePath() );
                }
            } else {
                final String MSG = "trying to delete (null) file";

                Log.e( LOG_TAG, MSG );
                throw new Error( MSG );
            }
        }

        return;
    }

    /** Builds the name of the experiment's media directory.
     * @param p The experiment itself.
     * @return the name of the directory, as a string.
     */
    private static String buildMediaDirectoryNameFor(@NonNull Persistent p)
    {
        final Experiment OWNER = p.getExperimentOwner();
        final Id ID = OWNER != null ? OWNER.getId() : p.getId();

        return FILE_NAME_PART_MEDIA + FILE_NAME_PART_SEPARATOR + ID.get();
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
    public List<File> collectMediaFilesFor(Experiment expr)
    {
        final File DIR_MEDIA = this.buildMediaDirectoryFor( expr );
        final List<File> TORET = expr.enumerateMediaFiles();

        // Build the complete path of the associated files
        TORET.replaceAll( assocFile -> new File( DIR_MEDIA, assocFile.getName() ));
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
            final List<File> REG_MEDIA_FILES = new ArrayList<>( this.collectMediaFilesFor( expr ) );

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

    /** Extracts the id from the root media dir for a given experiment.
     * Whatever this file is, the id between the '.' for the ext and the '-' separator.
     * @param file the file to extract the id from.
     * @return A long with the id.
     * @throws Error if the file name does not contain an id.
     */
    public static long parseIdFromMediaDir(File file)
    {
        final String FILE_NAME = removeFileNameExt( file.getName() );
        long toret;

        // Divide by separator
        final String[] PARTS = FILE_NAME.split( FILE_NAME_PART_SEPARATOR );

        // Chk basic formatting
        if ( PARTS.length >= 2
          && PARTS[ 0 ].equals( FILE_NAME_PART_MEDIA ) )
        {
            // Convert second part to long for the id.
            try {
                toret = Long.parseLong( PARTS[ 1 ] );
            } catch(NumberFormatException exc) {
                throw new Error( "parseIdFromFile: malformed id: " + PARTS[ 1 ] );
            }
        } else {
            throw new Error( "parseIdFromMediaDir: wrong file name formatting: " + FILE_NAME );
        }

        return toret;
    }

    /** Removes all media that does not pertain to an existing Experiment. */
    public void purgeOrphanMedia()
    {
        try {
            for(File mediaDir: getAllFilesInDir( this.getDirRes() )) {
                final Id ID = new Id( FileManager.parseIdFromMediaDir( mediaDir ) );
                final Map<EntityId, File> FILES = this.enumerateAllFiles( Persistent.TypeId.Experiment );

                // Find EntityID
                if ( FILES.get( EntityId.fromIdType( Persistent.TypeId.Experiment, ID ) ) == null )
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
        return File.createTempFile( prefix, suffix, this.getDirTmp() );
    }

    /** @return a list of the files in the resource's directory.
     *         Remember that files for each experiment are saved under subdir.
     *         This means we need to recurse subdirectories.
     */
    public List<Pair<String, List<File>>> enumerateResFiles()
    {
        final List<Pair<String, List<File>>> TORET = new ArrayList<>();

        for(File subDir: getAllFilesInDir( this.getDirRes() )) {
            TORET.add( Pair.create( subDir.getName(), getAllFilesInDir( subDir ) ) );
        }

        return TORET;
    }

    /** @return A persistent object, given its id and the type of object. */
    public EntityId lookForObjById(Persistent.TypeId typeId, Id id) throws IOException
    {
        final Map<EntityId, File> ALL_FILES = this.enumerateAllFiles( typeId );
        EntityId toret = null;

        for(final EntityId ENT_ID: ALL_FILES.keySet()) {
            if ( ENT_ID.getId().equals( id ) ) {
                toret = ENT_ID;
                break;
            }
        }

        return toret;
    }

    /** Enumerates all experiments, getting a vector containing their id's and names. */
    public List<EntityId> enumerateExperiments() throws IOException
    {
        return new ArrayList<>( this.enumerateAllFiles( Persistent.TypeId.Experiment ).keySet() );
    }

    /** Enumerates all object names, given an array of partial object. */
    public List<String> enumerateObjNames(List<EntityId> objs)
    {
        final List<String> TORET = new ArrayList<>( objs.size() );

        for(final EntityId ENT_ID: objs) {
            TORET.add( ENT_ID.getName() );
        }

        return TORET;
    }

    /** @return a mapping of EntityID to File. */
    private Map<EntityId, File> enumerateAllFiles()
    {
        return enumerateAllFiles( null );
    }

    /** Create a mapping from EntituId to File.
      * @param typeId the typeId to filter for, or null for all types.
      * @return a mapping of EntityID to File.
      */
    private Map<EntityId, File> enumerateAllFiles(Persistent.TypeId typeId)
    {
        final Map<EntityId, File> TORET = new HashMap<>();
        final List<File> FILES = getAllFilesInDir( this.getDirDb() );

        try {
            for(File f: FILES) {
                if ( !f.isDirectory() ) {
                    final EntityId ENT_ID = EntityId.fromFile( f );

                    if ( typeId == null
                      || ENT_ID.getTypeId() == typeId )
                    {
                        TORET.put( ENT_ID, f );
                    }
                }
            }
        } catch(IOException exc) {
            final String MSG = "unable to read file: " + exc.getMessage();
            Log.e( LOG_TAG, MSG );
            throw new Error( MSG );
        }

        return TORET;
    }

    /** Looks up a given file, given its type and id.
      * @param typeId the type of the entity's file.
      * @param id the id of the entity's file.
      * @return a File object, or null.
      */
    public File lookUpFile(Persistent.TypeId typeId, Id id)
    {
        final Map<EntityId, File> FILES = this.enumerateAllFiles( typeId );
        return FILES.get( new EntityId( typeId, id, EntityId.NO_NAME ) );
    }

    /** Stores a persistent entity.
      * @param p the persistent entity: an experiment, a result...
      * @throws IOException when saving goes south.
      */
    public void store(Persistent p) throws IOException
    {
        // Assign a real id
        if ( p.getId().isFake() ) {
            p.updateIds();
        }

        // Store the data
        final File DATA_FILE = new File( this.getDirDb(), FileManager.buildFileNameFor( p ) );
        final File TEMP_FILE = this.createTempFile(
                p.getTypeId().toString(),
                p.getId().toString() );
        Log.i( LOG_TAG, "Storing: " + p + " to: " + DATA_FILE.getAbsolutePath() );

        try (Writer writer = FileManager.openWriterFor( TEMP_FILE )) {
            p.toJSON( writer );
        } catch(IOException exc) {
            final String ERROR_MSG = "Ofm.store(): I/O error writing: "
                    + TEMP_FILE + ": " + exc.getMessage();
            Log.e( LOG_TAG, ERROR_MSG );
            throw new IOException( ERROR_MSG );
        } catch(JSONException exc) {
            final String ERROR_MSG = "Ofm.store(): error creating JSON for: "
                    + TEMP_FILE + ": " + exc.getMessage();
            Log.e( LOG_TAG, ERROR_MSG );
            throw new IOException( ERROR_MSG );
        }

        if ( !TEMP_FILE.renameTo( DATA_FILE ) ) {
            Log.d( LOG_TAG, "Unable to move: " + DATA_FILE );
            Log.d( LOG_TAG, "Trying to copy: " + TEMP_FILE + " to: " + DATA_FILE );
            FileManager.copy( TEMP_FILE, DATA_FILE );
        }

        Log.i( LOG_TAG, "Finished storing." );
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

    public static BufferedReader openReaderFor(InputStream inStream)
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
    public static void copy(File source, File dest) throws IOException
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
    public static void copy(InputStream is, File dest) throws IOException
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

    /** Returns a media file name suitable for storing in the database.
     * Media files are stored in the db with the same name they have outside,
     * but with an important caveat: it cannot have spaces inside.
     * @param fileName The name of the file.
     * @return A suitable file name.
     */
    public String buildMediaFileNameForDbFromMediaFileName(String fileName)
    {
        // This is an extra indirection to avoid calling directly Tag.encode( fileName ).
        return this.plainStringEncoder.encode( fileName );
    }

    /** Decide the type for a file, following the extension.
     * @param f the file to decide the type for.
     * @return the typeid for that file.
     * @see Persistent::TypeId
     */
    public static Persistent.TypeId getTypeIdForExt(File f)
    {
        final String EXT_EXPERIMENT = getFileExtFor( Persistent.TypeId.Experiment );
        final String EXT_RESULT = getFileExtFor( Persistent.TypeId.Result );
        Persistent.TypeId toret = null;

        if ( f.getName().endsWith( EXT_EXPERIMENT ) ) {
            toret = Persistent.TypeId.Experiment;
        }
        else
        if ( f.getName().endsWith( EXT_RESULT ) ) {
            toret = Persistent.TypeId.Result;
        }

        return toret;
    }

    /** Returns the extension for the corresponding type.
     * Note that it will be of three chars, lowercase.
     *
     * @param typeId the typeId id of the object.
     * @return the corresponding extension, as a string.
     * @see Persistent
     */
    public static String getFileExtFor(Persistent.TypeId typeId)
    {
        return typeId.toString().substring( 0, 3 ).toLowerCase();
    }

    private static final String REGULAR_FILE_FORMAT =
            Id.FILE_NAME_PART
                    + FILE_NAME_PART_SEPARATOR + "$" + Id.FILE_NAME_PART
                    + ".$" + FIELD_EXT;

    /** @return the file name for the an entity, given its id and type.
     * @param id The id object representing the if of the file.
     * @param typeId the type of the object.
     */
    public static String buildFileNameFor(Persistent.TypeId typeId, Id id)
    {
        return REGULAR_FILE_FORMAT
                .replace( "$" + Id.FILE_NAME_PART, id.toString() )
                .replace( "$" + FIELD_EXT, getFileExtFor( typeId ) );
    }

    /** Removes a whole dir and all its subdirectories, recursively.
     * @param dir The directory to remove.
     */
    public static void removeTreeAt(File dir)
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

    /** @return the given file name, with its extension removed.
     * @param FILE_NAME a file name, with or without extension. */
    public static String removeFileNameExt(final String FILE_NAME)
    {
        int extSeparatorPos = FILE_NAME.lastIndexOf( '.' );

        if ( extSeparatorPos < 0 ) {
            extSeparatorPos = FILE_NAME.length();
        }

        return FILE_NAME.substring( 0, extSeparatorPos );
    }

    /** @return all files in the given dir, as a List.
     * @param dir The directory to inspect, as a File.
     */
    public static List<File> getAllFilesInDir(File dir)
    {
        final File[] FILES = dir.listFiles();
        final List<File> TORET = new ArrayList<>();

        if ( FILES != null ) {
            TORET.addAll( Arrays.asList( FILES ) );
        }

        return TORET;
    }

    /** Creates a temporary directory, i.e., under the internal app's temp/
      * @param dirName the name of the dir. Never prefix subdirs!!
      * @return the created directory, or null if there was an error.
      */
    public File createTempDir(String dirName)
    {
        File toret = new File( this.getDirTmp(), dirName );

        if ( !createDir( toret ) ) {
            toret = null;
        }

        return toret;
    }

    /** Creates a file, given an absolute file.
      * @param absolutePath a File pointed by an absolute path.
      * @return true if the dir was created, false otherwise.
      */
    public static boolean createDir(File absolutePath)
    {
        boolean toret = absolutePath.mkdirs();

        if ( !toret ) {
            toret = absolutePath.exists();
        }

        return toret;
    }

    public void export(final Activity ACT, final File FILE, final String MIME_TYPE)
    {
        // Prepare
        final Intent SHARING_INTENT = new Intent( Intent.ACTION_SEND );
        final Uri FILE_URI = FileProvider.getUriForFile(
                this.ctx,
                "com.devbaltasarq.varse.fileprovider",
                FILE );

        SHARING_INTENT.setType( "application/zip" );
        SHARING_INTENT.putExtra( Intent.EXTRA_STREAM, FILE_URI );

        // Grant permissions
        SHARING_INTENT.addFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION );

        final List<ResolveInfo> RESOLVED_INFO_ACTIVITIES =
                this.ctx.getPackageManager().queryIntentActivities(
                        SHARING_INTENT,
                        PackageManager.MATCH_DEFAULT_ONLY );

        for (final ResolveInfo RI : RESOLVED_INFO_ACTIVITIES) {
            this.ctx.grantUriPermission(
                    RI.activityInfo.packageName,
                    FILE_URI,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION );
        }

        // Share the file
        ACT.startActivity( Intent.createChooser(
                SHARING_INTENT,
                this.ctx.getString( R.string.lblExport ) ) );

    }

    /** @return the only instance of this class. */
    public static FileManager create(Context ctx, PlainStringEncoder strEncoder)
    {
        if ( instance == null ) {
            instance = new FileManager( ctx, strEncoder );
        }

        return instance;
    }

    /** @return the only instance of this class. */
    public static FileManager get()
    {
        if ( instance == null ) {
            throw new Error( "FileManager not yet created!!" );
        }

        return instance;
    }

    private final PlainStringEncoder plainStringEncoder;
    private final Context ctx;
    private static FileManager instance;
}

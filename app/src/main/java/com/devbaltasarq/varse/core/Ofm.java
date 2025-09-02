// Varse (c) 2019/20 - 25 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.core;


import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import com.devbaltasarq.varse.core.experiment.Tag;
import com.devbaltasarq.varse.core.ofm.FileManager;


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
    public static final String FIELD_EXPR_ID = "exprid";

    private static final String ZIP_MIME_TYPE = "application/zip";

    /** Prepares the ORM to operate. */
    private Ofm()
    {
    }

    public void close()
    {
        fmanager.close();
        fmanager = null;
        instance = null;
    }

    /** @return the file manager being used. */
    public FileManager getFileManager()
    {
        return Ofm.fmanager;
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
        fileName = this.getFileManager().buildMediaFileNameForDbFromMediaFileName( fileName );

        final File DIR_EXPERIMENTS = fmanager.buildMediaDirectoryFor( expr );
        final File MEDIA_FILE = new File( DIR_EXPERIMENTS, fileName );

        if ( FileManager.createDir( DIR_EXPERIMENTS ) ) {
            FileManager.copy( inMedia, MEDIA_FILE );
        } else {
            final String MSG_ERROR = "unable to create directory: " + DIR_EXPERIMENTS;
            throw new IOException( MSG_ERROR );
        }

        return MEDIA_FILE;
    }

    /** Stores any data object.
     * @param p The persistent object to store.
     */
    public void store(Persistent p) throws IOException
    {
        fmanager.store( p );
    }

    /** Imports an experiment result from a JSON file, previously exported.
     * @param jsonFileIn An input stream to the zip file.
     * @throws IOException if something goes wrong, like not enough space.
     */
    public void importResult(InputStream jsonFileIn) throws IOException
    {
        try {
            final Reader FILE_READER = FileManager.openReaderFor( jsonFileIn );
            final Result TORET = Result.fromJSON( FILE_READER );
            FileManager.close( FILE_READER );

            // Store the result data set
            TORET.updateIds();
            this.store( TORET );
        } catch(JSONException exc) {
            final String ERROR_MSG = "unable to import result file: " + exc.getMessage();

            Log.e( LOG_TAG, ERROR_MSG );
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
        final String TEMP_DIR_NAME = "zip" + FileManager.FILE_NAME_PART_SEPARATOR
                                     + new Random().nextLong();
        final File TEMP_DIR = fmanager.createTempDir( TEMP_DIR_NAME );
        Experiment toret;

        if ( TEMP_DIR != null ) {
            try {
                ZipUtil.unzip( zipFileIn, TEMP_DIR );

                final List<File> ALL_FILES = FileManager.getAllFilesInDir( TEMP_DIR );
                File experimentFile = null;
                final List<File> MEDIA_FILES = new ArrayList<>( ALL_FILES.size() );
                final String EXPERIMENT_EXTENSION = FileManager.getFileExtFor( Persistent.TypeId.Experiment );

                // Classify files
                for(File f: ALL_FILES) {
                    if ( FileManager.extractFileExt( f ).equals( EXPERIMENT_EXTENSION ) ) {
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
                                    FileManager.openReaderFor( experimentFile ) );
                    this.chkIds( toret );
                } catch(JSONException exc)
                {
                    throw new IOException( "error reading JSON: " + exc.getMessage() );
                }

                // Store the experiment
                this.store( toret );
                for(File f: MEDIA_FILES) {
                    try (final var IN_STREAM = new FileInputStream( f )) {
                        this.storeMedia( toret,
                                f.getName(),
                                IN_STREAM );
                    }
                }
            } finally {
                FileManager.removeTreeAt( TEMP_DIR );
            }
        } else {
            throw new IOException( "fatal: unable to create temp dir: " + TEMP_DIR );
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
            if ( fmanager.lookForObjById( Persistent.TypeId.Experiment, ID ) != null ) {
                throw new JSONException( "already existing as experiment id:" + ID );
            }

            if ( fmanager.lookForObjById( Persistent.TypeId.Result, ID ) != null ) {
                throw new JSONException( "already existing as a result id:" + ID );
            }
        } catch(IOException exc) {
            throw new JSONException( exc.getMessage() );
        }

        return;
    }

    /** Exports a given experiment.
      * @param expr the experiment to export.
      * @throws IOException if something goes wrong, like a write fail.
      */
    public File createZipForExperiment(Experiment expr) throws IOException
    {
        final String OUTPUT_NAME =
                Persistent.TypeId.Experiment.toShortString()
                        + FileManager.FILE_NAME_PART_SEPARATOR
                        + PlainStringEncoder.get().encode( expr.getName() )
                        + FileManager.FILE_NAME_PART_SEPARATOR;
        final File TORET = fmanager.createTempFile( OUTPUT_NAME, ZipUtil.FILE_EXT );
        final File TEMP_FILE = fmanager.createTempFile(
                expr.getTypeId().toString(),
                expr.getId().toString() );

        try {
            final List<File> FILES =
                    new ArrayList<>( fmanager.collectMediaFilesFor( expr ) );

            FILES.add( fmanager.getAbsolutePathFor( expr ) );
            this.store( expr );             // Ensure it's saved.
            ZipUtil.zip(
                    FILES.toArray( new File[ 0 ] ),
                    TEMP_FILE );
            FileManager.copy( TEMP_FILE, TORET );
        } catch(IOException exc) {
            throw new IOException(
                            "exporting: '"
                            + expr.getName()
                            + "': " + exc.getMessage() );
        } finally {
            if ( !TEMP_FILE.delete() ) {
                Log.e( LOG_TAG, "Error removing: " + TEMP_FILE );
            }
        }

        return TORET;
    }

    /** Retrieves a single object from a table, given its id.
     * @param id The id of the object to retrieve.
     * @param typeId The id of the type of the object.
     * @return A Persistent object.
     * @see Persistent
     */
    public Persistent retrieve(@NonNull Id id, @NonNull Persistent.TypeId typeId) throws IOException
    {
        final File DATA_FILE = fmanager.lookUpFile( typeId, id );
        Persistent toret = null;

        if ( DATA_FILE != null ) {
            try (Reader reader = FileManager.openReaderFor( DATA_FILE )) {
                toret = Persistent.fromJSON( typeId, reader );
                Log.i( LOG_TAG, "Retrieved: " + toret.toString() + " from: "
                                + DATA_FILE.getAbsolutePath()  );
            } catch(IOException exc) {
                final String ERROR_MSG = "I/O error reading: "
                                + DATA_FILE.toString() + ": " + exc.getMessage();
                Log.e( LOG_TAG, ERROR_MSG );
                throw new IOException( ERROR_MSG );
            } catch(JSONException exc) {
                final String ERROR_MSG = "error reading JSON for: "
                        + DATA_FILE.toString() + ": " + exc.getMessage();
                Log.e( LOG_TAG, ERROR_MSG );
                throw new IOException( ERROR_MSG );
            }
        } else {
            throw new IOException("file not found for entity: " + id + "/" + typeId );
        }

        return toret;
    }

    public void remove(Persistent p)
    {
        this.getFileManager().remove( p );
    }

    /** Gets the already open database.
     * @return The Orm singleton object.
     */
    public static Ofm get()
    {
        if ( instance == null ) {
            final String ERROR_MSG = "Orm database manager not created yet.";

            Log.e( LOG_TAG, ERROR_MSG );
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
            instance = new Ofm();
            Ofm.fmanager = FileManager.create( context, fileNameAdapter );
        }
    }

    public static FileManager fmanager;
    private static Ofm instance;
}

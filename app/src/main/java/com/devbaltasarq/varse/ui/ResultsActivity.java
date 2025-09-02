// VARSE 2019/25 (c) Baltasar for MILEGroup MIT License <baltasarq@uvigo.es>


package com.devbaltasarq.varse.ui;


import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Ofm;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.core.Result;
import com.devbaltasarq.varse.core.experiment.Tag;
import com.devbaltasarq.varse.core.ofm.EntityId;
import com.devbaltasarq.varse.core.ofm.FileManager;
import com.devbaltasarq.varse.ui.adapters.ListViewResultArrayAdapter;
import com.devbaltasarq.varse.ui.showresult.ResultViewerActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ResultsActivity extends AppActivity {
    private final static String LOG_TAG = ResultsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_results );

        final Toolbar TOOLBAR = this.findViewById( R.id.toolbar );
        this.setSupportActionBar( TOOLBAR );

        final ImageButton BT_BACK = this.findViewById( R.id.btCloseResults );
        final ImageButton BT_IMPORT = this.findViewById( R.id.btImport );
        final Spinner CB_EXPERIMENTS = this.findViewById( R.id.cbExperiments );

        // Init
        this.backupFinished = true;
        this.dataStore = Ofm.get();
        this.dataStore.getFileManager().removeTempData();

        // Event handlers
        BT_BACK.setOnClickListener( v -> this.finish() );
        BT_IMPORT.setOnClickListener(
                v -> this.LAUNCH_FILE_PICKER.launch(
                                    new String[]{ "text/plain" } ) );
        CB_EXPERIMENTS.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                        ResultsActivity.this.onExperimentChosen( pos );
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        ResultsActivity.this.onExperimentChosen( 0 );
                    }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        try {
            this.loadExperimentsSpinner();
            this.loadResultsListView();
        } catch(Error err)
        {
            final String IO_ERROR = this.getString( R.string.errIO );

            this.showStatus( LOG_TAG, IO_ERROR );
            Log.d( LOG_TAG, IO_ERROR + ": " + err.getMessage() );
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        experiment = null;
    }

    @Override
    public boolean askBeforeLeaving()
    {
        return false;
    }

    @Override
    public void finish()
    {
        if ( !this.backupFinished ) {
            Toast.makeText( this,
                    this.getString( R.string.msgWaitForBackup ),
                    Toast.LENGTH_SHORT ).show();
        } else {
            super.finish();
        }

        return;
    }

    public void launchResultViewer(Result result)
    {
        ResultViewerActivity.result = result;
        final Intent GRPH_VIEWER_INTENT = new Intent( this, ResultViewerActivity.class );
        this.startActivity( GRPH_VIEWER_INTENT );
    }

    public void exportResult(Result res)
    {
        try {
            final FileManager FMAN = this.dataStore.getFileManager();
            final String RES_TYPE_NAME = Persistent.TypeId.Result.toShortString();
            final String FILE_NAME = RES_TYPE_NAME
                                        + FileManager.FILE_NAME_PART_SEPARATOR
                                        + res.getRec()
                                        + FileManager.FILE_NAME_PART_SEPARATOR
                                        + "rrs.txt";
            final File RR_TEMP_FILE = this.dataStore.getFileManager()
                                                .createTempFile( RES_TYPE_NAME, FILE_NAME );

            // Ensure it is in the store
            final Result RESULT = (Result) this.dataStore.retrieve( res.getId(),
                                                                    Persistent.TypeId.Result );

            // Write rr's to the tmp dir
            final Writer BEATS_STREAM = FileManager.openWriterFor( RR_TEMP_FILE );
            RESULT.exportToStdTextFormat( null, BEATS_STREAM );
            FileManager.close( BEATS_STREAM );

            // Rename and export
            final Path PATH_ORG = Paths.get( RR_TEMP_FILE.toURI() );
            final Path PATH_DEST = PATH_ORG.resolveSibling( FILE_NAME );

            Files.move( PATH_ORG, PATH_DEST, REPLACE_EXISTING );
            FMAN.export( this, PATH_DEST.toFile() );
        } catch(IOException exc) {
            this.showStatus( LOG_TAG,
                    this.getString( R.string.errExport )
                            + ": " + exc.getMessage() );
        }

        return;
    }

    public void deleteResult(Result res)
    {
        final AlertDialog.Builder DLG = new AlertDialog.Builder( this );

        DLG.setTitle( this.getString( R.string.lblDelete )
                        + " "
                        + this.getString( R.string.lblResult ).toLowerCase() );
        DLG.setMessage( this.getString( R.string.msgAreYouSure ) );
        DLG.setPositiveButton( R.string.lblDelete, (dlgIntf, i) -> {
            try {
                final Result RESULT = (Result) this.dataStore.retrieve( res.getId(), Persistent.TypeId.Result );

                this.dataStore.getFileManager().remove( RESULT );
                this.loadResultsListView();
            } catch(IOException exc) {
                final String ERR_MSG = this.getString( R.string.errDeleting );
                Log.e( LOG_TAG, ERR_MSG + ": " + exc.getMessage() );
                this.showStatus( LOG_TAG, ERR_MSG );
            }
        });
        DLG.setNegativeButton( R.string.lblNo, null );
        DLG.create().show();
    }

    /** Reads a given file.
      * @param uri the file to read from.
      * @return a String with the contents of the file.
      * @throws IOException if reading goes wrong.
      */
    @NonNull
    private String readTextFromUri(Uri uri) throws IOException
    {
        final StringBuilder TORET = new StringBuilder();
        final ContentResolver SOLVER = this.getContentResolver();
        final String UTF8_BOM = "\uFEFF";

        try (final BufferedReader INPUT =
                     new BufferedReader(
                            new InputStreamReader(
                                     SOLVER.openInputStream( uri ) )))
        {
            String line;

            while ( ( line = INPUT.readLine() ) != null ) {
                if ( line.startsWith( UTF8_BOM )) {
                    line = line.substring( UTF8_BOM.length() );
                }

                TORET.append( line );
                TORET.append( '\n' );
            }

            FileManager.close( INPUT );
        }

        return TORET.toString();
    }

    private void onImport(@NonNull final String RAW_DATA)
    {
        final List<Integer> DATA = Stream.of( RAW_DATA.split( "\n" ) )
                                    .map( s -> Integer.valueOf( s.trim() ) )
                                    .collect( Collectors.toList() );

        final Calendar DATE_TIME = Calendar.getInstance();
        final String STR_ISO_TIME = String.format(
                Locale.getDefault(),
                "-%04d-%02d-%02d",
                DATE_TIME.get( Calendar.YEAR ),
                DATE_TIME.get( Calendar.MONTH + 1 ),
                DATE_TIME.get( Calendar.DAY_OF_MONTH ));
        final Ofm OFM = Ofm.get();
        int totalMilliSeconds = DATA.stream().mapToInt( Integer::intValue ).sum();
        int totalSeconds = (int) Math.ceil( totalMilliSeconds / 1000.0 );

        // Create a suitable experiment
        final Experiment EXPR = Experiment.createSimpleExperiment(
                            new Duration( totalSeconds ) );

        // Create a suitable result
        final Result.Builder RES_BUILDER =
                new Result.Builder(
                        "import" + STR_ISO_TIME,
                        EXPR,
                        DATE_TIME.getTimeInMillis() );

        RES_BUILDER.add(
                new Result.ActivityChangeEvent( 0,
                        new Tag( "record" ) ));

        int millisOffset = 0;
        for(int rr: DATA) {
            RES_BUILDER.add( new Result.BeatEvent( millisOffset, rr ) );
            millisOffset += rr;
        }

        final Result RES = RES_BUILDER.build( totalMilliSeconds );

        try {
            OFM.store( EXPR );
            OFM.store( RES );
            this.onResume();
        } catch(IOException exc) {
            this.showStatus( LOG_TAG, this.getString( R.string.errIO ) );
        }
    }

    /** Reads the experiments' names from the Ofm. */
    private void loadExperimentsSpinner()
    {
        final Spinner CB_EXPERIMENTS = this.findViewById( R.id.cbExperiments );

        // Read experiments' names
        try {
            // Populate the experiments list
            this.experimentsList = this.dataStore.getFileManager().enumerateExperiments();

            // Spinner experiments
            final ArrayAdapter<String> ADAPTER_EXPERIMENTS =
                    new ArrayAdapter<>( this.getBaseContext(),
                                        android.R.layout.simple_spinner_item,
                                        this.experimentsList.stream().
                                                map(
                                            exp -> String.format(
                                                                    Locale.getDefault(),
                                                                    "%s (id: %s)",
                                                                    exp.getName(),
                                                                    exp.getId() ))
                                                .collect( Collectors.toList() ) );

            ADAPTER_EXPERIMENTS.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
            CB_EXPERIMENTS.setAdapter( ADAPTER_EXPERIMENTS );
            CB_EXPERIMENTS.setSelection( 0, false );

            if ( !this.experimentsList.isEmpty() ) {
                final EntityId ENT_ID = experimentsList.get( 0 );
                experiment = (Experiment) this.dataStore.
                                    retrieve( ENT_ID.getId(), Persistent.TypeId.Experiment );
            }
        } catch(IOException exc) {
            Log.e( LOG_TAG, "loadExperimentsSpinner(): " + exc.getMessage() );
        }

        return;
    }

    /** Loads the results for the given experiment. */
    private void loadResultsListView()
    {
        if ( experiment == null ) {
            return;
        }

        final TextView LBL_NO_ENTRIES = this.findViewById( R.id.lblNoEntries );
        final ListView LV_RESULTS = this.findViewById( R.id.lvResultItems );
        final List<EntityId> ALL_EXPR_RES_IDS =
                                    this.dataStore.getFileManager().
                                        enumerateResultsForExperiment( experiment );

        if ( !ALL_EXPR_RES_IDS.isEmpty() ) {
            final List<Result> ALL_EXPR_RES = new ArrayList<>( ALL_EXPR_RES_IDS.size() );

            // Sort by creation time, reversed (more recent before)
            try {
                for(EntityId eid: ALL_EXPR_RES_IDS) {
                    ALL_EXPR_RES.add(
                            (Result) this.dataStore.
                                            retrieve( eid.getId(), eid.getTypeId() ) );
                }
                ALL_EXPR_RES.sort( Comparator.comparingLong( Result::getTime ) );
            } catch(IOException exc) {
                this.showStatus( LOG_TAG, "error loading results: " + exc.getMessage() );
            }

            // Prepare the list view
            LV_RESULTS.setAdapter(
                            new ListViewResultArrayAdapter(
                                    this, ALL_EXPR_RES.toArray( new Result[ 0 ] ) ) );

            // Show the experiments list (or maybe not).

            LBL_NO_ENTRIES.setVisibility( View.GONE );
            LV_RESULTS.setVisibility( View.VISIBLE );
        } else {
            LBL_NO_ENTRIES.setVisibility( View.VISIBLE );
            LV_RESULTS.setVisibility( View.GONE );
        }

        return;
    }

    /** Select experiment
      * @param pos The position of the experiment in the list.
      */
    private void onExperimentChosen(int pos)
    {
        final EntityId ENT_ID = this.experimentsList.get( pos );

        try {
            experiment = (Experiment) this.dataStore.retrieve( ENT_ID.getId(),
                                                               Persistent.TypeId.Experiment );
        } catch(IOException exc) {
            throw new Error( "unable to retrieve experiment: " + exc.getMessage() );
        }

        this.loadResultsListView();
    }

    private final ActivityResultLauncher<String[]> LAUNCH_FILE_PICKER =
            this.registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(), uri -> {
                        final ResultsActivity SELF = ResultsActivity.this;

                        if ( uri != null ) {
                            SELF.showStatus(
                                    LOG_TAG,
                                    this.getString( R.string.lblImport )
                                            + "..." );

                            try {
                                SELF.onImport( SELF.readTextFromUri( uri ) );
                            } catch(IOException exc) {
                                SELF.showStatus( LOG_TAG,
                                        this.getString( R.string.lblImport )
                                        + ": "
                                        + this.getString( R.string.errIO ) );
                            }
                        }
                    });

    private boolean backupFinished;
    private List<EntityId> experimentsList;
    private Ofm dataStore;

    static Experiment experiment;
}

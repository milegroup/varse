// VARSE 2019/23 (c) Baltasar for MILEGroup MIT License <baltasarq@uvigo.es>


package com.devbaltasarq.varse.ui;


import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.DropboxUsrClient;
import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Ofm;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.core.Result;
import com.devbaltasarq.varse.core.Settings;
import com.devbaltasarq.varse.core.experiment.Tag;
import com.devbaltasarq.varse.core.ofmcache.PartialObject;
import com.devbaltasarq.varse.ui.adapters.ListViewResultArrayAdapter;
import com.devbaltasarq.varse.ui.showresult.ResultViewerActivity;
import com.dropbox.core.DbxException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
        this.dataStore.removeCache();

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

        this.loadExperimentsSpinner();
        this.loadResults();
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

    public void showResults(Result result)
    {
        try {
            result = (Result) this.dataStore.retrieve( result.getId(), Persistent.TypeId.Result );

            ResultViewerActivity.result = result;
            final Intent GRPH_VIEWER_INTENT = new Intent( this, ResultViewerActivity.class );
            this.startActivity( GRPH_VIEWER_INTENT );
        } catch(IOException exc) {
            this.showStatus(LOG_TAG, "unable to generate result data set" );
        }
    }

    public void exportResult(Result res)
    {
        try {
            final File RR_TEMP_FILE = new File( this.dataStore.getDirTmp(), "rrs.txt" );

            // Ensure it is in the store
            final Result RESULT = (Result) this.dataStore.retrieve(
                                                     res.getId(), Persistent.TypeId.Result );

            // Write rr's to the tmo dir
            final Writer BEATS_STREAM = Ofm.openWriterFor( RR_TEMP_FILE );
            RESULT.exportToStdTextFormat( null, BEATS_STREAM );
            Ofm.close( BEATS_STREAM );

            // Share file
            final Intent SHARING_INTENT = new Intent( Intent.ACTION_SEND );
            Uri screenshotUri = Uri.parse( RR_TEMP_FILE.getAbsolutePath() );
            SHARING_INTENT.setType( "text/plain" );
            SHARING_INTENT.putExtra( Intent.EXTRA_STREAM, screenshotUri );
            this.startActivity( Intent.createChooser(
                                    SHARING_INTENT,
                                    this.getString( R.string.lblExport ) ) );
        } catch(IOException exc) {
            this.showStatus( LOG_TAG,
                    this.getString( R.string.errExport )
                            + ": " + exc.getMessage() );
        }

        return;
    }

    public void uploadResult(Result res)
    {
        final ResultsActivity SELF = this;
        final ProgressBar PB_PROGRESS = this.findViewById( R.id.pbIndeterminateResultUpload );
        final Ofm OFM = this.dataStore;
        final String USR_EMAIL = Settings.get().getEmail();
        final DropboxUsrClient DBOX_SERVICE = new DropboxUsrClient( this, USR_EMAIL );

        this.handlerThread = new HandlerThread( "dropbox_backup" );
        this.handlerThread.start();
        this.handler = new Handler( this.handlerThread.getLooper() );

        this.backupFinished = false;
        PB_PROGRESS.setVisibility( View.VISIBLE );

        this.handler.post( () -> {
                try {
                    final Result RES = (Result) OFM.retrieve( res.getId(), Persistent.TypeId.Result );
                    final Experiment EXPR = RES.getExperiment();

                    // Collect files
                    File[] DATA_FILES = new File[] {
                            OFM.getFileFor( Persistent.TypeId.Result, RES.getId() ),
                            OFM.getFileFor( Persistent.TypeId.Experiment, EXPR.getId() ),
                    };

                    // Upload data
                    for(File f: DATA_FILES) {
                        DBOX_SERVICE.uploadDataFile( f );
                    }

                    // Upload exported files
                    final File EXPORT_FILE =
                            OFM.createTempFile( Id.FILE_NAME_PART
                                                + "_"
                                                + RES.getId() + "_",
                                                "_" + Ofm.FIELD_REC + "_"
                                                + RES.getRec() );
                    final Writer WRITER = Ofm.openWriterFor( EXPORT_FILE );
                    RES.exportToStdTextFormat( null, WRITER );
                    WRITER.close();
                    DBOX_SERVICE.uploadExportFile( EXPORT_FILE, RES.getId().get(), RES.getTime(), RES.getRec() );
                    SELF.runOnUiThread( () -> SELF.showStatus(LOG_TAG, SELF.getString( R.string.msgFinishedBackup ) ) );
                } catch (IOException | DbxException exc)
                {
                    SELF.runOnUiThread( () -> SELF.showStatus(LOG_TAG, SELF.getString( R.string.errIO ) ) );
                } finally {
                    SELF.runOnUiThread( () -> {
                        PB_PROGRESS.setVisibility( View.GONE );

                        SELF.handler.removeCallbacksAndMessages( null );
                        SELF.handlerThread.quit();
                        SELF.backupFinished = true;
                    });
                }
        });

        return;
    }

    public void deleteResult(Result res)
    {
        AlertDialog.Builder dlg = new AlertDialog.Builder( this );

        dlg.setTitle( this.getString( R.string.lblDelete )
                        + " " + this.getString( R.string.lblResult ).toLowerCase() );
        dlg.setMessage( this.getString( R.string.msgAreYouSure ) );
        dlg.setPositiveButton( R.string.lblDelete, (dlgIntf, i) -> {
            try {
                final Result RESULT = (Result) this.dataStore.retrieve( res.getId(), Persistent.TypeId.Result );

                this.dataStore.remove( RESULT );
                this.loadResults();
            } catch(IOException exc) {
                Log.e( LOG_TAG, this.getString( R.string.errDeleting) + ": " + exc.getMessage() );
                this.showStatus(LOG_TAG, this.getString( R.string.errDeleting) );
            }
        });
        dlg.setNegativeButton( R.string.lblNo, null );
        dlg.create().show();
    }

    /** Reads a given file.
      * @param uri the file to read from.
      * @return a String with the contents of the file.
      * @throws IOException if reading goes wrong.
      */
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

            Ofm.close( INPUT );
        }

        return TORET.toString();
    }

    private void onImport(final String RAW_DATA)
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
            this.experimentsList = this.dataStore.enumerateExperiments();

            // Spinner experiments
            final ArrayAdapter<PartialObject> ADAPTER_EXPERIMENTS =
                    new ArrayAdapter<>( this,
                                        android.R.layout.simple_spinner_item,
                                        this.experimentsList );
            ADAPTER_EXPERIMENTS.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
            CB_EXPERIMENTS.setAdapter( ADAPTER_EXPERIMENTS );
            CB_EXPERIMENTS.setSelection( 0, false );

            if ( this.experimentsList.length > 0 ) {
                experiment = experimentsList[ 0 ];
            }
        } catch(IOException exc) {
            this.showStatus( LOG_TAG, this.getString( R.string.errIO) );
        }

        return;
    }

    /** Loads the results for the given experiment. */
    private void loadResults()
    {
        if ( experiment == null ) {
            return;
        }

        try {
            final TextView LBL_NO_ENTRIES = this.findViewById( R.id.lblNoEntries );
            final ListView LV_RESULTS = this.findViewById( R.id.lvResultItems );
            final ArrayList<PartialObject> PO_ENTRIES =
                    new ArrayList<>( Arrays.asList(
                            this.dataStore.enumerateResultsForExperiment( experiment.getId() ) ) );

            // Sort by creation time, reversed (more recent before)
            PO_ENTRIES.sort( (po1, po2) -> Long.compare(
                    Result.parseTimeFromName( po2.getName() ),
                    Result.parseTimeFromName( po1.getName() ) )
            );

            // Prepare the list of experiments
            final int NUM_RESULT_ENTRIES = PO_ENTRIES.size();
            final Result[] RESULT_ENTRIES = new Result[ NUM_RESULT_ENTRIES ];

            for(int i = 0; i < NUM_RESULT_ENTRIES; ++i) {
                final PartialObject PO = PO_ENTRIES.get( i );
                String rec = "";

                try {
                    rec = Result.parseRecFromName( PO.getName() );
                } catch(Error e) {
                    // The record could not be parsed.
                    rec = "{rec}";
                }

                RESULT_ENTRIES[ i ] =
                            new Result( PO.getId(),
                                        Result.parseTimeFromName( PO.getName() ),
                                        0,
                                        rec,
                                        null, new Result.Event[ 0 ] );
            }

            // Prepare the list view
            LV_RESULTS.setAdapter( new ListViewResultArrayAdapter(this, RESULT_ENTRIES ) );

            // Show the experiments list (or maybe not).
            if ( RESULT_ENTRIES.length > 0 ) {
                LBL_NO_ENTRIES.setVisibility( View.GONE );
                LV_RESULTS.setVisibility( View.VISIBLE );
            } else {
                LBL_NO_ENTRIES.setVisibility( View.VISIBLE );
                LV_RESULTS.setVisibility( View.GONE );
            }
        } catch(IOException exc)
        {
            this.showStatus( LOG_TAG, this.getString( R.string.errIO) );
        }

        return;
    }

    /** Select experiment
      * @param pos The position of the experiment in the list.
      */
    private void onExperimentChosen(int pos)
    {
        experiment = this.experimentsList[ pos ];
        this.loadResults();
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
    private HandlerThread handlerThread;
    private Handler handler;
    private PartialObject[] experimentsList;
    private Ofm dataStore;

    static Persistent experiment;
}

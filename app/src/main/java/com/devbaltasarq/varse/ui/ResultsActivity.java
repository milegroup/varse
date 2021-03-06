package com.devbaltasarq.varse.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
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

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.DropboxUsrClient;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.PartialObject;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.core.Result;
import com.devbaltasarq.varse.core.Settings;
import com.devbaltasarq.varse.core.User;
import com.devbaltasarq.varse.ui.showresult.ResultViewerActivity;
import com.devbaltasarq.varse.ui.adapters.ListViewResultArrayAdapter;
import com.dropbox.core.DbxException;

import java.io.File;
import java.io.IOException;

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
        final Spinner CB_EXPERIMENTS = this.findViewById( R.id.cbExperiments );

        // Init
        this.backupFinished = true;
        this.dataStore = Orm.get();
        this.dataStore.removeCache();

        // Event handlers
        BT_BACK.setOnClickListener( (v) -> this.finish() );
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
        final String PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        final int RESULT_REQUEST = ContextCompat.checkSelfPermission( this, PERMISSION );

        try {
            if ( RESULT_REQUEST != PackageManager.PERMISSION_GRANTED ) {
                this.showStatus(LOG_TAG, this.getString( R.string.errPermissionDenied ) );
            } else {
                final Result RESULT = (Result) this.dataStore.retrieve( res.getId(), Persistent.TypeId.Result );
                this.doExportResult( RESULT );
            }
        } catch(IOException exc) {
            this.showStatus(LOG_TAG, this.getString( R.string.errExport) );
        }

        return;
    }

    public void uploadResult(Result res)
    {
        final ResultsActivity SELF = this;
        final ProgressBar PB_PROGRESS = this.findViewById( R.id.pbIndeterminateResultUpload );
        final Orm ORM = this.dataStore;
        final String USR_EMAIL = Settings.get().getEmail();
        final DropboxUsrClient DBOX_SERVICE = new DropboxUsrClient( this, USR_EMAIL );

        this.handlerThread = new HandlerThread( "dropbox_backup" );
        this.handlerThread.start();
        this.handler = new Handler( this.handlerThread.getLooper() );

        this.backupFinished = false;
        PB_PROGRESS.setVisibility( View.VISIBLE );

        this.handler.post( () -> {
                try {
                    final Result RES = (Result) Orm.get().retrieve( res.getId(), Persistent.TypeId.Result );

                    // Collect files
                    File[] allFiles = new File[] {
                            ORM.getFileById( RES.getId(), Persistent.TypeId.Result ),
                            ORM.getFileById( RES.getExperiment().getId(), Persistent.TypeId.Experiment )
                    };

                    // Upload them
                    for(File f: allFiles) {
                        DBOX_SERVICE.uploadDataFile( f );
                    }

                    SELF.runOnUiThread( () -> SELF.showStatus(LOG_TAG, SELF.getString( R.string.msgFinishedBackup ) ) );
                } catch (IOException | DbxException exc)
                {
                    SELF.runOnUiThread( () -> SELF.showStatus(LOG_TAG, SELF.getString( R.string.errIO ) ) );
                } finally {
                    SELF.runOnUiThread( () -> {
                        PB_PROGRESS.setVisibility( View.GONE );

                        ResultsActivity.this.handler.removeCallbacksAndMessages( null );
                        ResultsActivity.this.handlerThread.quit();
                        ResultsActivity.this.backupFinished = true;
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
                Log.e(LOG_TAG, this.getString( R.string.errDeleting) + ": " + exc.getMessage() );
                this.showStatus(LOG_TAG, this.getString( R.string.errDeleting) );
            }
        });
        dlg.setNegativeButton( R.string.lblNo, null );
        dlg.create().show();
    }

    private void doExportResult(Result result)
    {
        final String LBL_RESULT = this.getString( R.string.lblResult );

        try {
            this.dataStore.exportResult( null, result );
            this.showStatus(LOG_TAG, this.getString( R.string.msgExported ) + ": " + LBL_RESULT );
        } catch(IOException exc)
        {
            this.showStatus(LOG_TAG, this.getString( R.string.errExport) + ": " + LBL_RESULT );
        }

        return;
    }

    /** Reads the experiments' names from the ORM. */
    private void loadExperimentsSpinner()
    {
        final Spinner CB_EXPERIMENTS = this.findViewById( R.id.cbExperiments );

        // Read experiments' names
        try {
            // Populate the experiments list
            experimentsList = this.dataStore.enumerateExperiments();

            // Spinner experiments
            final ArrayAdapter<Persistent> ADAPTER_EXPERIMENTS =
                    new ArrayAdapter<>( this,
                                        android.R.layout.simple_spinner_item,
                                        experimentsList );
            ADAPTER_EXPERIMENTS.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
            CB_EXPERIMENTS.setAdapter( ADAPTER_EXPERIMENTS );
            CB_EXPERIMENTS.setSelection( 0, false );

            if ( experimentsList.length > 0 ) {
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
            final PartialObject[] PO_ENTRIES = dataStore.enumerateResultsForExperiment( experiment.getId() );

            // Prepare the list of experiments
            final Result[] RESULT_ENTRIES = new Result[ PO_ENTRIES.length ];

            for(int i = 0; i < PO_ENTRIES.length; ++i) {
                final PartialObject PO = PO_ENTRIES[ i ];
                final Id USR_ID = new Id( Result.parseUserIdFromName( PO.getName() ) );
                final User USR = this.dataStore.createOrRetrieveUserById( USR_ID );

                RESULT_ENTRIES[ i ] =
                            new Result( PO.getId(),
                                        Result.parseTimeFromName( PO.getName() ),
                                        0,
                                        USR, null, new Result.Event[ 0 ] );
            }

            // Prepare the list view
            LV_RESULTS.setAdapter( new ListViewResultArrayAdapter(this, new Result[]{} ) );
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
            this.showStatus(LOG_TAG, this.getString( R.string.errIO) );
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

    private boolean backupFinished;
    private HandlerThread handlerThread;
    private Handler handler;
    private Persistent[] experimentsList;
    private Orm dataStore;

    static Persistent experiment;
}

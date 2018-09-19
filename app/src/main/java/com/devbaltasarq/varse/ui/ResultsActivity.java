package com.devbaltasarq.varse.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.PartialObject;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.core.Result;
import com.devbaltasarq.varse.core.User;
import com.devbaltasarq.varse.ui.showresult.ResultViewerActivity;
import com.devbaltasarq.varse.ui.showresult.ListViewResultEntry;
import com.devbaltasarq.varse.ui.showresult.ListViewResultEntryArrayAdapter;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;

public class ResultsActivity extends AppActivity {
    private final static String LogTag = ResultsActivity.class.getSimpleName();
    private  static final int RQC_ASK_PERMISSION = 78;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_results );

        final Toolbar toolbar = this.findViewById( R.id.toolbar );
        this.setSupportActionBar( toolbar );

        final ImageButton btBack = this.findViewById( R.id.btCloseResults );
        final Spinner cbExperiments = this.findViewById( R.id.cbExperiments );

        // Init
        this.dataStore = Orm.get();
        this.dataStore.removeCache( this.getApplicationContext() );

        // Event handlers
        btBack.setOnClickListener( (v) -> this.finish() );
        cbExperiments.setOnItemSelectedListener(
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

        final Spinner spExperiments = this.findViewById( R.id.cbExperiments );
        Persistent expr = null;

        if ( experiment != null ) {
            expr = experiment;
        }

        this.loadExperimentsSpinner();

        if ( expr != null ) {
            experiment = expr;

            for(int i = 0; i < spExperiments.getAdapter().getCount(); ++i) {
                expr = (Persistent) spExperiments.getAdapter().getItem( i );

                if ( expr.getId().equals( experiment.getId() ) ) {
                    spExperiments.setSelection( i, false );
                    break;
                }
            }
        }

        this.loadResults();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        experiment = null;
    }

    public void showResults(Result result)
    {
        try {
            result = (Result) this.dataStore.retrieve( result.getId(), Persistent.TypeId.Result );

            // Prepare data files
            final File partialResultFileTags = this.dataStore.createTempFile( "partialFile1",
                                                                        result.getId().toString() );
            final File partialResultFileBeats = this.dataStore.createTempFile( "partialFile2",
                                                                        result.getId().toString() );
            final Writer tagsWriter = Orm.openWriterFor( partialResultFileTags );
            final Writer beatsWriter = Orm.openWriterFor( partialResultFileBeats );

            result.exportToStdTextFormat( tagsWriter, beatsWriter );
            Orm.close( tagsWriter );
            Orm.close( beatsWriter );

            // Launch data chart viewer
            ResultViewerActivity.beatsFile = partialResultFileBeats;
            ResultViewerActivity.tagsFile = partialResultFileTags;
            final Intent graphViewerIntent = new Intent( this, ResultViewerActivity.class );
            this.startActivity( graphViewerIntent );
        } catch(IOException exc) {
            this.showStatus( LogTag, "unable to generate result data set" );
        }
    }

    public void exportResult(Result res)
    {
        final String PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        final int RESULT_REQUEST = ContextCompat.checkSelfPermission( this, PERMISSION );

        try {
            final Result result = (Result) this.dataStore.retrieve( res.getId(), Persistent.TypeId.Result );

            if ( RESULT_REQUEST != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions( this,
                        new String[]{ PERMISSION },
                        RQC_ASK_PERMISSION );
            } else {
                this.doExportResult( result );
            }
        } catch(IOException exc) {
            this.showStatus( LogTag, this.getString( R.string.ErrExport ) );
        }

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
                final Result result = (Result) this.dataStore.retrieve( res.getId(), Persistent.TypeId.Result );

                this.dataStore.remove( result );
                this.loadResults();
            } catch(IOException exc) {
                Log.e( LogTag, this.getString( R.string.ErrDeleting ) + ": " + exc.getMessage() );
                this.showStatus( LogTag, this.getString( R.string.ErrDeleting ) );
            }
        });
        dlg.setNegativeButton( R.string.lblNo, null );
        dlg.create().show();
    }

    private void doExportResult(Result result)
    {
        final String lblResult = this.getString( R.string.lblResult );

        try {
            this.dataStore.exportResult( null, result );
            this.showStatus( LogTag, this.getString( R.string.msgExported ) + ": " + lblResult );
        } catch(IOException exc)
        {
            this.showStatus( LogTag, this.getString( R.string.ErrExport ) + ": " + lblResult );
        }

        return;
    }

    /** Reads the experiments' names from the ORM. */
    private void loadExperimentsSpinner()
    {
        final Spinner cbExperiments = this.findViewById( R.id.cbExperiments );

        // Read experiments' names
        try {
            // Populate the experiments list
            experimentsList = this.dataStore.enumerateExperiments();

            // Spinner experiments
            final ArrayAdapter<Persistent> adapterExperiments =
                    new ArrayAdapter<>( this,
                                        android.R.layout.simple_spinner_item,
                                        experimentsList );
            adapterExperiments.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
            cbExperiments.setAdapter( adapterExperiments );
            cbExperiments.setSelection( 0, false );

            if ( experimentsList.length > 0 ) {
                experiment = experimentsList[ 0 ];
            }
        } catch(IOException exc) {
            this.showStatus( LogTag, this.getString( R.string.ErrIO ) );
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
            final TextView lblNoEntries = this.findViewById( R.id.lblNoEntries );
            final ListView lvResults = this.findViewById( R.id.lvResultItems );
            final PartialObject[] poEntries = dataStore.enumerateResultsForExperiment( experiment.getId() );

            // Prepare the list of experiments
            final ListViewResultEntry[] resultEntries = new ListViewResultEntry[ poEntries.length ];

            for(int i = 0; i < poEntries.length; ++i) {
                final PartialObject po = poEntries[ i ];
                final Id userId = new Id( Result.parseUserIdFromName( po.getName() ) );
                final User user = this.dataStore.createOrRetrieveUserById( userId );

                resultEntries[ i ] =
                        new ListViewResultEntry(
                                new Result( po.getId(),
                                        Result.parseTimeFromName( po.getName() ),
                                        user, null  ) );
            }

            // Prepare the list view
            lvResults.setAdapter( new ListViewResultEntryArrayAdapter(this, resultEntries ) );

            // Show the experiments list (or maybe not).
            if ( poEntries.length > 0 ) {
                lblNoEntries.setVisibility( View.GONE );
                lvResults.setVisibility( View.VISIBLE );
            } else {
                lblNoEntries.setVisibility( View.VISIBLE );
                lvResults.setVisibility( View.GONE );
            }
        } catch(IOException exc)
        {
            this.showStatus( LogTag, this.getString( R.string.ErrIO ) );
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

    @Override
    public boolean askBeforeLeaving()
    {
        return false;
    }

    private Persistent[] experimentsList;
    private Orm dataStore;

    static Persistent experiment;
}

package com.devbaltasarq.varse.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
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
import com.devbaltasarq.varse.core.bluetooth.BluetoothDeviceWrapper;
import com.devbaltasarq.varse.ui.editexperiment.ListViewExperimentEntry;
import com.devbaltasarq.varse.ui.editexperiment.ListViewExperimentEntryArrayAdapter;
import com.devbaltasarq.varse.ui.performexperiment.PerformExperimentActivity;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ResultsActivity extends AppActivity {
    final static String LogTag = ResultsActivity.class.getSimpleName();

    private static class ExperimentWrapper {
        public ExperimentWrapper(PartialObject expr)
        {
            this.experiment = expr;
        }

        public PartialObject getExperiment()
        {
            return this.experiment;
        }

        @Override
        public String toString()
        {
            return this.experiment.getName();
        }

        private PartialObject experiment;
    }

    private static class ResultWrapper {
        public ResultWrapper(Context context, Orm orm, Result res)
        {
            this.result = res;
            this.context = context;
        }

        private Result getResult()
        {
            return this.result;
        }

        @Override
        public String toString()
        {
            final Result result = this.getResult();
            final Calendar localDate = Calendar.getInstance();

            localDate.setTimeInMillis( result.getTime() );
            return String.format(   "%04d-%02d-%02d %02d:%02d:%02d: "
                                        + context.getString( R.string.lblRecord )
                                        + ": " + result.getUser().getName(),
                                    localDate.get( Calendar.YEAR ),
                                    localDate.get( Calendar.MONTH ) + 1,
                                    localDate.get( Calendar.DAY_OF_MONTH ),
                                    localDate.get( Calendar.HOUR_OF_DAY ),
                                    localDate.get( Calendar.MINUTE ),
                                    localDate.get( Calendar.SECOND )
            );
        }

        private Context context;
        private Result result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_results );

        final Toolbar toolbar = this.findViewById( R.id.toolbar );
        this.setSupportActionBar( toolbar );

        final ImageButton btBack = this.findViewById( R.id.btCloseResults );
        final Spinner cbExperiments = this.findViewById( R.id.cbExperiments );
        final ListView lvResultItems = this.findViewById( R.id.lvResultItems );

        // Init
        this.dataStore = Orm.get();

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
        lvResultItems.setOnItemClickListener(
                (adptView, view, pos, l) -> this.showResults( pos ) );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Persistent expr = null;

        if ( experiment != null ) {
            expr = experiment;
        }

        this.loadExperimentsSpinner();

        if ( expr != null ) {
            experiment = expr;
        }

        this.loadResults();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        experiment = null;
    }

    private void showResults(int pos)
    {
        final ResultWrapper resultWrapper = this.resultWrapperEntries[ pos ];

        try {
            final Result result = (Result) this.dataStore.retrieve(
                                    resultWrapper.getResult().getId(),
                                    Persistent.TypeId.Result );

            // Prepare data files
            final File partialFile1 = this.dataStore.createTempFile( "partialFile1",
                                                                        result.getId().toString() );
            final File partialFile2 = this.dataStore.createTempFile( "partialFile2",
                                                                        result.getId().toString() );
            final Writer writer1 = Orm.openWriterFor( partialFile1 );
            final Writer writer2 = Orm.openWriterFor( partialFile2 );

            result.exportToStdTextFormat( writer1, writer2 );
            Orm.close( writer1 );
            Orm.close( writer2 );

            // Launch data chart viewer
        } catch(IOException exc) {
            this.showStatus( LogTag, "unable to load result data set" );
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
            PartialObject[] poList = this.dataStore.enumerateExperiments();
            this.experimentsList = new ExperimentWrapper[ poList.length ];
            int i = 0;
            for(PartialObject po: poList) {
                this.experimentsList[ i ] = new ExperimentWrapper( po );
                ++i;
            }

            // Spinner experiments
            final ArrayAdapter<ExperimentWrapper> adapterExperiments =
                    new ArrayAdapter<>( this,
                                        android.R.layout.simple_spinner_item,
                                        this.experimentsList );
            adapterExperiments.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
            cbExperiments.setAdapter( adapterExperiments );
            cbExperiments.setSelection( 0, false );

            if ( this.experimentsList.length > 0 ) {
                experiment = this.experimentsList[ 0 ].getExperiment();
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
            this.resultWrapperEntries = new ResultWrapper[ poEntries.length ];

            for(int i = 0; i < poEntries.length; ++i) {
                final PartialObject po = poEntries[ i ];
                final Id userId = new Id( Result.parseUserIdFromName( po.getName() ) );
                final User user = this.dataStore.createOrRetrieveUser( userId );

                this.resultWrapperEntries[ i ] =
                        new ResultWrapper(
                                this,
                                this.dataStore,
                                new Result( po.getId(),
                                            Result.parseTimeFromName( po.getName() ),
                                            user, null ) );
            }

            // Prepare the list view
            final ArrayAdapter<ResultWrapper> adptr =
                    new ArrayAdapter<ResultWrapper>( this,
                                                        android.R.layout.simple_list_item_1,
                                                        this.resultWrapperEntries );
            lvResults.setAdapter( adptr );

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
        experiment = this.experimentsList[ pos ].getExperiment();
        this.loadResults();
    }

    @Override
    public boolean askBeforeLeaving()
    {
        return false;
    }

    private ExperimentWrapper[] experimentsList;
    private ResultWrapper[] resultWrapperEntries;
    private Orm dataStore;

    static Persistent experiment;
}

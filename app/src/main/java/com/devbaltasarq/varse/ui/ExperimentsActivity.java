package com.devbaltasarq.varse.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.PartialObject;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.ui.editexperiment.EditExperimentActivity;
import com.devbaltasarq.varse.ui.editexperiment.ListViewExperimentEntry;
import com.devbaltasarq.varse.ui.editexperiment.ListViewExperimentEntryArrayAdapter;
import com.devbaltasarq.varse.ui.performexperiment.BluetoothLeScannerPerformExperimentActivity;

import java.io.IOException;
import java.util.ArrayList;

public class ExperimentsActivity extends AppActivity {
    public static final String LogTag = ExperimentsActivity.class.getSimpleName();
    public static final int RQC_ADD_EXPERIMENT = 76;
    public static final int RQC_EDIT_EXPERIMENT = 77;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_experiments );

        final Toolbar toolbar = this.findViewById( R.id.toolbar );
        this.setSupportActionBar( toolbar );

        final FloatingActionButton fab = this.findViewById(R.id.fbAddExperiment);
        final ImageButton btCloseExperiments = this.findViewById( R.id.btCloseExperiments );

        fab.setOnClickListener( (v) -> this.addExperiment() );
        btCloseExperiments.setOnClickListener( (v) -> this.finish() );

        this.setTitle( "" );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        this.showExperiments();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult( requestCode, resultCode, data );

        final ListView lvExperiments = this.findViewById( R.id.lvExperiments );

        try {
            if ( resultCode == RSC_SAVE_DATA ) {
                if ( requestCode == RQC_ADD_EXPERIMENT ) {
                    // Add new experiment
                    Orm.get().store( selectedExperiment );
                    this.experimentEntries.add( new ListViewExperimentEntry( selectedExperiment ) );
                }
                else
                if ( requestCode == RQC_EDIT_EXPERIMENT ) {
                    // Edit a given experiment
                    Orm.get().store( selectedExperiment );
                    this.substituteExperiment( selectedExperiment );
                }

                ( (ArrayAdapter) lvExperiments.getAdapter() ).notifyDataSetChanged();
            }
        } catch(IOException exc) {
            this.showStatus( LogTag, this.getString( R.string.ErrIO ) );
        }

        return;
    }

    private void showExperiments()
    {
        try {
            final TextView lblNoEntries = this.findViewById( R.id.lblNoEntries );
            final ListView lvExperiments = this.findViewById( R.id.lvExperiments );
            final PartialObject[] poEntries = Orm.get().enumerateExperiments();

            this.experimentEntries = new ArrayList<>( poEntries.length );

            if ( poEntries.length > 0 ) {
                lblNoEntries.setVisibility( View.GONE );
                lvExperiments.setVisibility( View.VISIBLE );

                for (int i = 0; i < poEntries.length; ++i) {
                    final PartialObject po = poEntries[ i ];

                    experimentEntries.add(
                            new ListViewExperimentEntry(
                                    new Experiment( po.getId(), po.getName() ) ) );
                }

                lvExperiments.setAdapter(
                        new ListViewExperimentEntryArrayAdapter(this, experimentEntries ) );
            } else {
                lblNoEntries.setVisibility( View.VISIBLE );
                lvExperiments.setVisibility( View.GONE );
            }
        } catch(IOException exc)
        {
            this.showStatus( LogTag, this.getString( R.string.ErrIO ) );
        }

        return;
    }

    /** Ensures the real experiment is loaded, and stored in selectedExperiment.
     * @param id the id if the experiment to load.
     */
    private void loadExperiment(Id id)
    {
        selectedExperiment = null;

        try {
            selectedExperiment =
                            (Experiment) Orm.get().retrieve( id, Persistent.TypeId.Experiment );
        } catch(IOException exc)
        {
            Log.e( LogTag, "error retrieving experiment: " + exc.getMessage() );
            this.showStatus( LogTag, this.getString( R.string.ErrIO ) );
        }
    }

    public void substituteExperiment(Experiment expr)
    {
        for(int i = 0; i < this.experimentEntries.size(); ++i)
        {
            final ListViewExperimentEntry foundExpr = this.experimentEntries.get( i );

            if ( foundExpr.getExperiment().getId().equals( expr.getId() ) ) {
                foundExpr.setExperiment( expr );
            }
        }

        return;
    }

    public void deleteExperiment(int position, Experiment e)
    {
        Orm.get().remove( e );
        this.experimentEntries.remove( position );
    }

    public void launchExperiment(Experiment e)
    {
        this.loadExperiment( e.getId() );

        if ( selectedExperiment != null ) {
            this.startActivity(
                new Intent( this, BluetoothLeScannerPerformExperimentActivity.class ) );
        }

        return;
    }

    public void addExperiment()
    {
        selectedExperiment =
            EditExperimentActivity.experiment = new Experiment( Id.createFake(), "expr" );

        this.startActivityForResult(
                new Intent( ExperimentsActivity.this, EditExperimentActivity.class ),
                RQC_ADD_EXPERIMENT);
    }

    public void editExperiment(Experiment e)
    {
        this.loadExperiment( e.getId() );

        if ( selectedExperiment != null ) {
            selectedExperiment = EditExperimentActivity.experiment = selectedExperiment.copy();

            this.startActivityForResult(
                new Intent( ExperimentsActivity.this, EditExperimentActivity.class ),
                RQC_EDIT_EXPERIMENT );
        }

        return;
    }

    public void exportExperiment(Experiment e)
    {
        final Orm db = Orm.get();

        try {
            db.export( null, e );
            this.showStatus( LogTag,
                    this.getString( R.string.msgExport )
                            + ": '" + e.getName() + '\'' );
        } catch(IOException exc)
        {
            this.showStatus( LogTag,
                    this.getString( R.string.ErrExport )
                    + ": '" + e.getName() + '\'' );
        }

        return;
    }

    @Override
    public boolean askBeforeLeaving()
    {
        return false;
    }

    public static Experiment selectedExperiment;
    private ArrayList<ListViewExperimentEntry> experimentEntries;
}

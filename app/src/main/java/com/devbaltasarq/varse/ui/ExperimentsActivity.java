package com.devbaltasarq.varse.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
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
import com.devbaltasarq.varse.ui.adapters.ListViewExperimentArrayAdapter;
import com.devbaltasarq.varse.ui.performexperiment.PerformExperimentActivity;

import java.io.IOException;
import java.util.ArrayList;

public class ExperimentsActivity extends AppActivity {
    public static final String LogTag = ExperimentsActivity.class.getSimpleName();
    public static final int RQC_ADD_EXPERIMENT = 76;
    public static final int RQC_EDIT_EXPERIMENT = 77;
    public static final int RQC_ASK_PERMISSION = 78;

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

        try {
            if ( resultCode == RSC_SAVE_DATA ) {
                if ( requestCode == RQC_ADD_EXPERIMENT ) {
                    // Add new experiment
                    Orm.get().store( selectedExperiment );
                    this.experimentEntries.add( selectedExperiment );
                }
                else
                if ( requestCode == RQC_EDIT_EXPERIMENT ) {
                    // Edit a given experiment
                    Orm.get().store( selectedExperiment );
                    this.substituteExperiment( selectedExperiment );
                }

                this.updateExperimentsList();

                // Erase all the media that is not registered (not needed).
                Orm.get().purgeOrphanMediaFor( selectedExperiment );
            } else {
                Orm.get().purgeOrphanMedia();
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

            // Prepare the list of experiments
            this.experimentEntries = new ArrayList<>( poEntries.length );

            for(PartialObject po: poEntries) {
                experimentEntries.add( new Experiment( po.getId(), po.getName() ) );
            }

            // Prepare the list view
            this.experimentsListAdapter =
                    new ListViewExperimentArrayAdapter(this, experimentEntries );
            lvExperiments.setAdapter( this.experimentsListAdapter );

            // Show the experiments list (or maybe not).
            if ( poEntries.length > 0 ) {
                lblNoEntries.setVisibility( View.GONE );
                lvExperiments.setVisibility( View.VISIBLE );
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

    private void updateExperimentsList()
    {
        final TextView lblNoEntries = this.findViewById( R.id.lblNoEntries );
        final ListView lvExperiments = this.findViewById( R.id.lvExperiments );

        this.experimentsListAdapter.notifyDataSetChanged();

        if ( this.experimentEntries.size() == 0 ) {
            lblNoEntries.setVisibility( View.VISIBLE );
            lvExperiments.setVisibility( View.GONE );
        } else {
            lblNoEntries.setVisibility( View.GONE );
            lvExperiments.setVisibility( View.VISIBLE );
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
        // Solve the entries issue
        for(int i = 0; i < this.experimentEntries.size(); ++i) {
            final Experiment exprItem = this.experimentEntries.get( i );

            if ( exprItem.getId().equals( expr.getId() ) ) {
                this.experimentEntries.set( i, expr );
            }
        }

        return;
    }

    public void deleteExperiment(int position, Experiment e)
    {
        AlertDialog.Builder dlg = new AlertDialog.Builder( this );

        dlg.setTitle( this.getString( R.string.lblDelete )
                + " " + this.getString( R.string.lblExperiment ).toLowerCase() );
        dlg.setMessage( this.getString( R.string.msgAreYouSure ) );
        dlg.setPositiveButton( R.string.lblDelete, (dlgIntf, i) -> {
            loadExperiment( e.getId() );

            if ( selectedExperiment != null ) {
                Orm.get().remove( selectedExperiment );
                this.experimentEntries.remove( position );
                this.updateExperimentsList();
            } else {
                this.showStatus( LogTag, this.getString( R.string.ErrDeleting )
                                            + ": " + e.toString() );
            }
        });
        dlg.setNegativeButton( R.string.lblNo, null );
        dlg.create().show();
    }

    public void launchExperimentResults(Experiment e)
    {
        final Intent showResultsIntent = new Intent( this, ResultsActivity.class );

        try {
             selectedExperiment = (Experiment)
                                  Orm.get().retrieve( e.getId(), Persistent.TypeId.Experiment );
        } catch(IOException exc) {
            this.showStatus( LogTag,
                             this.getString( R.string.msgFileNotFound )
                             + ": " + this.getString(R.string.lblExperiment ) );
        }

        ResultsActivity.experiment = selectedExperiment;
        this.startActivity( showResultsIntent );
    }

    public void launchExperiment(Experiment e)
    {
        this.loadExperiment( e.getId() );

        PerformExperimentActivity.chosenExperiment = selectedExperiment;
        if ( selectedExperiment != null ) {
            this.startActivity(
                new Intent( this, PerformExperimentActivity.class ) );
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
        final String PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        final int RESULT_REQUEST = ContextCompat.checkSelfPermission( this, PERMISSION );

        this.loadExperiment( e.getId() );

        if ( RESULT_REQUEST != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions( this,
                    new String[]{ PERMISSION },
                    RQC_ASK_PERMISSION );
        } else {
            this.doExportExperiment( selectedExperiment );
        }

        return;
    }

    private void doExportExperiment(Experiment e)
    {
        final String lblExperiment = this.getString( R.string.lblExperiment );
        final Orm db = Orm.get();

        try {
            db.exportExperiment( null, e );
            this.showStatus( LogTag,
                    this.getString( R.string.msgExported )
                            + ": " + lblExperiment
                            + ": '" + e.getName() + '\'' );
        } catch(IOException exc)
        {
            this.showStatus( LogTag,
                    this.getString( R.string.ErrExport )
                            + ": " + lblExperiment
                            + ": '" + e.getName() + '\'' );
        }

        return;
    }

    @Override
    public boolean askBeforeLeaving()
    {
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch ( requestCode ) {
            case RQC_ASK_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if ( grantResults.length > 0
                  && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED ) {
                    doExportExperiment( selectedExperiment );
                } else {
                    this.showStatus( LogTag, this.getString( R.string.ErrPermissionDenied ) );
                }
                return;
            }
        }

        return;
    }

    public static Experiment selectedExperiment;
    private ArrayList<Experiment> experimentEntries;
    private ListViewExperimentArrayAdapter experimentsListAdapter;
}

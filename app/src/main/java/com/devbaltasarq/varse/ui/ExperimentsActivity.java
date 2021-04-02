package com.devbaltasarq.varse.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
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
    public static final String LOG_TAG = ExperimentsActivity.class.getSimpleName();
    public static final int RQC_ADD_EXPERIMENT = 76;
    public static final int RQC_ADD_BY_TEMPLATE = 77;
    public static final int RQC_EDIT_EXPERIMENT = 78;
    public static final int RQC_ASK_PERMISSION = 79;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_experiments );

        final Toolbar TOOLBAR = this.findViewById( R.id.toolbar );
        this.setSupportActionBar( TOOLBAR );

        final FloatingActionButton FAB = this.findViewById(R.id.fbAddExperiment);
        final ImageButton BT_CLOSE_EXPERIMENTS = this.findViewById( R.id.btCloseExperiments );

        FAB.setOnClickListener( (v) -> this.addExperiment() );
        BT_CLOSE_EXPERIMENTS.setOnClickListener( (v) -> this.finish() );

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
                if ( requestCode == RQC_ADD_BY_TEMPLATE ) {
                    selectedExperiment = TemplatesActivity.selectedTemplate.create();
                    selectedExperiment.setName(
                            selectedExperiment.getName()
                            + "_" + this.experimentEntries.size() );
                    Orm.get().store( selectedExperiment );
                    this.experimentEntries.add( selectedExperiment );
                } else {
                    Orm.get().purgeOrphanMedia();
                }
            }
        } catch(IOException exc) {
            this.showStatus(LOG_TAG, this.getString( R.string.errIO) );
        }

        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );

        if ( requestCode == RQC_ASK_PERMISSION ) {
            // If request is cancelled, the result arrays are empty.
            if ( grantResults.length > 0
              && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED )
            {
                doExportExperiment( selectedExperiment );
            } else {
                this.showStatus(LOG_TAG, this.getString( R.string.errPermissionDenied) );
            }
        }

        return;
    }

    @Override
    public boolean askBeforeLeaving()
    {
        return false;
    }

    private void showExperiments()
    {
        try {
            final TextView LBL_NO_ENTRIES = this.findViewById( R.id.lblNoEntries );
            final ListView LV_EXPERIMENTS = this.findViewById( R.id.lvExperiments );
            final PartialObject[] PO_ENTRIES = Orm.get().enumerateExperiments();

            // Prepare the list of experiments
            this.experimentEntries = new ArrayList<>( PO_ENTRIES.length );

            for(PartialObject po: PO_ENTRIES) {
                experimentEntries.add( new Experiment( po.getId(), po.getName() ) );
            }

            // Prepare the list view
            this.experimentsListAdapter =
                    new ListViewExperimentArrayAdapter(this, experimentEntries );
            LV_EXPERIMENTS.setAdapter( this.experimentsListAdapter );

            // Show the experiments list (or maybe not).
            if ( PO_ENTRIES.length > 0 ) {
                LBL_NO_ENTRIES.setVisibility( View.GONE );
                LV_EXPERIMENTS.setVisibility( View.VISIBLE );
            } else {
                LBL_NO_ENTRIES.setVisibility( View.VISIBLE );
                LV_EXPERIMENTS.setVisibility( View.GONE );
            }
        } catch(IOException exc)
        {
            this.showStatus(LOG_TAG, this.getString( R.string.errIO) );
        }

        return;
    }

    private void updateExperimentsList()
    {
        final TextView LBL_NO_ENTRIES = this.findViewById( R.id.lblNoEntries );
        final ListView LV_EXPERIMENTS = this.findViewById( R.id.lvExperiments );

        this.experimentsListAdapter.notifyDataSetChanged();

        if ( this.experimentEntries.size() == 0 ) {
            LBL_NO_ENTRIES.setVisibility( View.VISIBLE );
            LV_EXPERIMENTS.setVisibility( View.GONE );
        } else {
            LBL_NO_ENTRIES.setVisibility( View.GONE );
            LV_EXPERIMENTS.setVisibility( View.VISIBLE );
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
            Log.e(LOG_TAG, "error retrieving experiment: " + exc.getMessage() );
            this.showStatus(LOG_TAG, this.getString( R.string.errIO) );
        }
    }

    public void substituteExperiment(Experiment expr)
    {
        // Solve the entries issue
        for(int i = 0; i < this.experimentEntries.size(); ++i) {
            final Experiment EXPR_ITEM = this.experimentEntries.get( i );

            if ( EXPR_ITEM.getId().equals( expr.getId() ) ) {
                this.experimentEntries.set( i, expr );
            }
        }

        return;
    }

    public void deleteExperiment(int position, Experiment e)
    {
        final AlertDialog.Builder DLG = new AlertDialog.Builder( this );

        DLG.setTitle( this.getString( R.string.lblDelete )
                + " " + this.getString( R.string.lblExperiment ).toLowerCase() );
        DLG.setMessage( this.getString( R.string.msgAreYouSure ) );
        DLG.setPositiveButton( R.string.lblDelete, (dlgIntf, i) -> {
            loadExperiment( e.getId() );

            if ( selectedExperiment != null ) {
                Orm.get().remove( selectedExperiment );
                this.experimentEntries.remove( position );
                this.updateExperimentsList();
            } else {
                this.showStatus(LOG_TAG, this.getString( R.string.errDeleting)
                                            + ": " + e.toString() );
            }
        });
        DLG.setNegativeButton( R.string.lblNo, null );
        DLG.create().show();
    }

    public void launchExperimentResults(Experiment e)
    {
        final Intent SHOW_RESULTS_INTENT = new Intent( this, ResultsActivity.class );

        try {
             selectedExperiment = (Experiment)
                                  Orm.get().retrieve( e.getId(), Persistent.TypeId.Experiment );
        } catch(IOException exc) {
            this.showStatus(LOG_TAG,
                             this.getString( R.string.msgFileNotFound )
                             + ": " + this.getString(R.string.lblExperiment ) );
        }

        ResultsActivity.experiment = selectedExperiment;
        this.startActivity( SHOW_RESULTS_INTENT );
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
        final IconListAlertDialog DLG = new IconListAlertDialog( this,
                                        R.drawable.ic_app_icon,
                                        R.string.lblExperiment,
                                        new int[]{ R.drawable.ic_ecg_button, R.drawable.ic_template },
                                        new int[]{ R.string.lblExperiment, R.string.lblTemplates } );

        DLG.setItemClickListener( (AdapterView<?> adapterView, View view, int i, long l) -> {
            DLG.dismiss();

            if ( i == 0 ) {
                selectedExperiment =
                        EditExperimentActivity.experiment = new Experiment( Id.createFake(), "expr" );

                this.startActivityForResult(
                        new Intent( ExperimentsActivity.this, EditExperimentActivity.class ),
                        RQC_ADD_EXPERIMENT );
            } else {
                selectedExperiment = null;

                this.startActivityForResult(
                        new Intent( ExperimentsActivity.this, TemplatesActivity.class ),
                        RQC_ADD_BY_TEMPLATE );
            }
        });

        DLG.show();
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
        final String LBL_EXPERIMENT = this.getString( R.string.lblExperiment );
        final Orm DB = Orm.get();

        try {
            DB.exportExperiment( null, e );
            this.showStatus(LOG_TAG,
                    this.getString( R.string.msgExported )
                            + ": " + LBL_EXPERIMENT
                            + ": '" + e.getName() + '\'' );
        } catch(IOException exc)
        {
            this.showStatus(LOG_TAG,
                    this.getString( R.string.errExport)
                            + ": " + LBL_EXPERIMENT
                            + ": '" + e.getName() + '\'' );
        }

        return;
    }

    public static Experiment selectedExperiment;
    private ArrayList<Experiment> experimentEntries;
    private ListViewExperimentArrayAdapter experimentsListAdapter;
}

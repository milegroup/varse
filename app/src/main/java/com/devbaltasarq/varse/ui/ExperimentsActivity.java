package com.devbaltasarq.varse.ui;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Ofm;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.core.ofmcache.PartialObject;
import com.devbaltasarq.varse.ui.adapters.ListViewExperimentArrayAdapter;
import com.devbaltasarq.varse.ui.editexperiment.EditExperimentActivity;
import com.devbaltasarq.varse.ui.performexperiment.PerformExperimentActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;


public class ExperimentsActivity extends AppActivity {
    public static final String LOG_TAG = ExperimentsActivity.class.getSimpleName();
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
                this.showStatus( LOG_TAG, this.getString( R.string.errPermissionDenied) );
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
            final PartialObject[] PO_ENTRIES = Ofm.get().enumerateExperiments();

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
                            (Experiment) Ofm.get().retrieve( id, Persistent.TypeId.Experiment );
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
                Ofm.get().remove( selectedExperiment );
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
                                  Ofm.get().retrieve( e.getId(), Persistent.TypeId.Experiment );
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

                LAUNCH_ADD.launch(
                        new Intent(
                                ExperimentsActivity.this,
                                EditExperimentActivity.class ) );
            } else {
                selectedExperiment = null;
                this.LAUNCH_ADD_BY_TEMPLATE.launch(
                        new Intent( this, TemplatesActivity.class ) );
            }
        });

        DLG.show();
    }

    public void editExperiment(Experiment e)
    {
        this.loadExperiment( e.getId() );

        if ( selectedExperiment != null ) {
            selectedExperiment = EditExperimentActivity.experiment = selectedExperiment.copy();

            this.LAUNCH_EDIT.launch(
                    new Intent( this, EditExperimentActivity.class ) );
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
        final Ofm DB = Ofm.get();

        try {
            DB.exportExperimentToDownloads( e );
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
    private final ActivityResultLauncher<Intent> LAUNCH_ADD =
        this.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if ( result.getResultCode() == RSC_SAVE_DATA ) {
                        try {
                            Ofm.get().store( selectedExperiment );
                            this.experimentEntries.add( selectedExperiment );
                            this.updateExperimentsList();
                        } catch(IOException exc) {
                            this.showStatus( LOG_TAG, this.getString( R.string.errIO ) );
                        }
                    } else {
                        Ofm.get().purgeOrphanMediaFor( selectedExperiment );
                    }
        });

    private final ActivityResultLauncher<Intent> LAUNCH_EDIT =
            this.registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if ( result.getResultCode() == RSC_SAVE_DATA ) {
                            try {
                                // Edit a given experiment
                                Ofm.get().store( selectedExperiment );
                                this.substituteExperiment( selectedExperiment );
                                this.updateExperimentsList();
                            } catch(IOException exc) {
                                this.showStatus( LOG_TAG, this.getString( R.string.errIO ) );
                            }
                        } else {
                            Ofm.get().purgeOrphanMediaFor( selectedExperiment );
                        }
                    });

    private final ActivityResultLauncher<Intent> LAUNCH_ADD_BY_TEMPLATE =
            this.registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        try {
                            selectedExperiment = TemplatesActivity.selectedTemplate.create();
                            selectedExperiment.setName(
                                    selectedExperiment.getName()
                                            + "_" + this.experimentEntries.size() );
                            Ofm.get().store( selectedExperiment );
                            this.experimentEntries.add( selectedExperiment );
                        } catch(IOException exc) {
                            this.showStatus( LOG_TAG, this.getString( R.string.errIO ) );
                        }
                    });
}

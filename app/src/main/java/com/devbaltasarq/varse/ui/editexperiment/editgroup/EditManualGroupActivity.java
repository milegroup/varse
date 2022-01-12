package com.devbaltasarq.varse.ui.editexperiment.editgroup;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.core.experiment.ManualGroup;


public class EditManualGroupActivity extends EditGroupActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView( R.layout.activity_edit_manual_group );

        final ImageButton btCloseEditManualGroup = this.findViewById( R.id.btCloseEditMediaGroup );
        final FloatingActionButton btAddAct = this.findViewById( R.id.fbAddManualActivity );
        final FloatingActionButton btSaveManualGroup = this.findViewById( R.id.fbSaveManualGroup );

        btAddAct.setOnClickListener( (v) -> this.addActivity() );
        btCloseEditManualGroup.setOnClickListener( (v) -> this.finishWithResultCode( RSC_DISMISS_DATA ) );
        btSaveManualGroup.setOnClickListener( (v) -> this.finishWithResultCode( RSC_SAVE_DATA ) );

        this.setTitle( "" );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        this.showActivities();
    }

    @Override
    public void addActivity()
    {
        EditManualEntryActivity.manualActivity = new ManualGroup.ManualActivity();

        this.LAUNCH_ADD.launch(
                new Intent( this, EditManualEntryActivity.class ) );
    }

    @Override
    public void editActivity(Group.Activity act)
    {
        EditManualEntryActivity.manualActivity = (ManualGroup.ManualActivity) act.copy();

        this.LAUNCH_EDIT.launch(
                new Intent( this, EditManualEntryActivity.class ) );
    }

    private final ActivityResultLauncher<Intent> LAUNCH_ADD =
            this.registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if ( result.getResultCode() == RSC_SAVE_DATA ) {
                            group.add( EditManualEntryActivity.manualActivity );
                            this.showActivities();
                        }
                    });

    private final ActivityResultLauncher<Intent> LAUNCH_EDIT =
            this.registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if ( result.getResultCode() == RSC_SAVE_DATA ) {
                            group.substituteActivity( EditManualEntryActivity.manualActivity );
                            this.showActivities();
                        }
                    });

}

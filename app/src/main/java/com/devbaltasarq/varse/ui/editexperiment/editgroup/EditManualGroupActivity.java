package com.devbaltasarq.varse.ui.editexperiment.editgroup;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.core.experiment.ManualGroup;


public class EditManualGroupActivity extends EditGroupActivity {
    private static final int RQC_EDIT_ACTIVITY = 801;
    private static final int RQC_ADD_ACTIVITY = 802;

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
    @SuppressWarnings("unchecked")
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult( requestCode, resultCode, data );

        if ( resultCode == RSC_SAVE_DATA ) {
            if ( requestCode == RQC_EDIT_ACTIVITY ) {
                group.substituteActivity( EditManualEntryActivity.manualActivity );
            }
            else
            if ( requestCode == RQC_ADD_ACTIVITY ) {
                group.add( EditManualEntryActivity.manualActivity );
            }

            this.showActivities();
        }

        return;
    }

    @Override
    public void addActivity()
    {
        EditManualEntryActivity.manualActivity = new ManualGroup.ManualActivity();

        this.startActivityForResult(
                new Intent( this, EditManualEntryActivity.class ),
                RQC_ADD_ACTIVITY );
    }

    @Override
    public void editActivity(Group.Activity act)
    {
        EditManualEntryActivity.manualActivity = (ManualGroup.ManualActivity) act.copy();

        this.startActivityForResult(
                new Intent( this, EditManualEntryActivity.class ),
                RQC_EDIT_ACTIVITY );
    }
}

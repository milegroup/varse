package com.devbaltasarq.varse.ui.editexperiment;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.core.experiment.ManualGroup;
import com.devbaltasarq.varse.core.experiment.PictureGroup;
import com.devbaltasarq.varse.core.experiment.VideoGroup;
import com.devbaltasarq.varse.ui.AppActivity;
import com.devbaltasarq.varse.ui.adapters.ListViewGroupArrayAdapter;
import com.devbaltasarq.varse.ui.editexperiment.editgroup.EditManualGroupActivity;
import com.devbaltasarq.varse.ui.editexperiment.editgroup.EditMediaGroupActivity;
import com.devbaltasarq.varse.ui.IconListAlertDialog;

public class EditExperimentActivity extends AppActivity {
    private final int RQC_ADD_GROUP = 112;
    private final int RQC_EDIT_GROUP = 113;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_edit_experiment );

        final FloatingActionButton FB_ADD_GROUP = this.findViewById( R.id.fbAddGroup );
        final FloatingActionButton FB_SAVE_EXPERIMENT = this.findViewById( R.id.fbSaveExperiment );
        final ImageButton BT_CLOSE_EDIT_EXPERIMENT = this.findViewById( R.id.btCloseEditExperiment );
        final TextView ED_EXPERIMENT_NAME = this.findViewById( R.id.edExperimentName );
        final CheckBox CHK_RANDOM = this.findViewById( R.id.chkRandom );

        FB_ADD_GROUP.setOnClickListener( (v) -> this.addNewGroup() );
        FB_SAVE_EXPERIMENT.setOnClickListener( (v) -> this.finishWithResultCode( RSC_SAVE_DATA ) );
        BT_CLOSE_EDIT_EXPERIMENT.setOnClickListener( (v) -> this.finishWithResultCode( RSC_DISMISS_DATA ) );
        CHK_RANDOM.setOnCheckedChangeListener( (button, booleanValue) -> this.onRandomChanged() );
        ED_EXPERIMENT_NAME.addTextChangedListener( new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {

            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                EditExperimentActivity.this.onNameChanged( ED_EXPERIMENT_NAME.getText().toString() );
            }
        });

        return;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        this.fillInData();
        this.showGroups();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult( requestCode, resultCode, data );

        if ( resultCode == RSC_SAVE_DATA ) {
            if ( requestCode == RQC_EDIT_GROUP ) {
                experiment.substituteGroup( selectedGroup );
            }
            else
            if ( requestCode == RQC_ADD_GROUP ) {
                experiment.addGroup( selectedGroup );
            }

            this.showGroups();
        }

        selectedGroup = null;
        return;
    }

    /** Fills the data in the fields. */
    private void fillInData()
    {
        final TextView ED_EXPERIMENT_NAME = this.findViewById( R.id.edExperimentName );
        final CheckBox CHK_RANDOM = this.findViewById( R.id.chkRandom );

        ED_EXPERIMENT_NAME.setText( experiment.getName() );
        CHK_RANDOM.setChecked( experiment.isRandom() );
    }

    private void showGroups()
    {
        final Group[] GROUPS = experiment.getGroups();
        final int NUM_ENTRIES = GROUPS.length;
        final ListView LV_EXPERIMENT_MEDIA = this.findViewById( R.id.lvExperimentMedia );
        final TextView LBL_NO_ENTRIES = this.findViewById( R.id.lblNoEntries );

        if ( NUM_ENTRIES > 0 ) {
            // Create adapter
            LV_EXPERIMENT_MEDIA.setAdapter(
                    new ListViewGroupArrayAdapter( this, GROUPS ) );

            LBL_NO_ENTRIES.setVisibility( View.GONE );
            LV_EXPERIMENT_MEDIA.setVisibility( View.VISIBLE );
        } else {
            LBL_NO_ENTRIES.setVisibility( View.VISIBLE );
            LV_EXPERIMENT_MEDIA.setVisibility( View.GONE );
        }

        return;
    }

    public void deleteGroup(Group m)
    {
        selectedGroup = null;
        experiment.removeGroup( m );
        this.showGroups();
    }

    protected void addNewGroup()
    {
        final IconListAlertDialog NEW_GROUP_DLG = new IconListAlertDialog( this,
                R.drawable.ic_group_button,
                R.string.lblAddGroup,
                new int[] {
                        R.drawable.ic_manual,
                        R.drawable.ic_picture,
                        R.drawable.ic_video
                },
                new int[] {
                        R.string.lblGroupManual,
                        R.string.lblGroupImages,
                        R.string.lblGroupVideos,
                });

        NEW_GROUP_DLG.setItemClickListener( (adpt, v, op, l) -> {
                    Intent launchData = new Intent( EditExperimentActivity.this,
                                                            EditMediaGroupActivity.class );

                    if ( op == 0 ) {
                        launchData = new Intent( EditExperimentActivity.this,
                                EditManualGroupActivity.class );
                        selectedGroup =
                                EditManualGroupActivity.group =
                                        new ManualGroup( Id.createFake(), this.experiment );
                    }
                    else
                    if ( op == 1 ) {
                        selectedGroup =
                                EditMediaGroupActivity.group =
                                        new PictureGroup( Id.createFake(), this.experiment );
                    }
                    else
                    if ( op == 2 ) {
                        selectedGroup =
                                EditMediaGroupActivity.group =
                                        new VideoGroup( Id.createFake(), this.experiment );
                    }

                    NEW_GROUP_DLG.dismiss();
                    EditExperimentActivity.this.startActivityForResult( launchData, RQC_ADD_GROUP) ;
        });

        NEW_GROUP_DLG.show();
    }

    public void editGroup(Group group)
    {
        Intent launchData;

        EditManualGroupActivity.group = selectedGroup = group.copy();

        if ( group instanceof ManualGroup ) {
            launchData = new Intent( this, EditManualGroupActivity.class );
        } else {
            launchData = new Intent( this, EditMediaGroupActivity.class );
        }

        this.startActivityForResult( launchData, RQC_EDIT_GROUP );
    }

    public void sortGroupUp(Group g)
    {
        experiment.sortGroupUp( g );
        this.showGroups();
    }

    public void sortGroupDown(Group g)
    {
        experiment.sortGroupDown( g );
        this.showGroups();
    }

    /** Manages the clicking of the "random" checkbox. */
    private void onRandomChanged()
    {
        experiment.setRandom( !experiment.isRandom() );
    }

    /** Change the name of the experiment, if possible.
      * @param newName the new name for the experiment.
      */
    private void onNameChanged(String newName)
    {
        if ( newName != null ) {
            newName = newName.trim();

            if ( !newName.isEmpty() ) {
                experiment.setName( newName );
            }
        }

        return;
    }

    public static Experiment experiment;
    public static Group selectedGroup;
}

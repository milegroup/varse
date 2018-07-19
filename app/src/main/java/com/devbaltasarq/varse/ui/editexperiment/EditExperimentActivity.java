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
import com.devbaltasarq.varse.ui.editexperiment.editgroup.EditManualGroupActivity;
import com.devbaltasarq.varse.ui.editexperiment.editgroup.EditMediaGroupActivity;
import com.devbaltasarq.varse.ui.util.IconListAlertDialog;

public class EditExperimentActivity extends AppActivity {
    private final int RQC_ADD_GROUP = 112;
    private final int RQC_EDIT_GROUP = 113;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_edit_experiment );

        final FloatingActionButton fabAddGroup = this.findViewById( R.id.fbAddGroup );
        final FloatingActionButton fabSaveExperiment = this.findViewById( R.id.fbSaveExperiment );
        final ImageButton btCloseEditExperiment = this.findViewById( R.id.btCloseEditExperiment );
        final TextView edExperimentName = this.findViewById( R.id.edExperimentName );
        final CheckBox chkRandom = this.findViewById( R.id.chkRandom );

        fabAddGroup.setOnClickListener( (v) -> this.addNewGroup() );
        fabSaveExperiment.setOnClickListener( (v) -> this.finishWithResultCode( RSC_SAVE_DATA ) );
        btCloseEditExperiment.setOnClickListener( (v) -> this.finishWithResultCode( RSC_DISMISS_DATA ) );
        chkRandom.setOnCheckedChangeListener( (button, booleanValue) -> this.onRandomChanged() );
        edExperimentName.addTextChangedListener( new TextWatcher() {
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
                EditExperimentActivity.this.onNameChanged( edExperimentName.getText().toString() );
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
        final TextView edExperimentName = this.findViewById( R.id.edExperimentName );
        final CheckBox chkRandom = this.findViewById( R.id.chkRandom );

        edExperimentName.setText( experiment.getName() );
        chkRandom.setChecked( experiment.isRandom() );
    }

    private void showGroups()
    {
        final Group[] groups = experiment.getGroups();
        final int NUM_ENTRIES = groups.length;
        final ListView lvExperimentMedia = this.findViewById( R.id.lvExperimentMedia );
        final TextView lblNoEntries = this.findViewById( R.id.lblNoEntries );

        if ( NUM_ENTRIES > 0 ) {
            final ListViewGroupEntry[] mediaEntryList = new ListViewGroupEntry[ NUM_ENTRIES ];

            // Create appropriate list
            for(int i = 0; i < NUM_ENTRIES; ++i) {
               mediaEntryList[ i ] = new ListViewGroupEntry( experiment, groups[ i ] );
            }

            // Create adapter
            lvExperimentMedia.setAdapter(
                    new ListViewGroupEntryArrayAdapter( this, mediaEntryList ) );

            lblNoEntries.setVisibility( View.GONE );
            lvExperimentMedia.setVisibility( View.VISIBLE );
        } else {
            lblNoEntries.setVisibility( View.VISIBLE );
            lvExperimentMedia.setVisibility( View.GONE );
        }

        return;
    }

    protected void deleteGroup(Group m)
    {
        selectedGroup = null;
        experiment.removeGroup( m );
        this.showGroups();
    }

    protected void addNewGroup()
    {
        final IconListAlertDialog newGroupDlg = new IconListAlertDialog( this,
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

        newGroupDlg.setItemClickListener( (adpt, v, op, l) -> {
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

                    newGroupDlg.dismiss();
                    EditExperimentActivity.this.startActivityForResult( launchData, RQC_ADD_GROUP) ;
        });

        newGroupDlg.show();
    }

    protected void editGroup(Group group)
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

    protected void sortGroupUp(Group g)
    {
        experiment.sortGroupUp( g );
        this.showGroups();
    }

    protected void sortGroupDown(Group g)
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

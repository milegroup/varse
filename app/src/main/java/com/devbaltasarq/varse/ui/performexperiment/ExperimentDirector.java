package com.devbaltasarq.varse.ui.performexperiment;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.User;
import com.devbaltasarq.varse.core.bluetooth.BluetoothDeviceWrapper;
import com.devbaltasarq.varse.ui.AppActivity;

public class ExperimentDirector extends AppActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        this.setContentView(R.layout.activity_experiment_director);

        final ImageButton btClose = this.findViewById( R.id.btCloseExperimentDirector );
        final TextView lblExperiment = this.findViewById( R.id.lblExperimentName );
        final FloatingActionButton fbLaunchNow = this.findViewById( R.id.fbLaunchNow );
        final Toolbar toolbar = this.findViewById( R.id.toolbar );
        final Chronometer crCrono = this.findViewById( R.id.crCrono );

        this.setSupportActionBar( toolbar );

        // Views
        btClose.setOnClickListener( (v) -> this.finish() );
        crCrono.setOnChronometerTickListener( (crono) -> onCronoUpdate( crono ) );
        fbLaunchNow.setOnClickListener( (v) -> this.launchExperiment() );

        // Assign values
        this.btDevice = PerformExperimentActivity.chosenBtDevice;
        this.experiment = PerformExperimentActivity.chosenExperiment;
        this.user = PerformExperimentActivity.chosenUser;
        lblExperiment.setText( this.experiment.getName() );

        // Create interface objects
        this.ivPictureBox = this.buildPictureBox();
        this.tvTextBox = this.buildTextBox();
        this.vVideoBox = this.buildVideoBox();

        // Prepare the UI for start
        this.prepareUIForManualGroup();
        this.tvTextBox.setText( R.string.lblPerformExperiment );
    }

    /** Builds the picture box needed to show images. */
    private ImageView buildPictureBox()
    {
        final ImageView ivPictureBox = new ImageView( this );

        return ivPictureBox;
    }

    /** Builds the textview needed to show tags. */
    private TextView buildTextBox()
    {
        final TextView textBox = new TextView( this );

        return textBox;
    }

    /** Builds the video box needed to show videos. */
    private VideoView buildVideoBox()
    {
        final VideoView videoBox = new VideoView( this );

        return videoBox;
    }

    /** Changes the main view in the container (FrameLayout). */
    private void changeChildInContainer(View v)
    {
        final FrameLayout flContainer = this.findViewById( R.id.flContainer );

        flContainer.removeAllViews();
        flContainer.addView( v );
    }

    /** Triggers when the crono changes. */
    private void onCronoUpdate(Chronometer crono)
    {
    }

    /** Prepares the UI for a picture group: show the chronometer and the picture box. */
    private void prepareUIForPictureGroup()
    {
        final Chronometer crCrono = this.findViewById( R.id.crCrono );

        crCrono.setVisibility( View.VISIBLE );
        this.changeChildInContainer( this.ivPictureBox );
    }

    /** Prepares the UI for a manual group: show the chronometer and the text box. */
    private void prepareUIForManualGroup()
    {
        final Chronometer crCrono = this.findViewById( R.id.crCrono );

        crCrono.setVisibility( View.VISIBLE );
        this.changeChildInContainer( this.tvTextBox );
    }

    /** Prepares the UI for a picture group: hide the chronometer and the video box. */
    private void prepareUIForVideoGroup()
    {
        final Chronometer crCrono = this.findViewById( R.id.crCrono );

        crCrono.setVisibility( View.INVISIBLE );
        changeChildInContainer( this.vVideoBox );
    }

    /** Launches the experiment. */
    private void launchExperiment()
    {
        final Chronometer crCrono = this.findViewById( R.id.crCrono );

        crCrono.start();
    }

    private ImageView ivPictureBox;
    private TextView tvTextBox;
    private VideoView vVideoBox;

    private User user;
    private Experiment experiment;
    private BluetoothDeviceWrapper btDevice;
}

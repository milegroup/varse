package com.devbaltasarq.varse.ui.performexperiment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.devbaltasarq.varse.BuildConfig;
import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.Result;
import com.devbaltasarq.varse.core.User;
import com.devbaltasarq.varse.core.bluetooth.BleService;
import com.devbaltasarq.varse.core.bluetooth.BluetoothDeviceWrapper;
import com.devbaltasarq.varse.core.bluetooth.BluetoothUtils;
import com.devbaltasarq.varse.core.bluetooth.HRListenerActivity;
import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.core.experiment.ManualGroup;
import com.devbaltasarq.varse.core.experiment.MediaGroup;
import com.devbaltasarq.varse.ui.AppActivity;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ExperimentDirector extends AppActivity implements HRListenerActivity {
    public static final String LogTag = ExperimentDirector.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_experiment_director );

        final ImageButton btClose = this.findViewById( R.id.btCloseExperimentDirector );
        final TextView lblExperiment = this.findViewById( R.id.lblExperimentName );
        final FloatingActionButton fbLaunchNow = this.findViewById( R.id.fbLaunchNow );
        final FloatingActionButton fbSkip = this.findViewById( R.id.fbSkip );
        final Toolbar toolbar = this.findViewById( R.id.toolbar );
        final Chronometer crCrono = this.findViewById( R.id.crCrono );
        final TextView lblExperimentDescription = this.findViewById( R.id.lblExperimentDescription );
        final TextView lblRecord = this.findViewById( R.id.lblRecord );
        final TextView lblDeviceName = this.findViewById( R.id.lblDeviceName );

        this.setSupportActionBar( toolbar );

        // Assign values
        this.orm = Orm.get();
        this.currentActivityIndex = 0;
        this.accumulatedTime = 0;
        this.askBeforeExit = true;
        this.btDevice = PerformExperimentActivity.chosenBtDevice;
        this.experiment = PerformExperimentActivity.chosenExperiment;
        this.user = PerformExperimentActivity.chosenUser;
        this.activitiesToPlay = this.buildActivitiesToPlay();

        // Create interface objects
        this.ivPictureBox = this.buildPictureBox();
        this.tvTextBox = this.buildTextBox();
        this.vVideoBox = this.buildVideoBox();

        // Prepare the UI for the start
        this.prepareUIForInitialDescription();
        lblExperimentDescription.setText( this.buildDescription() );

        // Views
        btClose.setOnClickListener( (v) -> this.finish() );
        crCrono.setOnChronometerTickListener( (crono) -> onCronoUpdate( crono ) );
        fbLaunchNow.setOnClickListener( (v) -> this.launchExperiment() );
        fbSkip.setOnClickListener( (v) -> this.skipCurrentActivity() );
        lblRecord.setText( this.user.getName() );
        lblDeviceName.setText( this.btDevice.getName() );
        lblExperiment.setText( this.experiment.getName() );
    }

    private void setAbleToLaunch(boolean isAble)
    {
        final FloatingActionButton fbLaunchNow = this.findViewById( R.id.fbLaunchNow );
        final TextView lblConnectionStatus = this.findViewById( R.id.lblConnectionStatus );

        if ( isAble ) {
            lblConnectionStatus.setText( R.string.lblConnected );
        } else {
            lblConnectionStatus.setText( R.string.lblDisconnected );
        }

        this.readyToLaunch = isAble;
        fbLaunchNow.setEnabled( isAble );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        BluetoothUtils.openBluetoothConnections( this,
                this.getString( R.string.lblConnected ),
                this.getString( R.string.lblDisconnected ) );

        this.setAbleToLaunch( false );
    }

    @Override
    public void onPause()
    {
        super.onPause();

        this.setAbleToLaunch( false );

        final Chronometer crCrono = this.findViewById( R.id.crCrono );

        crCrono.stop();
        BluetoothUtils.closeBluetoothConnections( this );
        Log.d( LogTag, "Director finished, stopped chrono, closed connections." );
    }

    @Override
    public boolean askBeforeLeaving()
    {
        return this.askBeforeExit;
    }

    @Override
    public void showStatus(String msg)
    {
        this.showStatus( LogTag, msg );
    }

    /** Builds the picture box needed to show images. */
    private ImageView buildPictureBox()
    {
        final ImageView ivPictureBox = new ImageView( this );

        ivPictureBox.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT ) );
        return ivPictureBox;
    }

    /** Builds the textview needed to show tags. */
    private TextView buildTextBox()
    {
        final TextView textBox = new TextView( this );

        textBox.setTextSize( 24 );
        textBox.setTextAlignment( TextView.TEXT_ALIGNMENT_CENTER );
        textBox.setGravity( Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL );
        textBox.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT ) );
        return textBox;
    }

    /** Builds the video box needed to show videos. */
    private VideoView buildVideoBox()
    {
        final VideoView videoBox = new VideoView( this );

        videoBox.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT ) );
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
        Log.d( LogTag, "Current activity index: " + this.currentActivityIndex );
        Log.d( LogTag, "Accumulated time: " + this.accumulatedTime );

        if ( this.currentActivityIndex < this.activitiesToPlay.length ) {
            final Group.Activity activity = this.activitiesToPlay[ this.currentActivityIndex ];
            final int timeToSpendInActivity = activity.getTime().getTimeInSeconds();
            final int seconds = (int)
                            ( ( SystemClock.elapsedRealtime() - crono.getBase() ) / 1000 )
                            - this.accumulatedTime;

            Log.d( LogTag, "Elapsed time: " + seconds + '"' );

            if ( seconds >= timeToSpendInActivity ) {
                this.skipCurrentActivity();
            }
        } else {
            this.stopExperiment();
        }

        return;
    }

    /** @return the elapsed time, in millis, from the start of the experiment. */
    private long getElapsedExperimentTime()
    {
        final Chronometer crono = this.findViewById( R.id.crCrono );

        return SystemClock.elapsedRealtime() - crono.getBase();
    }

    /** @return the elapsed time, in seconds, from the start of the experiment. */
    private int getElapsedActivityTime()
    {
        return (int) ( this.getElapsedExperimentTime() / 1000 ) - this.accumulatedTime;
    }

    /** Skips current activity. */
    private void skipCurrentActivity()
    {
        if ( this.currentActivityIndex < this.activitiesToPlay.length ) {
            final long elapsedTime = this.getElapsedExperimentTime();
            final int secondsInAct = this.getElapsedActivityTime();


            ++this.currentActivityIndex;
            this.accumulatedTime += secondsInAct;

            if ( this.currentActivityIndex < this.activitiesToPlay.length ) {
                Group.Activity act = this.activitiesToPlay[ this.currentActivityIndex ];
                this.result.add( new Result.ActivityChangeEvent( elapsedTime, act.getTag().toString() ) );
                this.showActivity();
            } else {
                this.stopExperiment();
            }
        } else {
            this.stopExperiment();
        }

        return;
    }

    /** Extracts the info received from the HR service.
     * @param intent The key-value extra collection has at least
     *                BleService.HEART_RATE_TAG for heart rate information (as int),
     *                and it can also have BleService.RR_TAG for the rr info (as int).
     */
    public void receiveBpm(Intent intent)
    {
        final int hr = intent.getIntExtra( BleService.HEART_RATE_TAG, -1 );
        final int rr = intent.getIntExtra( BleService.RR_TAG, -1 );

        if ( BuildConfig.DEBUG ) {
            if ( hr >= 0 ) {
                Log.d( LogTag, "HR received: " + hr + "bpm" );
            }

            if ( rr >= 0 ) {
                Log.d( LogTag, "RR received: " + rr + "millisecs" );
            }
        }

        if ( rr >= 0 ) {
            if ( !this.readyToLaunch ) {
                this.setAbleToLaunch( true );
            } else {
                if ( this.result != null ) {
                    this.result.add( new Result.BeatEvent(
                                this.getElapsedExperimentTime(),
                                rr ));
                }
            }
        }

        return;
    }

    /** Creates the description before the experiment starts. */
    private String buildDescription()
    {
        final StringBuilder toret = new StringBuilder();

        // Groups of this experiment
        for(Group g: this.experiment.getGroups()) {
            if ( g instanceof ManualGroup ) {
                toret.append( this.getString( R.string.lblGroupManual ) );
                toret.append( " - " );
                toret.append( g.toString() );
            } else {
                toret.append( g.toString() );
            }

            toret.append( ": " );
            toret.append( g.calculateTimeNeeded() );
            toret.append( '\n' );
        }

        return toret.toString();
    }

    /** Shows the description and hides the frame. */
    private void prepareUIForInitialDescription()
    {
        this.prepareGlobalUI( false );
    }

    /** Shows the frame and hides the experiment's description. */
    private void prepareUIForExperiment()
    {
        this.prepareGlobalUI( true );
    }

    private void prepareGlobalUI(boolean experimentVisible)
    {
        final FloatingActionButton fbLaunch = this.findViewById( R.id.fbLaunchNow );
        final FloatingActionButton fbSkip = this.findViewById( R.id.fbSkip );
        final FrameLayout flContainer = this.findViewById( R.id.flContainer );
        final LinearLayout lyInfo = this.findViewById( R.id.lyInfo );
        int frameVisibility = View.VISIBLE;
        int infoVisibility = View.GONE;
        int launchVisibility = View.GONE;
        int skipVisibility = View.VISIBLE;

        if ( !experimentVisible ) {
            frameVisibility = View.GONE;
            infoVisibility = View.VISIBLE;
            launchVisibility = View.VISIBLE;
            skipVisibility = View.GONE;
        }

        flContainer.setVisibility( frameVisibility );
        lyInfo.setVisibility( infoVisibility );
        fbSkip.setVisibility( skipVisibility );
        fbLaunch.setVisibility( launchVisibility );
    }

    /** Prepares the UI for a picture group: show the chronometer and the picture box. */
    private void prepareUIForPictureActivity()
    {
        this.changeChildInContainer( this.ivPictureBox );
    }

    /** Prepares the UI for a manual group: show the chronometer and the text box. */
    private void prepareUIForManualActivity()
    {
        this.changeChildInContainer( this.tvTextBox );
    }

    /** Prepares the UI for a picture group: hide the chronometer and the video box. */
    private void prepareUIForVideoActivity()
    {
        changeChildInContainer( this.vVideoBox );
    }

    /** Stops the experiment. */
    private void stopExperiment()
    {
        final Chronometer crCrono = this.findViewById( R.id.crCrono );

        crCrono.stop();

        try {
            final AlertDialog.Builder dlg = new AlertDialog.Builder( this );

            dlg.setTitle( this.experiment.getName() );
            dlg.setMessage( R.string.msgFinishedExperiment );
            dlg.setCancelable( false );
            dlg.setPositiveButton( R.string.lblBack, (dlgintf, i) -> {
                this.askBeforeExit = false;
                this.finish();
            } );

            this.orm.store( this.result );
            Log.i( LogTag, this.getString( R.string.msgFinishedExperiment ) );
            dlg.create().show();
        } catch(IOException exc) {
            this.showStatus( LogTag, "unable to save experiment result" );
        }
    }

    /** Launches the experiment. */
    private void launchExperiment()
    {
        final Chronometer crCrono = this.findViewById( R.id.crCrono );
        final TextView lblMaxTime = this.findViewById( R.id.lblMaxTime );
        final Duration timeNeeded = this.experiment.calculateTimeNeeded();

        // Prepare the UI
        this.prepareUIForExperiment();
        lblMaxTime.setText( timeNeeded.toChronoString() );

        // Create the result object
        final long currentTime = SystemClock.elapsedRealtime();
        this.result = new Result( Id.create(), currentTime, this.user, this.experiment );
        this.result.add(
                new Result.ActivityChangeEvent(
                        currentTime,
                        this.activitiesToPlay[ 0 ].getTag().toString() ) );

        // Prepare crono and launch
        crCrono.setBase( currentTime );
        this.currentActivityIndex = 0;
        this.accumulatedTime = 0;
        this.showActivity();
        crCrono.start();
    }

    private Group.Activity[] buildActivitiesToPlay()
    {
        final Group.Activity[] toret = new Group.Activity[ this.experiment.getNumActivities() ];
        final Group[] groupsInExperiment = this.experiment.getGroups();
        final int[] sequenceOfGroups = createSequence( groupsInExperiment.length, this.experiment.isRandom() );
        int pos = 0;

        // Run over all groups honoring randomness of experiment, and gather their activities
        for(int i: sequenceOfGroups) {
            // Retrieve the activities from groups, also honoring their own randomness
            final Group g = groupsInExperiment[ i ];
            final Group.Activity[] activitiesInGroup = g.getActivities();
            final int[] sequenceOfActivities = createSequence( g.size(), g.isRandom() );

            for(int j: sequenceOfActivities) {
                toret[ pos ] = activitiesInGroup[ j ];
                ++pos;
            }
        }

        return toret;
    }

    private void abortDueToMissingFile(File f)
    {
        final String ERROR_MSG = this.getString( R.string.msgFileNotFound )
                                    + ":\n" + f.getName();

        AlertDialog.Builder dlg = new AlertDialog.Builder( this );
        dlg.setTitle( R.string.ErrUnableToPerformExperiment );
        dlg.setMessage( ERROR_MSG );
        dlg.setPositiveButton( R.string.lblBack, (d, i) -> this.finish() );

        this.askBeforeExit = false;
        Log.e( LogTag, ERROR_MSG + "\n\t" + f.getAbsolutePath() );
        dlg.create().show();
    }

    private void loadImage(File imgFile)
    {
        final File experimentDirectory = this.orm.buildMediaDirectoryFor( this.experiment );
        final File mediaFile = new File( experimentDirectory, imgFile.getPath() );

        if ( mediaFile.exists() ) {
            final Bitmap bitmap = BitmapFactory.decodeFile( mediaFile.getPath() );

            this.ivPictureBox.setImageBitmap( bitmap );
        } else {
            this.abortDueToMissingFile( imgFile );
        }

        return;
    }

    private void loadVideo(File videoFile)
    {
        final File EXPERIMENT_DIR = this.orm.buildMediaDirectoryFor( this.experiment );
        final File MEDIA_FILE = new File( EXPERIMENT_DIR, videoFile.getPath() );

        if ( MEDIA_FILE.exists() ) {
            this.vVideoBox.setVideoPath( MEDIA_FILE.getPath() );
            this.vVideoBox.start();
        } else {
            this.abortDueToMissingFile( videoFile );
        }

        return;
    }

    private void loadManualActivity(ManualGroup.ManualActivity manualActivity)
    {
        this.tvTextBox.setText( manualActivity.getTag().toString() );
    }

    private void showActivity()
    {
        this.showActivity( this.currentActivityIndex );
    }

    private void showActivity(int i)
    {
        final TextView lblMaxActTime = this.findViewById( R.id.lblMaxActTime );
        final Group.Activity activity = this.activitiesToPlay[ i ];
        final Group group = activity.getGroup();

        if ( activity instanceof MediaGroup.MediaActivity ) {
            final MediaGroup.MediaActivity mediaActivity = (MediaGroup.MediaActivity) activity;

            final MediaGroup mediaGroup = (MediaGroup) group;

            if ( mediaGroup.getFormat() == MediaGroup.Format.Picture  ) {
                this.prepareUIForPictureActivity();
                this.loadImage( mediaActivity.getFile() );
            } else {
                this.prepareUIForVideoActivity();
                this.loadVideo( mediaActivity.getFile() );
            }
        }
        else
        if ( activity instanceof ManualGroup.ManualActivity ) {
            this.prepareUIForManualActivity();
            this.loadManualActivity( (ManualGroup.ManualActivity) activity );
        }

        final int exprTimeSecs = (int) Math.round( (double) this.getElapsedExperimentTime() / 1000 );
        final Duration durationDelta = new Duration( activity.getTime().getTimeInSeconds() );
        lblMaxActTime.setText( durationDelta.add( exprTimeSecs ).toChronoString() );
    }

    private static int[] createSequence(int max, boolean shuffle)
    {
        int[] toret = new int[ max ];

        if ( !shuffle ) {
            // Sorted sequence
            for(int i = 0; i < max; ++i) {
                toret[ i ] = i;
            }
        } else {
            // Random sequence
            final Random rnd = new Random();
            final Set<Integer> generatedIndexes = new HashSet<>( max );
            int generated = 0;

            while ( generated < max ) {
                final int newIndex = rnd.nextInt( max );

                if ( !generatedIndexes.contains( newIndex ) ) {
                    generatedIndexes.add( newIndex );
                    toret[ generated ] = newIndex;
                    ++generated;
                }
            }
        }

        return toret;
    }

    /** @return the BleService object used by this activity. */
    @Override
    public BleService getService()
    {
        return this.bleService;
    }

    @Override
    public void setService(BleService service)
    {
        this.bleService = service;
    }

    /** @return the BroadcastReceiver used by this activivty. */
    @Override
    public BroadcastReceiver getBroadcastReceiver()
    {
        return this.broadcastReceiver;
    }

    /** @return the device this activity will connect to. */
    @Override
    public BluetoothDeviceWrapper getBtDevice()
    {
        return this.btDevice;
    }

    /** @return the service connection for this activity. */
    @Override
    public ServiceConnection getServiceConnection()
    {
        return this.serviceConnection;
    }

    @Override
    public void setServiceConnection(ServiceConnection serviceConnection)
    {
        this.serviceConnection = serviceConnection;
    }

    @Override
    public void setBroadcastReceiver(BroadcastReceiver broadcastReceiver)
    {
        this.broadcastReceiver = broadcastReceiver;
    }

    private ImageView ivPictureBox;
    private TextView tvTextBox;
    private VideoView vVideoBox;

    private int currentActivityIndex;
    private int accumulatedTime;
    private User user;
    private Experiment experiment;
    private Group.Activity[] activitiesToPlay;
    private boolean askBeforeExit;
    private boolean readyToLaunch;
    private Result result;
    private Orm orm;

    private ServiceConnection serviceConnection;
    private BroadcastReceiver broadcastReceiver;
    private BleService bleService;
    private BluetoothDeviceWrapper btDevice;
}

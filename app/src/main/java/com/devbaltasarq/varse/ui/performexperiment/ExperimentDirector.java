package com.devbaltasarq.varse.ui.performexperiment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.devbaltasarq.varse.BuildConfig;
import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.Result;
import com.devbaltasarq.varse.core.User;
import com.devbaltasarq.varse.core.bluetooth.BleService;
import com.devbaltasarq.varse.core.bluetooth.BluetoothDeviceWrapper;
import com.devbaltasarq.varse.core.bluetooth.BluetoothUtils;
import com.devbaltasarq.varse.core.bluetooth.HRListenerActivity;
import com.devbaltasarq.varse.core.bluetooth.ServiceConnectionWithStatus;
import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.core.experiment.ManualGroup;
import com.devbaltasarq.varse.core.experiment.MediaGroup;
import com.devbaltasarq.varse.ui.AppActivity;
import com.devbaltasarq.varse.ui.adapters.ListViewActivityArrayAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;


public class ExperimentDirector extends AppActivity implements HRListenerActivity {
    public static final String LOG_TAG = ExperimentDirector.class.getSimpleName();

    /** Interface for listeners. */
    private interface Listener<T> {
        void handle(T sender);
    }

    /** Represents a chronometer. */
    private static class Chronometer {
        /** Creates a new chronometer with an event handler. */
        public Chronometer(Listener<Chronometer> eventHandler)
        {
            this.handler = new Handler( Looper.getMainLooper() );
            this.eventHandler = eventHandler;
            this.startTime = 0;
            this.stopped = false;
        }

        /** @return the starting time. */
        public long getBase()
        {
            return this.startTime;
        }

        /** @return the elapsed duration, in milliseconds. */
        public long getMillis()
        {
            return SystemClock.elapsedRealtime() - this.startTime;
        }

        /** Resets the current elapsed time with the current real time. */
        public void reset()
        {
            this.reset( SystemClock.elapsedRealtime() );
        }

        /** Resets the current elapsed time with the given time. */
        public void reset(long time)
        {
            this.startTime = time;
        }

        /** Starts the chronometer */
        public void start()
        {
            this.stopped = false;

            this.sendHR = () -> {
                if ( ! this.stopped ) {
                    this.eventHandler.handle( this );
                    this.handler.postDelayed( this.sendHR,1000);
                }
            };

            this.handler.post( this.sendHR );
        }

        /** Eliminates the daemon so the crono is stopped. */
        public void stop()
        {
            this.stopped = true;
            this.handler.removeCallbacks( this.sendHR );
            this.handler.removeCallbacksAndMessages( null );
        }

        private boolean stopped;
        private long startTime;
        private Handler handler;
        private Runnable sendHR;
        private Listener<Chronometer> eventHandler;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_experiment_director );

        final ImageButton BT_CLOSE = this.findViewById( R.id.btCloseExperimentDirector );
        final TextView LBL_EXPERIMENT = this.findViewById( R.id.lblExperimentName );
        final FloatingActionButton FB_LAUNCH_NOW = this.findViewById( R.id.fbLaunchNow );
        final FloatingActionButton FB_SKIP = this.findViewById( R.id.fbSkip );
        final Toolbar TOOLBAR = this.findViewById( R.id.toolbar );
        final TextView LBL_RECORD = this.findViewById( R.id.lblRecord );
        final TextView LBL_DEVICE_NAME = this.findViewById( R.id.lblDeviceName );
        final FrameLayout FL_CONTAINER = this.findViewById( R.id.flContainer );

        this.setSupportActionBar( TOOLBAR );

        // Assign values
        this.orm = Orm.get();
        this.activityIndex = 0;
        this.accumulatedTimeInSeconds = 0;
        this.askBeforeExit = true;
        this.onExperiment = false;
        this.btDevice = PerformExperimentActivity.chosenBtDevice;
        this.experiment = PerformExperimentActivity.chosenExperiment;
        this.user = PerformExperimentActivity.chosenUser;

        this.buildActivitiesToPlay();

        // Create interface objects
        this.ivPictureBox = this.buildPictureBox();
        this.tvTextBox = this.buildTextBox();
        this.vVideoBox = this.buildVideoBox();

        // Prepare the UI for the start
        this.prepareUIForInitialDescription();
        this.showActivities();

        // Events
        BT_CLOSE.setOnClickListener( (v) -> this.finish() );
        this.chrono = new Chronometer( this::onCronoUpdate );
        FB_LAUNCH_NOW.setOnClickListener( (v) -> this.launchExperiment() );
        FB_SKIP.setOnClickListener( (v) -> this.skipCurrentActivity() );
        LBL_RECORD.setText( this.user.getName() );
        LBL_DEVICE_NAME.setText( this.btDevice.getName() );
        LBL_EXPERIMENT.setText( this.experiment.getName() );
        FL_CONTAINER.setOnLongClickListener( (v) -> {
            ExperimentDirector.this.setInfoVisibility();
            return true;
        });
    }

    private void setInfoVisibility()
    {
        final CardView CD_INFO = this.findViewById( R.id.cdInfo );

        int visibility = CD_INFO.getVisibility();

        if ( visibility == View.VISIBLE ) {
            visibility = View.GONE;
        } else {
            visibility = View.VISIBLE;
        }

        CD_INFO.setVisibility( visibility );
    }

    private void setAbleToLaunch(boolean isAble)
    {
        final FloatingActionButton FB_LAUNCH_NOW = this.findViewById( R.id.fbLaunchNow );
        final TextView LBL_CONN_STATUS = this.findViewById( R.id.lblConnectionStatus );
        int visibility;

        if ( isAble ) {
            // "Connected" in "approval" color (e.g green).
            LBL_CONN_STATUS.setText( R.string.lblConnected );
            LBL_CONN_STATUS.setTextColor( Color.parseColor( "#228B22" ) );
        } else {
            // "Disconnected" in "denial" color (e.g red).
            LBL_CONN_STATUS.setText( R.string.lblDisconnected );
            LBL_CONN_STATUS.setTextColor( Color.parseColor( "#8B0000" ) );
        }

        // Check whether there is something to play
        if ( this.groupsToPlay.length == 0 ) {
            isAble = false;
        }

        // Set the visibility of the launch button
        if ( isAble ) {
            visibility = View.VISIBLE;
        } else {
            visibility = View.GONE;
        }

        this.readyToLaunch = isAble;
        FB_LAUNCH_NOW.setVisibility( visibility );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        this.getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );

        if ( BluetoothUtils.fixBluetoothNeededPermissions( this ).length > 0 ) {
            Toast.makeText( this, "Insufficient permissions", Toast.LENGTH_SHORT ).show();
        }

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
        this.chrono.stop();
        this.stopExperiment();

        this.getWindow().clearFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );

        BluetoothUtils.closeBluetoothConnections( this );
        Log.d(LOG_TAG, "Director finished, stopped chrono, closed connections." );
    }

    @Override
    public boolean askBeforeLeaving()
    {
        return this.askBeforeExit;
    }

    @Override
    public void showStatus(String msg)
    {
        this.showStatus(LOG_TAG, msg );
    }

    private void showActivities()
    {
        final ListView LV_ACTS = this.findViewById( R.id.lvExperimentActivities );
        final TextView LBL_NO_ENTRIES = this.findViewById( R.id.lblNoEntries );
        final int NUM_ENTRIES = this.groupsToPlay.length;

        Log.i(LOG_TAG, "starting showActivities()..." );
        Log.i(LOG_TAG, "entries: " + NUM_ENTRIES );

        if ( NUM_ENTRIES > 0 ) {
            final ListViewActivityArrayAdapter ACTS_ADAPTER;

            // Create adapter
            ACTS_ADAPTER = new ListViewActivityArrayAdapter( this, this.activitiesToPlay );

            LV_ACTS.setAdapter( ACTS_ADAPTER );
            LBL_NO_ENTRIES.setVisibility( View.GONE );
            LV_ACTS.setVisibility( View.VISIBLE );
        } else {
            LBL_NO_ENTRIES.setVisibility( View.VISIBLE );
            LV_ACTS.setVisibility( View.GONE );
            Log.i(LOG_TAG, "    no entries" );
        }

        Log.i(LOG_TAG, "finished showActivities()" );
    }

    /** Builds the picture box needed to show images. */
    private ImageView buildPictureBox()
    {
        final ImageView IV_PICTURE_BOX = new ImageView( this );

        IV_PICTURE_BOX.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT ) );
        IV_PICTURE_BOX.setLayerType( View.LAYER_TYPE_SOFTWARE, null );
        return IV_PICTURE_BOX;
    }

    /** Builds the textview needed to show tags. */
    private TextView buildTextBox()
    {
        final TextView TEXT_BOX = new TextView( this );

        TEXT_BOX.setTextSize( 24 );
        TEXT_BOX.setTextAlignment( TextView.TEXT_ALIGNMENT_CENTER );
        TEXT_BOX.setGravity( Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL );
        TEXT_BOX.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT ) );
        return TEXT_BOX;
    }

    /** Builds the video box needed to show videos. */
    private VideoView buildVideoBox()
    {
        final VideoView VIDEO_BOX = new VideoView( this );

        VIDEO_BOX.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT ) );
        return VIDEO_BOX;
    }

    /** Changes the main view in the container (FrameLayout). */
    private void changeChildInContainer(View v)
    {
        final FrameLayout FL_CONTAINER = this.findViewById( R.id.flContainer );

        FL_CONTAINER.removeAllViews();
        FL_CONTAINER.addView( v );
    }

    /** Triggers when the crono changes. */
    @SuppressWarnings("unused")
    private void onCronoUpdate(Chronometer crono)
    {
        final TextView LBL_CRONO = this.findViewById( R.id.lblCrono );
        final int ELAPSED_TIME_SECONDS = this.getElapsedExperimentSeconds();
        Log.d(LOG_TAG, "Current activity index: " + this.activityIndex);
        Log.d(LOG_TAG, "Accumulated time: " + this.accumulatedTimeInSeconds);

        LBL_CRONO.setText( new Duration( ELAPSED_TIME_SECONDS ).toChronoString() );

        // Stop if the service was disconnected
        if ( !this.serviceConnection.isConnected() ) {
            this.onExperiment = false;
        }

        // Now evaluate
        if ( this.onExperiment ) {
            final Group GROUP = this.groupsToPlay[ this.groupIndex];
            final Group.Activity ACTIVITY = GROUP.getActivities()[ this.activityIndex];
            final int MAX_TIME_SPENT_IN_ACT = ACTIVITY.getTime().getTimeInSeconds();
            final int SECONDS = ELAPSED_TIME_SECONDS - this.accumulatedTimeInSeconds;

            Log.d(LOG_TAG, "Elapsed time: " + SECONDS + '"' );

            if ( SECONDS >= MAX_TIME_SPENT_IN_ACT ) {
                this.skipCurrentActivity();
            }
        } else {
            this.stopExperiment();
        }

        return;
    }

    /** @return the elapsed time, in millis, from the start of the experiment. */
    private long getElapsedExperimentMillis()
    {
        return this.chrono.getMillis();
    }

    /** @return the elapsed time, in seconds, from the start of the experiment. */
    private int getElapsedExperimentSeconds()
    {
        return (int) ( (double) this.getElapsedExperimentMillis() / 1000 );
    }

    /** @return the elapsed time, in seconds, from the start of the current activity. */
    private int getElapsedActivityTime()
    {
        return this.getElapsedExperimentSeconds() - this.accumulatedTimeInSeconds;
    }

    /** @return the current tag, depending on the kind of activity. */
    private String inferTag()
    {
        String toret = "";

        if ( this.onExperiment ) {
            final Group GROUP = this.groupsToPlay[ this.groupIndex ];

            if ( GROUP instanceof MediaGroup ) {
                toret = ( (MediaGroup) GROUP ).getTag().toString();
            }
            else
            if ( GROUP instanceof ManualGroup ) {
                final Group.Activity ACTIVITY = GROUP.getActivities()[ this.activityIndex ];

                toret = ACTIVITY.getTag().toString();
            } else {
              throw new Error( "ExperimentDirector.inferTag(): unable to find tag" );
            }

        }

        return toret;
    }

    /** Skips current activity. */
    private void skipCurrentActivity()
    {
        if ( this.onExperiment
          && this.groupIndex < this.groupsToPlay.length )
        {
            final Group GROUP = this.groupsToPlay[ this.groupIndex ];

            if ( this.activityIndex < GROUP.size() ) {
                final long ELAPSED_TIME = this.getElapsedExperimentMillis();
                final int SECONDS_IN_ACT = this.getElapsedActivityTime();

                boolean changedGroup = this.calculateNextIndexes();
                this.accumulatedTimeInSeconds += SECONDS_IN_ACT;

                if ( this.onExperiment ) {
                    final Group NEW_GROUP = this.groupsToPlay[ this.groupIndex ];

                    if ( changedGroup
                      || ( NEW_GROUP instanceof ManualGroup ) )
                    {
                        this.addToResult( new Result.ActivityChangeEvent( ELAPSED_TIME, this.inferTag() ) );
                    }

                    this.showActivity();
                } else {
                    this.stopExperiment();
                }
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
    @Override
    public void onReceiveBpm(Intent intent)
    {
        final TextView LBL_INSTANT_BPM = this.findViewById( R.id.lblInstantBpm );
        final int HR = intent.getIntExtra( BleService.HEART_RATE_TAG, -1 );
        final int MEAN_RR = intent.getIntExtra( BleService.MEAN_RR_TAG, -1 );
        int[] rrs = intent.getIntArrayExtra( BleService.RR_TAG );

        LBL_INSTANT_BPM.setText( HR + this.getString( R.string.lblBpm ) );

        if ( BuildConfig.DEBUG ) {
            if ( HR >= 0 ) {
                Log.d(LOG_TAG, "HR received: " + HR + "bpm" );
            }

            if ( MEAN_RR >= 0 ) {
                Log.d(LOG_TAG, "Mean RR received: " + MEAN_RR + "millisecs" );
            }

            if ( rrs != null ) {
                final StringBuilder STR_RR = new StringBuilder();

                Log.d(LOG_TAG, "RR's received: " + rrs.length );

                for(int rr: rrs) {
                    STR_RR.append( rr );
                    STR_RR.append( ' ' );
                }

                Log.d(LOG_TAG, "RR's: { " + STR_RR.toString() + "}" );
            } else {
                Log.d(LOG_TAG, "No RR's received." );
            }
        }

        if ( HR >= 0 ) {
            // Build RR's, if necessary
            if ( rrs == null ) {
                rrs = new int[] {
                        (int) ( ( (float) HR / 60.0 ) * 1000 )
                };
            }

            // Start or store
            if ( !this.readyToLaunch ) {
                this.setAbleToLaunch( true );
            } else {
                long time = this.getElapsedExperimentMillis();

                for(int rr: rrs) {
                    this.addToResult( new Result.BeatEvent( time, rr ) );

                    time += rr;
                }
            }
        }

        return;
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
        final FloatingActionButton FB_LAUNCH = this.findViewById( R.id.fbLaunchNow );
        final FloatingActionButton FB_SKIP = this.findViewById( R.id.fbSkip );
        final FrameLayout FL_CONTAINER = this.findViewById( R.id.flContainer );
        final LinearLayout LY_INFO = this.findViewById( R.id.lyInfo );
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

        FL_CONTAINER.setVisibility( frameVisibility );
        LY_INFO.setVisibility( infoVisibility );
        FB_SKIP.setVisibility( skipVisibility );
        FB_LAUNCH.setVisibility( launchVisibility );
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
    private synchronized void stopExperiment()
    {
        // Finish for good
        this.chrono.stop();
        this.onExperiment = false;
        this.setRequestedOrientation( this.scrOrientationOnExperiment );

        // Store results
        if ( this.resultBuilder != null ) {
            final long ELAPSED_MILLIS = this.getElapsedExperimentMillis();

            try {
                this.orm.store( this.resultBuilder.build( ELAPSED_MILLIS ) );
                this.resultBuilder = null;
                Log.i(LOG_TAG, this.getString( R.string.msgFinishedExperiment ) );
            } catch(IOException exc) {
                this.showStatus(LOG_TAG, "unable to save experiment result" );
            }
        }

        // Warn the experiment has finished
        final AlertDialog.Builder DLG = new AlertDialog.Builder( this );

        DLG.setTitle( this.experiment.getName() );
        DLG.setMessage( R.string.msgFinishedExperiment );
        DLG.setCancelable( false );
        DLG.setPositiveButton( R.string.lblBack, (d, i) -> {
            this.askBeforeExit = false;
            this.finish();
        });

        DLG.create().show();
    }

    /** Puts the current group and current activities on their start.
      * Also sets onExperiment to false in case there are no more activities to change to.
      * @return true if there was a group change, false otherwise.
      */
    private boolean calculateNextIndexes()
    {
        final Group[] GROUPS = this.groupsToPlay;
        boolean toret = false;

        if ( this.groupIndex < GROUPS.length ) {
            final Group.Activity[] ACTIVITIES = GROUPS[ this.groupIndex ].getActivities();

            ++this.activityIndex;

            if ( this.activityIndex >= ACTIVITIES.length ) {
                toret = true;
                this.activityIndex = 0;
                ++this.groupIndex;

                while( this.groupIndex < GROUPS.length
                   &&  this.groupsToPlay[ this.groupIndex].getActivities().length == 0 )
                {
                    ++this.groupIndex;
                }
            }
        }

        if ( this.groupIndex >= GROUPS.length ) {
            this.onExperiment = false;
        }

        return toret;
    }

    /** Launches the experiment. */
    private void launchExperiment()
    {
        if ( this.groupsToPlay.length > 0 ) {
            final TextView LBL_MAX_TIME = this.findViewById( R.id.lblMaxTime );
            final Duration TIME_NEEDED = this.experiment.calculateTimeNeeded();

            // Prevent screen rotation
            this.scrOrientationOnExperiment = this.getRequestedOrientation();
            this.setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_NOSENSOR );

            // Prepare the UI
            this.prepareUIForExperiment();
            LBL_MAX_TIME.setText( TIME_NEEDED.toChronoString() );

            // Create the result object
            this.onExperiment = true;
            this.groupIndex = 0;
            this.activityIndex = -1;
            this.calculateNextIndexes();
            this.resultBuilder = new Result.Builder( this.user, this.experiment, System.currentTimeMillis() );
            this.addToResult( new Result.ActivityChangeEvent( 0, this.inferTag() ) );

            // Start counting time
            this.chrono.reset();
            this.chrono.start();
            Log.i(LOG_TAG, "Starting..." );

            // Prepare first activity
            this.accumulatedTimeInSeconds = 0;
            this.showActivity();
        } else {
            this.showStatus(LOG_TAG, this.getString( R.string.errNotEnoughActivities) );
        }

        return;
    }

    /** Creates the list of activities to play, in two structures: one in with the activities
      * divided in groups, and another one with a sequential collection of activities.
      * Honors the random attribute.
      */
    @SuppressWarnings("unchecked")
    private void buildActivitiesToPlay()
    {
        final ArrayList<Group.Activity> ACTIVITIES = new ArrayList<>( this.experiment.getNumActivities() );
        final Group[] GROUPS_IN_EXPERIMENT = this.experiment.getGroups();
        final int NUM_GROUPS = GROUPS_IN_EXPERIMENT.length;
        final int[] SEQUENCE_OF_GROUPS = createSequence( NUM_GROUPS, this.experiment.isRandom() );

        int pos = 0;
        this.groupsToPlay = new Group[ NUM_GROUPS ];

        // Run over all groups honoring randomness of experiment, and gather their activities
        for(int i: SEQUENCE_OF_GROUPS) {
            // Retrieve the activities from groups, also honoring their own randomness
            final Group GROUP = GROUPS_IN_EXPERIMENT[ i ].copy();
            final Group.Activity[] GROUP_ACTIVITIES = GROUPS_IN_EXPERIMENT[ i ].getActivities();
            final int[] SEQUENCE_OF_ACTIVITIES = createSequence( GROUP.size(), GROUP.isRandom() );

            // Append all activities in this group, randomly or not
            GROUP.clear();
            for(int j: SEQUENCE_OF_ACTIVITIES) {
                final Group.Activity ACTIVITY = GROUP_ACTIVITIES[ j ].copy();

                GROUP.add( ACTIVITY );
                ACTIVITIES.add( ACTIVITY );
            }

            this.groupsToPlay[ pos ] = GROUP;
            ++pos;
        }

        this.activitiesToPlay = ACTIVITIES.toArray( new Group.Activity[ 0 ] );
    }

    private void abortDueToMissingFile(File f)
    {
        final String ERROR_MSG = this.getString( R.string.msgFileNotFound )
                                    + ":\n" + f.getName();

        AlertDialog.Builder dlg = new AlertDialog.Builder( this );
        dlg.setTitle( R.string.errUnableToShowActivity);
        dlg.setMessage( ERROR_MSG );

        Log.e(LOG_TAG, ERROR_MSG + "\n\t" + f.getAbsolutePath() );
        dlg.create().show();
    }

    private void loadImage(File imgFile)
    {
        final File DIR_EXPERIMENT = this.orm.buildMediaDirectoryFor( this.experiment );
        final File MEDIA_FILE = new File( DIR_EXPERIMENT, imgFile.getPath() );

        if ( MEDIA_FILE.exists() ) {
            final Bitmap BITMAP = BitmapFactory.decodeFile( MEDIA_FILE.getPath() );

            this.ivPictureBox.setImageBitmap( BITMAP );
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

    /** Adds a new event to the result.
      * Since the bpm information comes from one thread and the time from another,
      * this centralized consumer is synchronized.
      * @param evt the event to store.
      */
    private synchronized void addToResult(Result.Event evt)
    {
        if ( this.resultBuilder != null
          && this.onExperiment )
        {
            this.resultBuilder.add( evt );
        }

        return;
    }

    private void showActivity()
    {
        this.showActivity( this.groupIndex, this.activityIndex);
    }

    private void showActivity(int i, int j)
    {
        final TextView LBL_MAX_ACT_TIME = this.findViewById( R.id.lblMaxActTime );
        final Group.Activity ACT = this.groupsToPlay[ i ].getActivities()[ j ];
        final Group GRP = ACT.getGroup();

        if ( ACT instanceof MediaGroup.MediaActivity ) {
            final MediaGroup.MediaActivity MEDIA_ACT = (MediaGroup.MediaActivity) ACT;

            final MediaGroup MEDIA_GRP = (MediaGroup) GRP;

            if ( MEDIA_GRP.getFormat() == MediaGroup.Format.Picture  ) {
                this.prepareUIForPictureActivity();
                this.loadImage( MEDIA_ACT.getFile() );
            } else {
                this.prepareUIForVideoActivity();
                this.loadVideo( MEDIA_ACT.getFile() );
            }
        }
        else
        if ( ACT instanceof ManualGroup.ManualActivity ) {
            this.prepareUIForManualActivity();
            this.loadManualActivity( (ManualGroup.ManualActivity) ACT );
        }

        final int TOTAL_SECS = this.getElapsedExperimentSeconds();
        final Duration DURATION_DELTA = new Duration( ACT.getTime().getTimeInSeconds() );
        LBL_MAX_ACT_TIME.setText( DURATION_DELTA.add( TOTAL_SECS ).toChronoString() );

        Log.i(LOG_TAG, "Starting activity: '" + ACT.getTag() + "'"
                        + "\n\tCurrent time: " + TOTAL_SECS + "s"
                        + "\n\tActivity time: " + ACT.getTime().getTimeInSeconds() + "s"
                        + "\n\tActivity ends at max: " + DURATION_DELTA + "s" );
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
            final Random RND = new Random();
            final Set<Integer> GENERATED_INDEXES = new HashSet<>( max );
            int generated = 0;

            while ( generated < max ) {
                final int newIndex = RND.nextInt( max );

                if ( !GENERATED_INDEXES.contains( newIndex ) ) {
                    GENERATED_INDEXES.add( newIndex );
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
    public ServiceConnectionWithStatus getServiceConnection()
    {
        return this.serviceConnection;
    }

    @Override
    public void setServiceConnection(ServiceConnectionWithStatus serviceConnection)
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

    private int scrOrientationOnExperiment;
    private int activityIndex;
    private int groupIndex;
    private int accumulatedTimeInSeconds;
    private User user;
    private Experiment experiment;
    private Group.Activity[] activitiesToPlay;
    private Group[] groupsToPlay;
    private boolean askBeforeExit;
    private boolean readyToLaunch;
    private boolean onExperiment;

    private Chronometer chrono;
    private Result.Builder resultBuilder;
    private Orm orm;

    private ServiceConnectionWithStatus serviceConnection;
    private BroadcastReceiver broadcastReceiver;
    private BleService bleService;
    private BluetoothDeviceWrapper btDevice;
}

package com.devbaltasarq.varse.ui.performexperiment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
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
    public static final String LogTag = ExperimentDirector.class.getSimpleName();

    /** Interface for listeners. */
    private interface Listener<T> {
        void handle(T sender);
    }

    /** Represents a chronometer. */
    private static class Chronometer {
        /** Creates a new chronometer with an event handler. */
        public Chronometer(Listener<Chronometer> eventHandler)
        {
            this.handler = new Handler();
            this.eventHandler = eventHandler;
            this.startTime = 0;
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
            this.sendHR = () -> {
                this.eventHandler.handle( this );
                this.handler.postDelayed( this.sendHR,1000);
            };

            this.handler.post( this.sendHR );
        }

        /** Eliminates the daemon so the crono is stopped. */
        public void stop()
        {
            this.handler.removeCallbacksAndMessages( null );
        }

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

        final ImageButton btClose = this.findViewById( R.id.btCloseExperimentDirector );
        final TextView lblExperiment = this.findViewById( R.id.lblExperimentName );
        final FloatingActionButton fbLaunchNow = this.findViewById( R.id.fbLaunchNow );
        final FloatingActionButton fbSkip = this.findViewById( R.id.fbSkip );
        final Toolbar toolbar = this.findViewById( R.id.toolbar );
        final TextView lblRecord = this.findViewById( R.id.lblRecord );
        final TextView lblDeviceName = this.findViewById( R.id.lblDeviceName );
        final FrameLayout flContainer = this.findViewById( R.id.flContainer );
        final CardView cdCrono = this.findViewById( R.id.cdCrono );

        this.setSupportActionBar( toolbar );

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
        btClose.setOnClickListener( (v) -> this.finish() );
        this.chrono = new Chronometer( this::onCronoUpdate );
        fbLaunchNow.setOnClickListener( (v) -> this.launchExperiment() );
        fbSkip.setOnClickListener( (v) -> this.skipCurrentActivity() );
        lblRecord.setText( this.user.getName() );
        lblDeviceName.setText( this.btDevice.getName() );
        lblExperiment.setText( this.experiment.getName() );
        flContainer.setOnLongClickListener( (v) -> {
            cdCrono.setVisibility( View.VISIBLE );
            return true;
        });
    }

    private void setAbleToLaunch(boolean isAble)
    {
        final FloatingActionButton fbLaunchNow = this.findViewById( R.id.fbLaunchNow );
        final TextView lblConnectionStatus = this.findViewById( R.id.lblConnectionStatus );
        int visibility;

        if ( isAble ) {
            // "Connected" in "approval" color (e.g green).
            lblConnectionStatus.setText( R.string.lblConnected );
            lblConnectionStatus.setTextColor( Color.parseColor( "#228B22" ) );
        } else {
            // "Disconnected" in "denial" color (e.g red).
            lblConnectionStatus.setText( R.string.lblDisconnected );
            lblConnectionStatus.setTextColor( Color.parseColor( "#8B0000" ) );
        }

        // Check whether there something to play
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
        fbLaunchNow.setVisibility( visibility );
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
        this.chrono.stop();

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

    private void showActivities()
    {
        final ListView lvActs = this.findViewById( R.id.lvExperimentActivities );
        final TextView lblNoEntries = this.findViewById( R.id.lblNoEntries );
        final int NUM_ENTRIES = this.groupsToPlay.length;

        Log.i( LogTag, "starting showActivities()..." );
        Log.i( LogTag, "entries: " + NUM_ENTRIES );

        if ( NUM_ENTRIES > 0 ) {
            final ListViewActivityArrayAdapter actsAdapter;

            // Create adapter
            actsAdapter = new ListViewActivityArrayAdapter( this, this.activitiesToPlay );

            lvActs.setAdapter( actsAdapter );
            lblNoEntries.setVisibility( View.GONE );
            lvActs.setVisibility( View.VISIBLE );
        } else {
            lblNoEntries.setVisibility( View.VISIBLE );
            lvActs.setVisibility( View.GONE );
            Log.i( LogTag, "    no entries" );
        }

        Log.i( LogTag, "finished showActivities()" );
    }

    /** Builds the picture box needed to show images. */
    private ImageView buildPictureBox()
    {
        final ImageView ivPictureBox = new ImageView( this );

        ivPictureBox.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT ) );
        ivPictureBox.setLayerType( View.LAYER_TYPE_SOFTWARE, null );
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
    @SuppressWarnings("unused")
    private void onCronoUpdate(Chronometer crono)
    {
        final TextView lblCrono = this.findViewById( R.id.lblCrono );
        final int elapsedTimeSeconds = this.getElapsedExperimentSeconds();
        Log.d( LogTag, "Current activity index: " + this.activityIndex);
        Log.d( LogTag, "Accumulated time: " + this.accumulatedTimeInSeconds);

        lblCrono.setText( new Duration( elapsedTimeSeconds ).toChronoString() );

        if ( this.onExperiment ) {
            final Group GROUP = this.groupsToPlay[ this.groupIndex];
            final Group.Activity ACTIVITY = GROUP.getActivities()[ this.activityIndex];
            final int maxTimeToSpendInActivity = ACTIVITY.getTime().getTimeInSeconds();
            final int seconds = elapsedTimeSeconds - this.accumulatedTimeInSeconds;

            Log.d( LogTag, "Elapsed time: " + seconds + '"' );

            if ( seconds >= maxTimeToSpendInActivity ) {
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

    /** @return the elapsed time, in millis, from the start of the experiment. */
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
                final long elapsedTime = this.getElapsedExperimentMillis();
                final int secondsInAct = this.getElapsedActivityTime();

                boolean changedGroup = this.calculateNextIndexes();
                this.accumulatedTimeInSeconds += secondsInAct;

                if ( this.onExperiment ) {
                    final Group NEW_GROUP = this.groupsToPlay[ this.groupIndex ];

                    if ( changedGroup
                      || ( NEW_GROUP instanceof ManualGroup ) )
                    {
                        this.addToResult( new Result.ActivityChangeEvent( elapsedTime, this.inferTag() ) );
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
                this.addToResult( new Result.BeatEvent( this.getElapsedExperimentMillis(), rr ) );
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
    private synchronized void stopExperiment()
    {
        this.chrono.stop();

        if ( !this.onExperiment
          && this.resultBuilder != null )
        {
            final long elapsedMillis = this.getElapsedExperimentMillis();

            try {
                final AlertDialog.Builder dlg = new AlertDialog.Builder( this );

                dlg.setTitle( this.experiment.getName() );
                dlg.setMessage( R.string.msgFinishedExperiment );
                dlg.setCancelable( false );
                dlg.setPositiveButton( R.string.lblBack, (d, i) -> {
                    this.askBeforeExit = false;
                    this.finish();
                } );

                this.orm.store( this.resultBuilder.build( elapsedMillis ) );
                this.resultBuilder = null;
                Log.i( LogTag, this.getString( R.string.msgFinishedExperiment ) );
                dlg.create().show();
            } catch(IOException exc) {
                this.showStatus( LogTag, "unable to save experiment result" );
            }
        }

        return;
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
            final TextView lblMaxTime = this.findViewById( R.id.lblMaxTime );
            final Duration timeNeeded = this.experiment.calculateTimeNeeded();

            // Prepare the UI
            this.prepareUIForExperiment();
            lblMaxTime.setText( timeNeeded.toChronoString() );

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
            Log.i( LogTag, "Starting..." );

            // Prepare first activity
            this.accumulatedTimeInSeconds = 0;
            this.showActivity();
        } else {
            this.showStatus( LogTag, this.getString( R.string.ErrNotEnoughActivities ) );
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
        dlg.setTitle( R.string.ErrUnableToShowActivity );
        dlg.setMessage( ERROR_MSG );

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
        final TextView lblMaxActTime = this.findViewById( R.id.lblMaxActTime );
        final Group.Activity activity = this.groupsToPlay[ i ].getActivities()[ j ];
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

        final int TOTAL_SECS = this.getElapsedExperimentSeconds();
        final Duration DURATION_DELTA = new Duration( activity.getTime().getTimeInSeconds() );
        lblMaxActTime.setText( DURATION_DELTA.add( TOTAL_SECS ).toChronoString() );

        Log.i( LogTag, "Starting activity: '" + activity.getTag() + "'"
                        + "\n\tCurrent time: " + TOTAL_SECS + "s"
                        + "\n\tActivity time: " + activity.getTime().getTimeInSeconds() + "s"
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

    private ServiceConnection serviceConnection;
    private BroadcastReceiver broadcastReceiver;
    private BleService bleService;
    private BluetoothDeviceWrapper btDevice;
}

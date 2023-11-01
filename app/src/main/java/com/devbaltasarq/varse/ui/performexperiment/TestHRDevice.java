// VARSE 2019/23 (c) Baltasar for MILEGroup MIT License <baltasarq@uvigo.es>


package com.devbaltasarq.varse.ui.performexperiment;


import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Ofm;
import com.devbaltasarq.varse.core.bluetooth.BleService;
import com.devbaltasarq.varse.core.bluetooth.BluetoothDeviceWrapper;
import com.devbaltasarq.varse.core.bluetooth.BluetoothUtils;
import com.devbaltasarq.varse.core.bluetooth.HRListenerActivity;
import com.devbaltasarq.varse.core.bluetooth.ServiceConnectionWithStatus;
import com.devbaltasarq.varse.ui.AppActivity;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.Locale;


/** Shows an activity in which the user can see the obtained bpm data.
  * The device is taken from the static attribute PerformExperimentActivity. */
public class TestHRDevice extends AppActivity implements HRListenerActivity {
    private final static String LOG_TAG = TestHRDevice.class.getSimpleName();
    private final static String CSV_MIME_TYPE = "text/csv";
    private enum Status {  INACTIVE, RECORDING, CANNOT_RECORD }
    private enum Finish{ EXIT, SAVE_AND_EXIT };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView( R.layout.activity_test_hrdevice );
        this.setTitle( "" );

        final ImageButton BT_CLOSE_TEST_DEVICE = this.findViewById( R.id.btCloseTestDevice );
        final ImageButton BT_SAVE = this.findViewById( R.id.btSave );
        final TextView LBL_DEVICE_NAME = this.findViewById( R.id.lblDeviceName );

        // Events
        BT_CLOSE_TEST_DEVICE.setOnClickListener( (view) -> this.finish( Finish.EXIT ) );
        BT_SAVE.setOnClickListener( (view) -> this.finish( Finish.SAVE_AND_EXIT ) );

        // Set device
        this.btDevice = PerformExperimentActivity.chosenBtDevice;
        LBL_DEVICE_NAME.setText( this.btDevice.getName() );

        // Assign
        this.ofm = Ofm.get();
        this.logInfoFile = null;
    }

    public void showStatus(String msg)
    {
        this.showStatus( LOG_TAG, msg );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        BluetoothUtils.openBluetoothConnections( this,
                                                    this.getString( R.string.lblConnected ),
                                                    this.getString( R.string.lblDisconnected ) );
        this.status = Status.INACTIVE;
        this.showInactive();

        // Bluetooth permissions
        final String[] BT_PERMISSIONS_NEEDED =
                BluetoothUtils.fixBluetoothNeededPermissions( this );

        if ( BT_PERMISSIONS_NEEDED.length > 0 ) {
            Toast.makeText( this, R.string.errNoBluetooth, Toast.LENGTH_LONG ).show();
        } else {
            this.startRecording();
        }

        Log.d( LOG_TAG, "UI started, service tried to bound." );
    }

    @Override
    public void onPause()
    {
        super.onPause();

        BluetoothUtils.closeBluetoothConnections( this );
        this.showInactive();

        if ( this.status == Status.RECORDING ) {
            this.stopRecording( Finish.EXIT );
        }

        Log.d(LOG_TAG, "test UI finished, closed connections." );
    }

    public void finish(Finish finishAction)
    {
        if ( finishAction == Finish.SAVE_AND_EXIT ) {
            this.showStatus( LOG_TAG, this.getString( R.string.msgExported ) );
            this.stopRecording( Finish.SAVE_AND_EXIT );
        }

        super.finish();
    }

    @Override
    public boolean askBeforeLeaving()
    {
        return false;
    }

    /** Extracts the info received from the HR service.
     * @param intent The key-value extra collection has at least
     *                BleService.HEART_RATE_TAG for heart rate information (as int),
     *                and it can also have BleService.RR_TAG for the rr info (as int).
     */
    public void onReceiveBpm(Intent intent)
    {
        final int HR = intent.getIntExtra( BleService.HEART_RATE_TAG, -1 );
        final int MEAN_RR = intent.getIntExtra( BleService.MEAN_RR_TAG, -1 );
        final BluetoothDeviceWrapper.BeatInfo BEAT_INFO = new BluetoothDeviceWrapper.BeatInfo();
        int[] rrs = intent.getIntArrayExtra( BleService.RR_TAG );

        if ( this.status == Status.INACTIVE ) {
            this.startRecording();
        }

        BEAT_INFO.set( BluetoothDeviceWrapper.BeatInfo.Info.TIME, this.chrono.getMillis() );
        BEAT_INFO.set( BluetoothDeviceWrapper.BeatInfo.Info.HR, HR );
        BEAT_INFO.set( BluetoothDeviceWrapper.BeatInfo.Info.MEAN_RR, MEAN_RR );
        BEAT_INFO.setRRs( rrs );

        try {
            this.logInfo.write( BEAT_INFO + System.lineSeparator() );
        } catch(IOException exc)
        {
            Log.e( LOG_TAG, "error writing beat info file: " + exc.getMessage()  );
        }

        String strHr = "";
        String strRr = "";

        if ( HR >= 0 ) {
            strHr = Integer.toString( HR );
        }

        if ( MEAN_RR >= 0 ) {
            strRr = Integer.toString( MEAN_RR );
        }

        if ( rrs == null ) {
            this.setRRNotSupported();
        }

        this.showBpm( strHr, strRr );
    }

    private void startRecording()
    {
        final Calendar TIME = Calendar.getInstance();
        final String STR_ISO_TIME = String.format(
                Locale.getDefault(),
                "-%04d-%02d-%02dT%02d-%02d-%02d",
                TIME.get( Calendar.YEAR ),
                TIME.get( Calendar.MONTH + 1 ),
                TIME.get( Calendar.DAY_OF_MONTH ),
                TIME.get( Calendar.HOUR ),
                TIME.get( Calendar.MINUTE ),
                TIME.get( Calendar.SECOND ) );

        try {
            this.logInfoFile = this.ofm.createTempFile(
                                "testhr-" + this.getBtDevice().getName() + "-",
                                    STR_ISO_TIME );
            this.logInfo = Ofm.openWriterFor( this.logInfoFile );
            this.logInfo.write(
                    BluetoothDeviceWrapper.BeatInfo.getInfoHeader()
                    + System.lineSeparator() );
            this.chrono = new Chronometer( this::onChronoUpdate );
            this.chrono.reset();
            this.chrono.start();
            this.status = Status.RECORDING;
        } catch(IOException exc)
        {
            this.logInfo = null;
            this.logInfoFile = null;
            this.status = Status.CANNOT_RECORD;
            Toast.makeText( this, R.string.errIO, Toast.LENGTH_LONG ).show();
        }

        return;
    }

    private void stopRecording(Finish finishAction)
    {
        this.chrono.stop();
        this.status = Status.INACTIVE;

        // Close writer
        if ( this.logInfo != null ) {
            Ofm.close( this.logInfo );
            this.logInfo = null;
        }

        // Save file
        if ( this.logInfoFile != null ) {
            if ( finishAction == Finish.SAVE_AND_EXIT ) {
                this.ofm.saveToDownloads( this.logInfoFile, CSV_MIME_TYPE );
            }

            this.logInfoFile.delete();
            this.logInfoFile = null;
        }

        return;
    }

    /** Shows the info in the appropriate labels. */
    private void showBpm(String bpm, String rr)
    {
        final TextView LBL_BPM = this.findViewById( R.id.lblBpm );
        final TextView LBL_RR = this.findViewById( R.id.lblRR );

        if ( bpm != null
          && !bpm.isEmpty()  )
        {
            TestHRDevice.this.runOnUiThread( () -> LBL_BPM.setText( bpm ) );

            if ( rr != null
              && !rr.isEmpty()  )
            {
                TestHRDevice.this.runOnUiThread( () -> LBL_RR.setText( rr ) );
            } else {
                this.showRRInactive();
            }
        } else {
            this.showInactive();
        }

        return;
    }

    /** Puts a -- in the label, so the user knows the BPM are not being measured. */
    private void showHRInactive()
    {
        TestHRDevice.this.runOnUiThread( () -> {
            final TextView LBL_BPM = this.findViewById( R.id.lblBpm );

            LBL_BPM.setText( "--" );
        });
    }

    /** Puts a -- in the label, so the user knows the RR are not being measured. */
    private void showRRInactive()
    {
        TestHRDevice.this.runOnUiThread( () -> {
            final TextView LBL_RR = this.findViewById( R.id.lblRR );

            LBL_RR.setText( "--" );
        });
    }

    /** Puts a -- in all labels, so the user knows both the RRR the BPM are not being measured. */
    private void showInactive()
    {
        this.showHRInactive();
        this.showRRInactive();
    }

    /** Updates the time.
      * @param wc An instance of the Chronometer.
      */
    private void onChronoUpdate(Chronometer wc)
    {
        final TextView LBL_TIME = this.findViewById( R.id.lblTime );
        final int SECONDS = (int) ( (double) wc.getMillis() / 1000 );

        LBL_TIME.setText( new Duration( SECONDS ).toChronoString() );
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
    public void setServiceConnection(ServiceConnectionWithStatus serviceConnection)
    {
        this.serviceConnection = serviceConnection;
    }

    @Override
    public void setBroadcastReceiver(BroadcastReceiver broadcastReceiver)
    {
        this.broadcastReceiver = broadcastReceiver;
    }

    @Override
    public void setHRNotSupported()
    {
        final LinearLayout LY_HR_NOT_SUPPORTED = this.findViewById( R.id.lyHRNotSupported );

        if ( LY_HR_NOT_SUPPORTED.getVisibility() != View.VISIBLE ) {
            LY_HR_NOT_SUPPORTED.setVisibility( View.VISIBLE );
        }
    }

    @Override
    public void setRRNotSupported()
    {
        final LinearLayout LY_RR_NOT_SUPPORTED = this.findViewById( R.id.lyRRNotSupported );

        if ( LY_RR_NOT_SUPPORTED.getVisibility() != View.VISIBLE ) {
            LY_RR_NOT_SUPPORTED.setVisibility( View.VISIBLE );
        }
    }

    private ServiceConnectionWithStatus serviceConnection;
    private BroadcastReceiver broadcastReceiver;
    private BleService bleService;
    private BluetoothDeviceWrapper btDevice;

    private Ofm ofm;
    private Status status;
    private Writer logInfo;
    private File logInfoFile;
    private Chronometer chrono;
}

package com.devbaltasarq.varse.ui.performexperiment;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.bluetooth.BleService;
import com.devbaltasarq.varse.core.bluetooth.BluetoothDeviceWrapper;
import com.devbaltasarq.varse.core.bluetooth.BluetoothUtils;
import com.devbaltasarq.varse.core.bluetooth.HRListenerActivity;
import com.devbaltasarq.varse.ui.AppActivity;


/** Shows an activity in which the user can see the obtained bpm data.
  * The device is taken from the static attribute PerformExperimentActivity. */
public class TestHRDevice extends AppActivity implements HRListenerActivity {
    private static String LogTag = TestHRDevice.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView( R.layout.activity_test_hrdevice );
        this.setTitle( "" );

        final ImageButton btCloseTestDevice = this.findViewById( R.id.btCloseTestDevice );
        final TextView lblDeviceName = this.findViewById( R.id.lblDeviceName );

        // Events
        btCloseTestDevice.setOnClickListener( (view) -> this.finish() );

        // Set device
        this.btDevice = PerformExperimentActivity.chosenBtDevice;
        lblDeviceName.setText( this.btDevice.getName() );
    }

    public void showStatus(String msg)
    {
        this.showStatus( LogTag, msg );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        BluetoothUtils.openBluetoothConnections( this,
                                                    this.getString( R.string.lblConnected ),
                                                    this.getString( R.string.lblDisconnected ) );
        this.showInactive();

        Log.d( LogTag, "UI started, service tried to bound." );
    }

    @Override
    public void onPause()
    {
        super.onPause();

        BluetoothUtils.closeBluetoothConnections( this );
        this.showInactive();

        Log.d( LogTag, "UI finished, closed connections." );
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
    public void receiveBpm(Intent intent)
    {
        final int hr = intent.getIntExtra( BleService.HEART_RATE_TAG, -1 );
        final int rr = intent.getIntExtra( BleService.RR_TAG, -1 );
        String strHr = "";
        String strRr = "";

        if ( hr >= 0 ) {
            strHr = Integer.toString( hr );
        }

        if ( rr >= 0 ) {
            strRr = Integer.toString( rr );
        }

        this.showBpm( strHr, strRr );
    }

    /** Shows the info in the appropriate labels. */
    private void showBpm(String bpm, String rr)
    {
        final TextView lblBpm = this.findViewById( R.id.lblBpm );
        final TextView lblRR = this.findViewById( R.id.lblRR );


        if ( bpm != null
          && !bpm.isEmpty()  )
        {
            TestHRDevice.this.runOnUiThread( () -> lblBpm.setText( bpm ) );
        } else {
            this.showInactive();
        }

        if ( rr != null
          && !rr.isEmpty()  )
        {
            TestHRDevice.this.runOnUiThread( () -> lblRR.setText( rr ) );
        } else {
            this.showInactive();
        }

        return;
    }

    /** Puts a -- in the label, so the user knows the BPM are not being measured. */
    private void showHRInactive()
    {
        TestHRDevice.this.runOnUiThread( () -> {
            final TextView lblBpm = this.findViewById( R.id.lblBpm );

            lblBpm.setText( "--" );
        });
    }

    /** Puts a -- in the label, so the user knows the RR are not being measured. */
    private void showRRInactive()
    {
        TestHRDevice.this.runOnUiThread( () -> {
            final TextView lblRR = this.findViewById( R.id.lblRR );

            lblRR.setText( "--" );
        });
    }

    /** Puts a -- in all labels, so the user knows both the RRR the BPM are not being measured. */
    private void showInactive()
    {
        this.showHRInactive();
        this.showRRInactive();
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

    private ServiceConnection serviceConnection;
    private BroadcastReceiver broadcastReceiver;
    private BleService bleService;
    private BluetoothDeviceWrapper btDevice;
}

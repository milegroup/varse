package com.devbaltasarq.varse.ui.performexperiment;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.bluetooth.BleService;
import com.devbaltasarq.varse.core.bluetooth.BluetoothDeviceWrapper;
import com.devbaltasarq.varse.core.bluetooth.BluetoothGattWrapper;
import com.devbaltasarq.varse.core.bluetooth.BluetoothUtils;
import com.devbaltasarq.varse.ui.AppActivity;

import java.util.concurrent.ScheduledThreadPoolExecutor;


/** Shows an activity in which the user can see the obtained bpm data.
  * The device is taken from the static attribute PerformExperimentActivity. */
public class TestHRDevice extends AppActivity {
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

        // Prepare callbacks and bind
        this.serviceConnection = this.createServiceConnectionCallBack();
        this.broadcastReceiver = this.createBroadcastReceiverCallBack();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        this.openConnections();
        this.showInactive();

        Log.d( LogTag, "UI started, service tried to bound." );
    }

    @Override
    public void onPause()
    {
        super.onPause();

        this.closeConnections();
        this.showInactive();

        Log.d( LogTag, "UI finished, closed connections." );
    }

    @Override
    public boolean askBeforeLeaving()
    {
        return false;
    }

    /** Turns on all services and callbacks needed to connect to the band. */
    private void openConnections()
    {
        final Intent gattServiceIntent = new Intent( this, BleService.class );

        this.bindService( gattServiceIntent, this.serviceConnection, BIND_AUTO_CREATE );

        Log.d( LogTag, "Binding service for: " + btDevice.getName() );

        // Follow up, once the service is bound, in createServiceConnectionCallback()
    }

    /** Turns down all services and callbacks needed to connect to the band. */
    private void closeConnections()
    {
        Log.d( LogTag, "Closing connections." );

        // Remove the task of watching the heart rate
        if ( this.exec != null ) {
            this.exec.shutdownNow();
            this.exec = null;
        }

        // Unregister receiver
        try {
            this.unregisterReceiver( this.broadcastReceiver );
        } catch(IllegalArgumentException exc) {
            Log.e( LogTag, "closing: not registered yet: " + exc.getMessage() );
        }

        // Disconnect the service
        if ( this.bleService != null ) {
            this.bleService.close();
            this.bleService = null;
        }

        // Unbind the service
        if ( this.serviceConnection != null ) {
            try {
                this.unbindService( this.serviceConnection );
            } catch(IllegalArgumentException exc) {
                Log.e( LogTag, "closing: service not bound yet: " + exc.getMessage() );
            }

            this.serviceConnection = null;
        }

        Log.d( LogTag, "Connections closed." );
    }

    /** Launches a request to find the heart rate measurement from the bluetooth band. */
    private void askForBpm()
    {
        if ( this.hrChar == null ) {
            this.hrChar = BluetoothUtils.getHeartRateChar( this.bleService.getGatt() );
        }

        this.bleService.readCharacteristic( this.hrChar );
        this.showStatus( LogTag, this.getString( R.string.msgReadingHR ) + "..." );
    }

    private void showHR(final String hrm)
    {
        final TextView lblBpm = this.findViewById( R.id.lblBpm );

        TestHRDevice.this.runOnUiThread( () -> lblBpm.setText( hrm ) );
    }

    /** Puts a -- in the label, so the user knows the BPM are not being measured. */
    private void showInactive()
    {
        TestHRDevice.this.runOnUiThread( () -> {
            final TextView lblBpm = this.findViewById( R.id.lblBpm );

            lblBpm.setText( "--" );
            this.showStatus( LogTag, this.getString( R.string.msgStandby ) );
        });
    }

    /** @return a receiver for the events fired by the service. */
    private BroadcastReceiver createBroadcastReceiverCallBack()
    {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                final String action = intent.getAction();
                final TestHRDevice cntxt = TestHRDevice.this;

                // ACTION_GATT_CONNECTED: connected to a GATT server.
                if ( BleService.ACTION_GATT_CONNECTED.equals( action ) ) {
                    cntxt.showStatus( LogTag, cntxt.getString( R.string.lblConnected ) );
                }
                else
                // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
                if ( BleService.ACTION_GATT_DISCONNECTED.equals( action ) ) {
                    cntxt.showStatus( LogTag, cntxt.getString( R.string.lblDisconnected ) );
                }
                else
                if ( BleService.ACTION_GATT_SERVICES_DISCOVERED.equals( action ) ) {
                    final BluetoothGattWrapper gatt = TestHRDevice.this.bleService.getGatt();

                    cntxt.hrChar = BluetoothUtils.getHeartRateChar( gatt );
                    cntxt.askForBpm();
                }
                else
                // ACTION_DATA_AVAILABLE: received data from the device.
                //                        This can be a result of read or notification operations.
                if ( BleService.ACTION_DATA_AVAILABLE.equals( action ) ) {
                    TestHRDevice.this.showHR( intent.getStringExtra( BleService.HEART_RATE_TAG ) );
                }
            }
        };
    }

    private ServiceConnection createServiceConnectionCallBack()
    {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service)
            {
                Log.d( LogTag, "Creating service." );

                TestHRDevice.this.bleService = ( (BleService.LocalBinder) service ).getService();
                TestHRDevice.this.bleService.initialize( TestHRDevice.this.btDevice );

                Log.d( LogTag, "Service bound, connecting." );

                final boolean result = TestHRDevice.this.bleService.connect();

                Log.d( LogTag, "Connect request result: " + result );

                if ( result ) {
                    TestHRDevice.this.registerReceiver(
                        TestHRDevice.this.broadcastReceiver,
                        createBroadcastReceiverIntentFilter() );
                }

                return;
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName)
            {
                TestHRDevice.this.bleService.close();
                TestHRDevice.this.bleService = null;
                TestHRDevice.this.closeConnections();
            }
        };
    }

    private static IntentFilter createBroadcastReceiverIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction( BleService.ACTION_GATT_CONNECTED );
        intentFilter.addAction( BleService.ACTION_GATT_DISCONNECTED );
        intentFilter.addAction( BleService.ACTION_GATT_SERVICES_DISCOVERED );
        intentFilter.addAction( BleService.ACTION_DATA_AVAILABLE );

        return intentFilter;
    }

    private ServiceConnection serviceConnection;
    private BroadcastReceiver broadcastReceiver;
    private BleService bleService;
    private BluetoothDeviceWrapper btDevice;
    private ScheduledThreadPoolExecutor exec;
    private BluetoothGattCharacteristic hrChar;
}

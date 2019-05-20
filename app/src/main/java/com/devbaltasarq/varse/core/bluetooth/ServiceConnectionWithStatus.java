package com.devbaltasarq.varse.core.bluetooth;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/** A service connection that easily makes it possible to query connection status. */
public class ServiceConnectionWithStatus implements ServiceConnection {
    private static final String LogTag = ServiceConnectionWithStatus.class.getSimpleName();

    /** Creates a service connection associated to an activity designed for listening.
     * @param activity An activity that implements the HRListenerActivity interface.
     */
    public ServiceConnectionWithStatus(HRListenerActivity activity)
    {
        this.activity = activity;
        this.connected = false;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service)
    {
        BluetoothUtils.createBleService( activity, service );

        Log.d( LogTag, "Connecting to service..." );
        final boolean result = activity.getService().connect();

        Log.d( LogTag, "Connect request result: " + result );

        if ( result ) {
            Activity context = (Activity) activity;
            context.registerReceiver( activity.getBroadcastReceiver(),
                    BluetoothUtils.createBroadcastReceiverIntentFilter() );
        }

        this.connected = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName)
    {
        BluetoothUtils.closeBluetoothConnections( activity );
        this.connected = false;
    }

    public boolean isConnected()
    {
        return this.connected;
    }

    private boolean connected;
    private HRListenerActivity activity;
}

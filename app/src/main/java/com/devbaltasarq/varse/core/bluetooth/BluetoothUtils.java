package com.devbaltasarq.varse.core.bluetooth;

import static android.content.Context.BIND_AUTO_CREATE;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;


public class BluetoothUtils {
    private static final String LOG_TAG = BluetoothUtils.class.getSimpleName();
    public static final UUID UUID_HR_MEASUREMENT_CHR = UUID.fromString( "00002a37-0000-1000-8000-00805f9b34fb" );
    public static final UUID UUID_HR_MEASUREMENT_SRV = UUID.fromString( "0000180D-0000-1000-8000-00805f9b34fb" );
    public static final UUID UUID_CLIENT_CHAR_CONFIG = UUID.fromString( "00002902-0000-1000-8000-00805f9b34fb" );
    public static final String STR_UNKNOWN_DEVICE = "??";

    private static final String[] BT_PERMISSION_LIST = new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
    };

    public static String[] fixBluetoothNeededPermissions(Context cntxt)
    {
        final ArrayList<String> BUILT_PERMISSIONS =
                new ArrayList<>( Arrays.asList( BT_PERMISSION_LIST ) );

        if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S )
        {
            BUILT_PERMISSIONS.add( Manifest.permission.BLUETOOTH_SCAN );
            BUILT_PERMISSIONS.add( Manifest.permission.BLUETOOTH_CONNECT );
        }

        final ArrayList<String> TORET = new ArrayList<>();

        // Check all permissions
        for(String permissionId: BUILT_PERMISSIONS) {
            int askAnswerBluetooth = ContextCompat.checkSelfPermission(
                    cntxt.getApplicationContext(),
                    permissionId );

            if ( askAnswerBluetooth != PackageManager.PERMISSION_GRANTED ) {
                TORET.add( permissionId );
            }
        }

        BUILT_PERMISSIONS.clear();
        return TORET.toArray( new String[ 0 ] );
    }

    public static BluetoothAdapter getBluetoothAdapter(Context cntxt)
    {
        final BluetoothManager BT_MANAGER =
                (BluetoothManager) cntxt.getSystemService( Context.BLUETOOTH_SERVICE );
        BluetoothAdapter toret = null;

        if ( BT_MANAGER != null ) {
            toret = BT_MANAGER.getAdapter();
        }

        return toret;
    }

    /** @return the HR characteristic from a GATT connection to a device. */
    public static BluetoothGattCharacteristic getHeartRateChar(BluetoothGatt gatt)
    {
        BluetoothGattCharacteristic toret = null;

        if ( gatt != null ) {
            final String DEVICE_NAME = getBTDeviceName( gatt.getDevice() );
            final BluetoothGattService HR_SERVICE = gatt.getService( UUID_HR_MEASUREMENT_SRV );

            if ( HR_SERVICE != null ) {
                toret = HR_SERVICE.getCharacteristic( UUID_HR_MEASUREMENT_CHR );

                if ( toret != null ) {
                    Log.d(LOG_TAG, "Building HR characteristic ("
                                            + toret.getUuid().toString()
                                            + ") in: " + DEVICE_NAME );

                    // Enabling notifications for HR
                    gatt.setCharacteristicNotification( toret, true );

                    Log.d(LOG_TAG, "HR enabled notifications ("
                            + toret.getUuid().toString() + ") in: " + DEVICE_NAME );

                    final BluetoothGattDescriptor DESCRIPTOR = toret.getDescriptor( UUID_CLIENT_CHAR_CONFIG );

                    if ( !DESCRIPTOR.setValue( BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE ) )
                    {
                        Log.e(LOG_TAG, "    Cannot create descriptor for HR in: " + DEVICE_NAME );
                    }

                    if ( !gatt.writeDescriptor( DESCRIPTOR ) ) {
                        Log.e(LOG_TAG, "    Cannot enable notifications for HR in: " + DEVICE_NAME );
                        toret = null;
                    }
                } else {
                    Log.d(LOG_TAG, "No HR characteristic found in: " + DEVICE_NAME );
                }
            } else {
                Log.d(LOG_TAG, "No HR service in: " + DEVICE_NAME );
            }
        }

        if ( toret != null ) {
            Log.d(LOG_TAG, "    Returning built HR char: " + toret.getUuid().toString() );
        }

        return toret;
    }

    /** @return a receiver for the events fired by the service. */
    public static BroadcastReceiver createBroadcastReceiverCallBack(final String MSG_CONNECTED,
                                                                    final String MSG_DISCONNECTED)
    {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                final String ACTION = intent.getAction();
                final HRListenerActivity LISTENER_ACTIVITY = (HRListenerActivity) context;
                final BleService SERVICE = LISTENER_ACTIVITY.getService();

                // ACTION_GATT_CONNECTED: connected to a GATT server.
                if ( BleService.ACTION_GATT_CONNECTED.equals( ACTION ) ) {
                    LISTENER_ACTIVITY.showStatus( MSG_CONNECTED );
                }
                else
                // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
                if ( BleService.ACTION_GATT_DISCONNECTED.equals( ACTION ) ) {
                    LISTENER_ACTIVITY.showStatus( MSG_DISCONNECTED );
                }
                else
                if ( BleService.ACTION_GATT_SERVICES_DISCOVERED.equals( ACTION ) ) {
                    if ( this.hrGattCharacteristic == null ) {
                        this.hrGattCharacteristic =
                            BluetoothUtils.getHeartRateChar( SERVICE.getGatt() );
                    }

                    if ( this.hrGattCharacteristic != null ) {
                        SERVICE.readCharacteristic( this.hrGattCharacteristic );
                        Log.d(LOG_TAG, "Reading hr..." );
                    } else {
                        LISTENER_ACTIVITY.setHRNotSupported();
                        LISTENER_ACTIVITY.setRRNotSupported();
                        Log.e(LOG_TAG, "Won't read hr since was not found..." );
                    }
                }
                else
                // ACTION_DATA_AVAILABLE: received data from the device.
                //                        This can be a result of read or notification operations.
                if ( BleService.ACTION_DATA_AVAILABLE.equals( ACTION ) ) {
                    LISTENER_ACTIVITY.onReceiveBpm( intent );
                }
            }

            private BluetoothGattCharacteristic hrGattCharacteristic;
        };
    }

    /** @return An intent filter suitable for HR measurement. */
    public static IntentFilter createBroadcastReceiverIntentFilter()
    {
        final IntentFilter INTENT_FILTER = new IntentFilter();

        INTENT_FILTER.addAction( BleService.ACTION_GATT_CONNECTED );
        INTENT_FILTER.addAction( BleService.ACTION_GATT_DISCONNECTED );
        INTENT_FILTER.addAction( BleService.ACTION_GATT_SERVICES_DISCOVERED );
        INTENT_FILTER.addAction( BleService.ACTION_DATA_AVAILABLE );

        return INTENT_FILTER;
    }

    /** Sets a suitable service in the caller.
      * @param ACTIVITY the caller of this functionality.
      * @param service the service mechanism.
      */
    public static void createBleService(final HRListenerActivity ACTIVITY, IBinder service)
    {
        Log.d(LOG_TAG, "Binding service..." );

        ACTIVITY.setService( ( (BleService.LocalBinder) service ).getService() );
        ACTIVITY.getService().initialize( ACTIVITY.getBtDevice() );

        Log.d(LOG_TAG, "Service bound." );
    }

    public static ServiceConnectionWithStatus createServiceConnectionCallBack(final HRListenerActivity ACTIVITY)
    {
        return new ServiceConnectionWithStatus( ACTIVITY );
    }

    /** Turns down all services and callbacks needed to connect to the band. */
    public static void closeBluetoothConnections(HRListenerActivity activity)
    {
        final AppCompatActivity CONTEXT = (AppCompatActivity) activity;

        Log.d(LOG_TAG, "Closing connections." );

        // Unregister receiver
        try {
            CONTEXT.unregisterReceiver( activity.getBroadcastReceiver() );
        } catch(IllegalArgumentException exc) {
            Log.e(LOG_TAG, "closing: not registered yet: " + exc.getMessage() );
        }

        // Disconnect the service
        if ( activity.getService() != null ) {
            activity.getService().close();
        }

        // Unbind the service
        if ( activity.getServiceConnection() != null ) {
            try {
                CONTEXT.unbindService( activity.getServiceConnection() );
            } catch(IllegalArgumentException exc) {
                Log.e(LOG_TAG, "closing: service not bound yet: " + exc.getMessage() );
            }
        }

        // Disconnect the demo bt device, provided it is being used
        if ( activity.getBtDevice().isDemo() ) {
            activity.getBtDevice().getDemoDevice().disconnect();
        }

        Log.d(LOG_TAG, "Connections closed." );
    }

    /** Turns on all services and callbacks needed to connect to the band. */
    public static void openBluetoothConnections(HRListenerActivity activity, final String CONN, final String DISCONN)
    {
        final AppCompatActivity CONTEXT = (AppCompatActivity) activity;
        final Intent GATT_SERV_INTENT = new Intent( CONTEXT, BleService.class );

        activity.setServiceConnection( BluetoothUtils.createServiceConnectionCallBack( activity ) );
        activity.setBroadcastReceiver( BluetoothUtils.createBroadcastReceiverCallBack( CONN, DISCONN ) );
        CONTEXT.bindService( GATT_SERV_INTENT, activity.getServiceConnection(), BIND_AUTO_CREATE );

        Log.d(LOG_TAG, "Binding service for: " + activity.getBtDevice().getName() );
        // Follow up, once the service is bound, in createServiceConnectionCallback()
    }

    public static String getBTDeviceName(BluetoothDeviceWrapper btwDevice)
    {
        String toret = "";

        if ( btwDevice != null ) {
            if ( btwDevice.isDemo() ) {
                toret = btwDevice.getName();
            } else {
                toret = getBTDeviceName( btwDevice.getDevice() );
            }
        }

        return toret;
    }

    public static String getBTDeviceName(BluetoothDevice btDevice)
    {
        String toret = "";

        if ( btDevice != null ) {
            final String ADDR = btDevice.getAddress();

            try {
                toret = btDevice.getName();
            } catch(SecurityException exc) {
                toret = ADDR;
            } finally {
                if ( toret == null ) {
                    toret = STR_UNKNOWN_DEVICE;
                }
            }
        }

        return toret;
    }
}

package com.devbaltasarq.varse.core.bluetooth;

import android.Manifest;
import android.app.Activity;
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
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static android.content.Context.BIND_AUTO_CREATE;


public class BluetoothUtils {
    private static final String LOG_TAG = BluetoothUtils.class.getSimpleName();
    public static final UUID UUID_HR_MEASUREMENT_CHR = UUID.fromString( "00002a37-0000-1000-8000-00805f9b34fb" );
    public static final UUID UUID_HR_MEASUREMENT_SRV = UUID.fromString( "0000180D-0000-1000-8000-00805f9b34fb" );
    public static final UUID UUID_CLIENT_CHAR_CONFIG = UUID.fromString( "00002902-0000-1000-8000-00805f9b34fb" );

    private static final String[] BT_PERMISSION_LIST = new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static String[] fixBluetoothNeededPermissions(Context cntxt)
    {
        final ArrayList<String> BUILT_PERMISSIONS = new ArrayList<>(
                Arrays.asList( BT_PERMISSION_LIST ) );

        if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q )
        {
            BUILT_PERMISSIONS.add( Manifest.permission.ACCESS_BACKGROUND_LOCATION );
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
        final String DEVICE_NAME = gatt.getDevice().getName();
        BluetoothGattCharacteristic toret = null;
        BluetoothGattService hrService = gatt.getService( UUID_HR_MEASUREMENT_SRV );

        if ( hrService != null ) {
            toret = hrService.getCharacteristic( UUID_HR_MEASUREMENT_CHR );

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

        if ( toret != null ) {
            Log.d(LOG_TAG, "    Returning built HR char: " + toret.getUuid().toString() );
        }

        return toret;
    }

    // Invoke this when trying to discover the services of a given device.
    // device.connectGatt(context,false,bluetoothGattCallback);
    public static BluetoothGattCallback createGattServiceFilteringCallback(
                                                    Context cntxt,
                                                    final GattServiceConsumer HR_SERV_DISCOVERED,
                                                    final GattServiceConsumer HR_SERVICE_NOT_AVAIL)
    {
        return new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
            {
                super.onConnectionStateChange(gatt, status, newState);

                if ( newState == BluetoothGatt.STATE_CONNECTED
                  && status == BluetoothGatt.GATT_SUCCESS )
                {
                    gatt.discoverServices();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status)
            {
                super.onServicesDiscovered( gatt, status );

                final BluetoothGattCharacteristic HR_CHR = BluetoothUtils.getHeartRateChar( gatt );

                if ( HR_CHR != null ) {
                    HR_SERV_DISCOVERED.consum( gatt.getDevice(), HR_CHR );
                } else {
                    HR_SERVICE_NOT_AVAIL.consum( gatt.getDevice(), null );
                }

                return;
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
            {
                super.onCharacteristicRead( gatt, characteristic, status );
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
            {
                super.onCharacteristicWrite( gatt, characteristic, status );
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
            {
                super.onCharacteristicChanged( gatt, characteristic );
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
            {
                super.onDescriptorRead( gatt, descriptor, status );
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
            {
                super.onDescriptorWrite( gatt, descriptor, status );
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status)
            {
                super.onMtuChanged( gatt, mtu, status );
            }
        };
    }

    public static BroadcastReceiver createActionDeviceDiscoveryReceiver(ScannerUI scanner)
    {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final BluetoothAdapter BT_ADAPTER = getBluetoothAdapter( context );
                String action = intent.getAction();

                if ( BluetoothDevice.ACTION_FOUND.equals( action ) ) {
                    BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
                    scanner.onDeviceFound( device );
                }
                else
                if ( BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals( action ) ) {
                    scanner.startScanning();
                }
                else
                if ( BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals( action ) ) {
                    scanner.stopScanning();
                }
            }
        };
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

    /** @return A suitable BleService. */
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
        final Activity CONTEXT = (Activity) activity;

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
        final Activity CONTEXT = (Activity) activity;
        final Intent GATT_SERV_INTENT = new Intent( CONTEXT, BleService.class );

        activity.setServiceConnection( BluetoothUtils.createServiceConnectionCallBack( activity ) );
        activity.setBroadcastReceiver( BluetoothUtils.createBroadcastReceiverCallBack( CONN, DISCONN ) );
        CONTEXT.bindService( GATT_SERV_INTENT, activity.getServiceConnection(), BIND_AUTO_CREATE );

        Log.d(LOG_TAG, "Binding service for: " + activity.getBtDevice().getName() );
        // Follow up, once the service is bound, in createServiceConnectionCallback()
    }
}

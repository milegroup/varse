package com.devbaltasarq.varse.core.bluetooth;

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
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.UUID;

import static android.content.Context.BIND_AUTO_CREATE;


public class BluetoothUtils {
    private static final String LogTag = BluetoothUtils.class.getSimpleName();
    public static final UUID UUID_HR_MEASUREMENT_CHR = UUID.fromString( "00002a37-0000-1000-8000-00805f9b34fb" );
    public static final UUID UUID_HR_MEASUREMENT_SRV = UUID.fromString( "0000180D-0000-1000-8000-00805f9b34fb" );
    public static final UUID UUID_CLIENT_CHAR_CONFIG = UUID.fromString( "00002902-0000-1000-8000-00805f9b34fb" );

    public static BluetoothAdapter getBluetoothAdapter(Context cntxt)
    {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) cntxt.getSystemService( Context.BLUETOOTH_SERVICE );
        BluetoothAdapter toret = null;

        if ( bluetoothManager != null ) {
            toret = bluetoothManager.getAdapter();
        }

        return toret;
    }

    /** @return the HR characteristic from a GATT connection to a device. */
    public static BluetoothGattCharacteristic getHeartRateChar(BluetoothGatt gatt)
    {
        final String deviceName = gatt.getDevice().getName();
        BluetoothGattCharacteristic toret = null;
        BluetoothGattService hrService = gatt.getService( UUID_HR_MEASUREMENT_SRV );

        if ( hrService != null ) {
            toret = hrService.getCharacteristic( UUID_HR_MEASUREMENT_CHR );

            if ( toret != null ) {
                Log.d( LogTag, "HR characteristic in: " + deviceName );

                final BluetoothGattDescriptor descriptor = toret.getDescriptor( UUID_CLIENT_CHAR_CONFIG );

                gatt.setCharacteristicNotification( toret, true );
                descriptor.setValue( BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE );
                gatt.writeDescriptor( descriptor );
            } else {
                Log.d( LogTag, "No HR characteristic found in: " + deviceName );
            }
        } else {
            Log.d( LogTag, "No HR service in: " + deviceName );
        }

        return toret;
    }

    // Invoke this when trying to discover the services of a given device.
    // device.connectGatt(context,false,bluetoothGattCallback);
    public static BluetoothGattCallback createGattServiceFilteringCallback(
                                                    Context cntxt,
                                                    final GattServiceConsumer hrServiceDiscovered,
                                                    final GattServiceConsumer hrServiceNotAvailable)
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

                final BluetoothGattCharacteristic hrChr = BluetoothUtils.getHeartRateChar( gatt );

                if ( hrChr != null ) {
                    hrServiceDiscovered.consum( gatt.getDevice(), hrChr );
                } else {
                    hrServiceNotAvailable.consum( gatt.getDevice(), null );
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
                final BluetoothAdapter btAdapter = getBluetoothAdapter( context );
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
                final String action = intent.getAction();
                final HRListenerActivity hrListenerAct = (HRListenerActivity) context;

                // ACTION_GATT_CONNECTED: connected to a GATT server.
                if ( BleService.ACTION_GATT_CONNECTED.equals( action ) ) {
                    hrListenerAct.showStatus( MSG_CONNECTED );
                }
                else
                // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
                if ( BleService.ACTION_GATT_DISCONNECTED.equals( action ) ) {
                    hrListenerAct.showStatus( MSG_DISCONNECTED );
                }
                else
                if ( BleService.ACTION_GATT_SERVICES_DISCOVERED.equals( action ) ) {
                    final BluetoothGattCharacteristic hrGattCharacteristic =
                            BluetoothUtils.getHeartRateChar( hrListenerAct.getService().getGatt() );

                    hrListenerAct.getService().readCharacteristic( hrGattCharacteristic );
                    Log.d( LogTag, "Reading hr..." );
                } else {
                    // ACTION_DATA_AVAILABLE: received data from the device.
                    //                        This can be a result of read or notification operations.
                    if ( BleService.ACTION_DATA_AVAILABLE.equals( action ) ) {
                        hrListenerAct.onReceiveBpm( intent );
                    }
                }
            }
        };
    }

    /** @return An intent filter suitable for HR measurement. */
    public static IntentFilter createBroadcastReceiverIntentFilter()
    {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction( BleService.ACTION_GATT_CONNECTED );
        intentFilter.addAction( BleService.ACTION_GATT_DISCONNECTED );
        intentFilter.addAction( BleService.ACTION_GATT_SERVICES_DISCOVERED );
        intentFilter.addAction( BleService.ACTION_DATA_AVAILABLE );

        return intentFilter;
    }

    /** @return A suitable BleService. */
    public static void createBleService(HRListenerActivity activity, IBinder service)
    {
        Log.d( LogTag, "Binding service..." );

        activity.setService( ( (BleService.LocalBinder) service ).getService() );
        activity.getService().initialize( activity.getBtDevice() );

        Log.d( LogTag, "Service bound." );
    }

    public static ServiceConnectionWithStatus createServiceConnectionCallBack(final HRListenerActivity activity)
    {
        return new ServiceConnectionWithStatus( activity );
    }

    /** Turns down all services and callbacks needed to connect to the band. */
    public static void closeBluetoothConnections(HRListenerActivity activity)
    {
        final Activity context = (Activity) activity;

        Log.d( LogTag, "Closing connections." );

        // Unregister receiver
        try {
            context.unregisterReceiver( activity.getBroadcastReceiver() );
        } catch(IllegalArgumentException exc) {
            Log.e( LogTag, "closing: not registered yet: " + exc.getMessage() );
        }

        // Disconnect the service
        if ( activity.getService() != null ) {
            activity.getService().close();
        }

        // Unbind the service
        if ( activity.getServiceConnection() != null ) {
            try {
                context.unbindService( activity.getServiceConnection() );
            } catch(IllegalArgumentException exc) {
                Log.e( LogTag, "closing: service not bound yet: " + exc.getMessage() );
            }
        }

        // Disconnect the demo bt device, provided it is being used
        if ( activity.getBtDevice().isDemo() ) {
            activity.getBtDevice().getDemoDevice().disconnect();
        }

        Log.d( LogTag, "Connections closed." );
    }

    /** Turns on all services and callbacks needed to connect to the band. */
    public static void openBluetoothConnections(HRListenerActivity activity, final String conn, final String disconn)
    {
        final Activity context = (Activity) activity;
        final Intent gattServiceIntent = new Intent( context, BleService.class );

        activity.setServiceConnection( BluetoothUtils.createServiceConnectionCallBack( activity ) );
        activity.setBroadcastReceiver( BluetoothUtils.createBroadcastReceiverCallBack( conn, disconn ) );
        context.bindService( gattServiceIntent, activity.getServiceConnection(), BIND_AUTO_CREATE );

        Log.d( LogTag, "Binding service for: " + activity.getBtDevice().getName() );
        // Follow up, once the service is bound, in createServiceConnectionCallback()
    }
}

package com.devbaltasarq.varse.core.bluetooth;

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
import android.util.Log;

import java.util.UUID;


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
    public static BluetoothGattCharacteristic getHeartRateChar(BluetoothGattWrapper gattw)
    {
        BluetoothGattCharacteristic toret = null;

        if ( gattw.isDemo() ) {
            toret = new BluetoothGattCharacteristic(
                            UUID_HR_MEASUREMENT_CHR,
                            BluetoothGattCharacteristic.PROPERTY_READ
                                    | BluetoothGattCharacteristic.PROPERTY_BROADCAST
                                    | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                            BluetoothGattCharacteristic.PERMISSION_READ );
        } else {
            toret = getHeartRateChar( gattw.getBtGatt() );
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
        final BluetoothAdapter bluetoothAdapter = getBluetoothAdapter( cntxt );

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
}

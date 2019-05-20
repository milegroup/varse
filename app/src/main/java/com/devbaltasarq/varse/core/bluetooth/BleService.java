package com.devbaltasarq.varse.core.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.devbaltasarq.varse.BuildConfig;

import java.util.UUID;


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BleService extends Service {
    private final static String LogTag = BleService.class.getSimpleName();

    private final static String INTENT_PREFIX = BleService.class.getPackage().getName();
    public final static String ACTION_GATT_CONNECTED = INTENT_PREFIX + ".ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = INTENT_PREFIX + ".ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = INTENT_PREFIX + "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = INTENT_PREFIX + ".ACTION_DATA_AVAILABLE";
    public final static String HEART_RATE_TAG = "HEART_RATE";
    public final static String RR_TAG = "RR_DISTANCE";
    public final static String MEAN_RR_TAG = "MEAN_RR_DISTANCE";

    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    /** Initializes the service for connection to a given device.
      * @param btDevice The device to connect to.
      * @return true if correctly initialized, false otherwise.
      */
    public boolean initialize(BluetoothDeviceWrapper btDevice)
    {
        boolean toret = false;

        if ( btDevice != null ) {
            this.btDevice = btDevice;
            this.readingGattCallback = this.createGattCallbackForReading();
            this.adapter = BluetoothUtils.getBluetoothAdapter( this.getBaseContext() );

            if ( this.adapter == null ) {
                Log.e( LogTag, "Unable to get a bluetooth adapter" );
            } else {
                toret = true;
            }
        } else {
            Log.e( LogTag, "Null reference given for BT device" );
        }

        return toret;
    }

    private void broadcastUpdate(final String action)
    {
        final Intent intent = new Intent( action );
        this.sendBroadcast( intent );
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic)
    {
        final UUID UUID_HEART_RATE_CHR = BluetoothUtils.UUID_HR_MEASUREMENT_CHR;
        final Intent intent = new Intent( action );

        // Handling following the Heart Rate Measurement profile.
        if ( UUID_HEART_RATE_CHR.equals( characteristic.getUuid() ) ) {
            final int LENGTH = characteristic.getValue().length;
            final int FLAGS = characteristic.getProperties();
            int heartRate;
            int format;
            int offset = 1;

            // Some valuable debug info
            if ( BuildConfig.DEBUG ) {
                final byte[] DATA = characteristic.getValue();
                final StringBuilder bytes = new StringBuilder( LENGTH * 3 );
                Log.d( LogTag, "HR info received: " + LENGTH + " bytes" );
                Log.d( LogTag, "Flags: " + FLAGS );

                bytes.append( '#' );
                bytes.append( LENGTH );
                bytes.append( ' ' );
                for (byte bt: DATA) {
                    bytes.append(
                            String.format( "%8s",
                                    Integer.toBinaryString( bt & 0xFF ) )
                                    .replace( ' ', '0' ) );
                    bytes.append( ' ' );
                }

                Log.d( LogTag, ":- HR byte sequence { " + bytes.toString() + "}" );
            }

            // Extract the heart rate value format
            if ( ( FLAGS & 1 ) != 0 ) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                heartRate = characteristic.getIntValue( format, offset );
                offset += 2;
                Log.d( LogTag, "Heart rate format UINT16." );
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                heartRate = characteristic.getIntValue( format, offset );
                offset += 1;
                Log.d( LogTag, "Heart rate format UINT8." );
            }

            intent.putExtra( HEART_RATE_TAG, heartRate );
            Log.d( LogTag, String.format("Received heart rate: %d", heartRate ) );

            // Energy Expended Status bit
            if ( ( FLAGS & 8 ) != 0 ) {
                offset += 2;
            }

            // Extract the heart beat distance (RR) bit 4 means that it is present
            int NUM_RRS = ( LENGTH - offset ) / 2;
            int[] RR_DATA = new int[ NUM_RRS ];
            int pos = 0;
            int totalRR = 0;

            if ( ( FLAGS & 16 ) != 0 ) {
                while ( offset < LENGTH ) {
                    Integer objRR = characteristic.getIntValue(
                                                BluetoothGattCharacteristic.FORMAT_UINT16, offset );
                    // So yes, rr is present
                    if ( objRR != null ) {
                        int rr = objRR;

                        // rr = ( v / 1024 ) * 1000
                        Log.d( LogTag, String.format( "Received raw rr (1024-based): %d", rr ) );
                        rr = (int) ( ( (double) rr / 1024 ) * 1000);
                        RR_DATA[ pos ] = rr;
                        totalRR += rr;
                        Log.d( LogTag, String.format( "Received rr: %d", rr ) );
                    } else {
                        Log.e( LogTag, "Missing RR signaled in properties." );
                    }

                    offset += 2;
                    ++pos;
                }

                intent.putExtra( RR_TAG, RR_DATA );
                intent.putExtra( MEAN_RR_TAG, (int) Math.round( (float) totalRR / NUM_RRS ) );
            } else {
                Log.d( LogTag, "RR info was not present." );
            }
        } else {
            Log.w( LogTag, "Read data not for HR profile, instead: "
                                    + characteristic.getUuid().toString()  );
        }

        this.sendBroadcast( intent );
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect()
    {
        boolean toret = false;

        if ( btDevice.isDemo() ) {
            toret = true;
            this.btDevice.connect( this, this.readingGattCallback );
            this.gatt = null;
        }
        else
        if ( this.adapter != null
          && this.btDevice != null )
        {
            final String address = this.btDevice.getAddress();

            if ( this.gatt == null ) {
                Log.d( LogTag, "Trying to create a new connection to: " + address );
                this.gatt = this.btDevice.connect( this, this.readingGattCallback );

                if ( this.gatt == null ) {
                    Log.e( LogTag, "Error trying to create a new connection: " + address );
                } else {
                    toret = true;
                }
            }
            else {
                toret = this.gatt.connect();
            }
        } else {
            if ( this.adapter == null ) {
                Log.e( LogTag, "BT Adapter is null" );
            }

            if ( this.btDevice == null ) {
                Log.e( LogTag, "BT Device is null" );
            }
        }

        return toret;
    }

    /** After using a given BLE device, release all resources. */
    public void close()
    {
        if ( this.gatt != null ) {
            this.gatt.disconnect();
            this.gatt.close();
        }

        this.gatt = null;
    }

    /** Request a read on a given {@code BluetoothGattCharacteristic}.
      * The read result is reported asynchronously through the callback:
      * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}

      * @param characteristic The characteristic to read from.
      */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        if ( this.btDevice != null ) {
            if ( this.adapter != null
              && this.gatt != null )
            {
                this.gatt.readCharacteristic( characteristic );
            } else {
                Log.w( LogTag, "Connection is not ready for: " + this.btDevice.getName() );
            }
        } else {
            Log.e( LogTag, "BT Device is null!!!" );
        }

        return;
    }

    /** @return the bluetooth gatt connection wrapper. */
    public BluetoothGatt getGatt()
    {
        return this.gatt;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind( intent );
    }

    /** Creates the needed callback for reading. */
    private BluetoothGattCallback createGattCallbackForReading()
    {
        // Implements callback methods for GATT events that the app cares about.
        // For example, connection change and services discovered.
        return new BluetoothGattCallback() {
            private static final int STATE_DISCONNECTED = 0;
            private static final int STATE_CONNECTED = 2;

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
            {
                String intentAction;

                if ( newState == BluetoothProfile.STATE_CONNECTED ) {
                    intentAction = ACTION_GATT_CONNECTED;
                    BleService.this.broadcastUpdate( intentAction );
                    gatt.discoverServices();
                    Log.i( LogTag, "Connected to GATT server for reading.");
                }
                else
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    intentAction = ACTION_GATT_DISCONNECTED;
                    Log.i( LogTag, "Disconnected from GATT server." );
                    BleService.this.broadcastUpdate( intentAction );
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status)
            {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    broadcastUpdate( ACTION_GATT_SERVICES_DISCOVERED );
                } else {
                    Log.w( LogTag, "onServicesDiscovered received failed status: " + status);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic,
                                             int status)
            {
                if ( status == BluetoothGatt.GATT_SUCCESS ) {
                    BleService.this.broadcastUpdate( ACTION_DATA_AVAILABLE, characteristic );
                }

                return;
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic)
            {
                BleService.this.broadcastUpdate( ACTION_DATA_AVAILABLE, characteristic );
            }
        };
    }

    private final IBinder binder = new LocalBinder();     // Mandatory since there isn't constructor
    private BluetoothAdapter adapter;
    private BluetoothDeviceWrapper btDevice;
    private BluetoothGatt gatt;
    private BluetoothGattCallback readingGattCallback;
}

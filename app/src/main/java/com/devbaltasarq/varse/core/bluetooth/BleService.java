package com.devbaltasarq.varse.core.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.Debug;
import android.os.IBinder;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BleService extends Service {
    private final static String LogTag = BleService.class.getSimpleName();

    /** This is the min accepted value (in millis), for any read rr (including itself).
     *      100 - 600bpm
     *       50 - 1200bpm
     */
    private final static int MIN_RR_VALUE = 100;


    private static class GattHeartRateCharAnalyzer {
        private final static String LogTag = GattHeartRateCharAnalyzer.class.getSimpleName();

        GattHeartRateCharAnalyzer(final BluetoothGattCharacteristic GATT_CHAR)
        {
            this.GATT_CHAR = GATT_CHAR;
            this.offset = 1;
            this.heartRate = this.meanRRs = -1;
            this.rr = new int[ 0 ];
        }

        int getHeartRate()
        {
            return this.heartRate;
        }

        int[] getRR()
        {
            return this.rr;
        }

        int getMeanRRs()
        {
            return this.meanRRs;
        }

        void extractData()
        {
            final UUID UUID_HEART_RATE_CHR = BluetoothUtils.UUID_HR_MEASUREMENT_CHR;

            // Handling following the Heart Rate Measurement profile.
            if ( UUID_HEART_RATE_CHR.equals( GATT_CHAR.getUuid() ) ) {
                final int FLAGS = GATT_CHAR.getProperties();
                this.offset = 1;

                if ( Debug.isDebuggerConnected() ) {
                    this.logDebugInfoForGattChar();
                }

                this.extractHeartRateData();
                Log.d( LogTag, String.format("Received heart rate: %d", heartRate ) );

                // Energy Expended Status bit
                if ( ( FLAGS & 8 ) != 0 ) {
                    this.offset += 2;
                }

                // Heart beat distance (RR) is present if bit 4 is set
                if ( ( FLAGS & 16 ) != 0 ) {
                    this.extractRRData();
                } else {
                    Log.d( LogTag, "RR info was not present." );
                }
            } else {
                Log.w( LogTag, "Read data not for HR profile, instead: "
                        + GATT_CHAR.getUuid().toString()  );
            }
        }

        private void logDebugInfoForGattChar()
        {
            final int LENGTH = GATT_CHAR.getValue().length;
            final int FLAGS = GATT_CHAR.getProperties();
            final byte[] DATA = GATT_CHAR.getValue();
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

        private void extractHeartRateData()
        {
            final int FLAGS = this.GATT_CHAR.getProperties();

            // Extract the heart rate value format
            if ( ( FLAGS & 1 ) != 0 ) {
                this.heartRate = this.GATT_CHAR.getIntValue(
                        BluetoothGattCharacteristic.FORMAT_UINT16,
                        this.offset );
                this.offset += 2;
                Log.d( LogTag, "Heart rate format UINT16." );
            } else {
                this.heartRate = GATT_CHAR.getIntValue(
                        BluetoothGattCharacteristic.FORMAT_UINT8,
                        this.offset );
                this.offset += 1;
                Log.d( LogTag, "Heart rate format UINT8." );
            }

            return;
        }

        /** Extracts the RR data from the GATT characteristic.
          * Take into account that a first data filter is carried out here:
          * not all values are finally exported.
          * The following criteria es applied:
          * - each rr raw must be above MIN_RR_VALUE.
          */
        private void extractRRData()
        {
            final int LENGTH = this.GATT_CHAR.getValue().length;
            final int EXPECTED_NUM_RRS = ( LENGTH - offset ) / 2;
            int numRRs = 0;
            this.rr = new int[ EXPECTED_NUM_RRS ];

            this.meanRRs = 0;

            while ( this.offset < LENGTH ) {
                Integer valueRR = this.GATT_CHAR.getIntValue(
                                    BluetoothGattCharacteristic.FORMAT_UINT16,
                                    this.offset );

                // So yes, rr is present
                if ( valueRR != null ) {
                    int rr = valueRR;

                    // rr = ( v / 1024 ) * 1000
                    Log.d( LogTag, String.format( "Received raw rr (1024-based): %d", rr ) );
                    rr = (int) ( ( (double) rr / 1024 ) * 1000);

                    if ( rr >= MIN_RR_VALUE ) {
                        this.rr[ numRRs ] = rr;
                        this.meanRRs += rr;
                        ++numRRs;
                        Log.d( LogTag, String.format( "Received rr: %d", rr ) );
                    } else {
                        Log.e( LogTag, String.format( "Received incorrect rr: %d", rr ) );
                    }
                } else {
                    Log.e( LogTag, "Missing RR that was yet signaled in properties." );
                }

                this.offset += 2;
            }

            // Get mean rr and render results
            if ( numRRs > 0 ) {
                if ( numRRs < EXPECTED_NUM_RRS ) {
                    this.rr = Arrays.copyOf( this.rr, numRRs );
                }

                this.meanRRs /= numRRs;
            }

            return;
        }

        final private BluetoothGattCharacteristic GATT_CHAR;
        private int meanRRs;
        private int offset;
        private int heartRate;
        private int[] rr;
    }

    private final static String INTENT_PREFIX = BleService.class.getPackage().getName();
    public final static String ACTION_GATT_CONNECTED = INTENT_PREFIX + ".ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = INTENT_PREFIX + ".ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = INTENT_PREFIX + "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = INTENT_PREFIX + ".ACTION_DATA_AVAILABLE";
    public final static String HEART_RATE_TAG = "HEART_RATE";
    public final static String RR_TAG = "RR_DISTANCE";
    public final static String MEAN_RR_TAG = "MEAN_RR_DISTANCE";

    class LocalBinder extends Binder {
        BleService getService() {
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
                                 final BluetoothGattCharacteristic GATT_CHAR)
    {
        final Intent INTENT = new Intent( action );
        final GattHeartRateCharAnalyzer GATT_ANALYZER = new GattHeartRateCharAnalyzer( GATT_CHAR );

        GATT_ANALYZER.extractData();

        final int[] RR_DATA = GATT_ANALYZER.getRR();

        if ( RR_DATA.length > 0 ) {
            INTENT.putExtra( RR_TAG, GATT_ANALYZER.getRR() );
            INTENT.putExtra( MEAN_RR_TAG, GATT_ANALYZER.getMeanRRs() );
        }

        INTENT.putExtra( HEART_RATE_TAG, GATT_ANALYZER.getHeartRate() );
        this.sendBroadcast( INTENT );
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
            final String deviceId = this.btDevice.toString();

            if ( this.gatt == null ) {
                Log.d( LogTag, "Trying to create a new connection to: " + deviceId );
                this.gatt = this.btDevice.connect( this, this.readingGattCallback );

                if ( this.gatt == null ) {
                    Log.e( LogTag, "Error trying to create a new connection: " + deviceId );
                } else {
                    Log.d( LogTag, "Created a new connection to: " + deviceId );
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
                if ( !this.gatt.readCharacteristic( characteristic ) ) {
                    Log.e( LogTag, "GATT Characteristic invalid: "
                                           + characteristic.getUuid().toString() );
                }
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
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
            {
                String intentAction;

                if ( newState == BluetoothProfile.STATE_CONNECTED ) {
                    intentAction = ACTION_GATT_CONNECTED;
                    BleService.this.broadcastUpdate( intentAction );
                    Log.i( LogTag, "Trying to connect to GATT server for reading.");

                    if ( !gatt.discoverServices() ) {
                        Log.e( LogTag, "Unable to discover services for: " + gatt.getDevice().getName() );
                    }

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

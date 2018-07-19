package com.devbaltasarq.varse.core.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

public class BluetoothGattWrapper {
    private static final String LogTag = BluetoothGattWrapper.class.getSimpleName();

    /** Creates a BtGattWrapper for the demo device. */
    private BluetoothGattWrapper()
    {
        this.init();
    }

    /** Creates a BtGattWrapper for any real device. */
    public BluetoothGattWrapper(BluetoothGatt btGatt)
    {
        this.init();
        this.btGatt = btGatt;
    }

    /** Common initialization. */
    private void init()
    {
        this.btGatt = null;
        this.connected = false;
    }

    /** @return true when the gatt is connected, false otherwise. */
    public boolean isConnected()
    {
        return this.connected;
    }

    /** Connects to the gatt.
      *
      * @return true if the connection was possible, false otherwise.
      */
    public boolean connect()
    {
        boolean toret = true;

        if ( this.btGatt != null ) {
            toret = this.btGatt.connect();
        }

        return toret;
    }

    /** @return true if this wrapper is for a demo GATT; false otherwise. */
    public boolean isDemo()
    {
        return ( this.btGatt == null );
    }

    /** @return the bluetooth GATT enclosed; null if this wrapper is for a demo. */
    public BluetoothGatt getBtGatt()
    {
        return btGatt;
    }

    /** Reads a characteristic.
     * @param characteristic the characteristic to be read from the gatt.
     * @return true if the characteristic could be read, false otherwise.
     */
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        boolean toret;

        Log.d( LogTag, "Reading characteristic: " + characteristic.getUuid().toString() );

        if ( this.btGatt != null ) {
            toret = this.btGatt.readCharacteristic( characteristic );
        } else {
            Log.e( LogTag, "*** Not implemented for Demo devices !!!" );
            toret = false;
        }

        return toret;
    }

    /** Disconnects from the gatt. */
    public void disconnect()
    {
        if ( this.btGatt != null ) {
            this.btGatt.disconnect();
        }

        this.connected = false;
    }

    /** Finally closes the connection to the gatt. */
    public void close()
    {
        if ( this.btGatt != null ) {
            this.btGatt.close();
        }

        this.connected = false;
    }

    /** Return the only instance of this class. */
    public static BluetoothGattWrapper get()
    {
        if ( instance == null ) {
            instance = new BluetoothGattWrapper();
        }

        return instance;
    }

    private BluetoothGatt btGatt;
    private boolean connected;

    private static BluetoothGattWrapper instance;
}

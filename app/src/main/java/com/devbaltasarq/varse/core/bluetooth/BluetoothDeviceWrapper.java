package com.devbaltasarq.varse.core.bluetooth;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;


/** Bridge between real Bluetooth devices and a demo device. */
public class BluetoothDeviceWrapper {
    public String LogTag = BluetoothDeviceWrapper.class.getSimpleName();

    /** Creates a new wrapper, with a real bluetooth device. */
    public BluetoothDeviceWrapper(@NonNull BluetoothDevice bt)
    {
        this.btDevice = bt;
        this.demoDevice = null;
    }

    /** Creates a new wrapper, with the demo device. */
    public BluetoothDeviceWrapper(@NonNull DemoBluetoothDevice demo)
    {
        this.demoDevice = demo;
        this.btDevice = null;
    }

    @Override
    public int hashCode()
    {
        int toret = -1;

        if ( !this.isDemo() ) {
            toret = this.getDevice().hashCode();
        }

        return toret;
    }

    @Override
    public boolean equals(Object other)
    {
        boolean toret = false;

        if ( other instanceof BluetoothDeviceWrapper ) {
            toret = ( this.hashCode() == other.hashCode() );
        }

        return toret;
    }

    /** @return the name of the device, as a String. */
    public String getName() throws Error
    {
        String toret = "";

        if ( this.btDevice != null ) {
            toret = this.btDevice.getName();
        }
        else
        if ( this.demoDevice != null ) {
            toret = this.demoDevice.getName();
        } else {
            throw new Error( "no real device wrapped" );
        }

        return toret;
    }

    /** @return the address of the device, as a String. */
    public String getAddress() throws Error
    {
        String toret = "";

        if ( this.btDevice != null ) {
            toret = this.btDevice.getAddress();
        }
        else
        if ( this.demoDevice != null ) {
            toret = this.demoDevice.getAddress();
        } else {
            throw new Error( "no device wrapped at all" );
        }

        return toret;
    }

    /** Opens a Gatt communication with the device.
      * @param cntxt The context of the activity that is doing the communication.
      * @param callBack The callback containing methods to call when something happens.
      * @return A BluetoothGatt object.
      * @see BluetoothGatt
    */
    public BluetoothGatt connect(Context cntxt, BluetoothGattCallback callBack)
    {
        BluetoothGatt toret = null;

        if ( isDemo() ) {
            this.demoDevice.connect( callBack );
        } else {
            final BluetoothGatt BT_GATT = this.btDevice.connectGatt( cntxt, false, callBack );

            if ( BT_GATT != null ) {
                toret = BT_GATT;
                Log.d( LogTag, "Connected to device." );
            } else {
                Log.e( LogTag, "Could not connect to device." );
            }
        }

        return toret;
    }

    /** Determines whether this is a demo device or not.
      * @return true if this is a demo device, false otherwise.
      */
    public boolean isDemo()
    {
        return ( this.btDevice == null && this.demoDevice != null );
    }

    /** @return the stored BluetoothDevice. This can be null. */
    public BluetoothDevice getDevice()
    {
        return btDevice;
    }

    /** @return the stored DemoBluetoothDevice. This can be null. */
    public DemoBluetoothDevice getDemoDevice()
    {
        return demoDevice;
    }

    @Override
    public String toString()
    {
        return String.format("%s (%s)", this.getName(), this.getAddress() );
    }

    private BluetoothDevice btDevice;
    private DemoBluetoothDevice demoDevice;
}

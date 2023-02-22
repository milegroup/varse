package com.devbaltasarq.varse.core.bluetooth;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;


/** Bridge between real Bluetooth devices and a demo device. */
public class BluetoothDeviceWrapper {
    public String LogTag = BluetoothDeviceWrapper.class.getSimpleName();


    public static class BeatInfo {
        private final static int MAX_NUM_RRS = 10;
        public enum Info { TIME, HR, MEAN_RR }

        public BeatInfo()
        {
            final Info[] INFO_VALUES = Info.values();
            this.data = new HashMap<>( INFO_VALUES.length );
            this.rrs = EMPTY_RRS;

            // Inits
            for(Info info: INFO_VALUES) {
                this.set( info, -1 );
            }
        }

        public void set(@NonNull Info info, long data)
        {
            this.data.put( info, data );
        }

        public void setRRs(int[] data)
        {
            if ( data != null
              && data.length > 0 )
            {
                this.rrs = data.clone();
            } else {
                this.rrs = EMPTY_RRS;
            }

            return;
        }

        public long get(@NonNull Info info)
        {
            return this.data.get( info );
        }

        public int[] getRRs()
        {
            int[] toret = EMPTY_RRS;

            if ( this.rrs != EMPTY_RRS ) {
                toret = this.rrs.clone();
            }

            return toret;
        }

        @Override
        public String toString()
        {
            final String STR_INFO_FMT = "%5d\t%3d\t%4d\t%2d%s";
            final Locale LOCALE = Locale.getDefault();
            final int REAL_LEN = this.rrs.length;

            // Load the rr's info
            final StringBuilder STR_RRS = new StringBuilder();

            for(int i = 0; i < MAX_NUM_RRS; ++i) {
                int valor = -1;

                if ( i < REAL_LEN ) {
                    valor = this.rrs[ i ];
                }

                STR_RRS.append( String.format( LOCALE, "\t%4d", valor ) );
            }

            return String.format(
                    LOCALE,
                    STR_INFO_FMT,
                    this.get( Info.TIME ),
                    this.get( Info.HR ),
                    this.get( Info.MEAN_RR ),
                    this.rrs.length,
                    STR_RRS );
        }

        public static String getInfoHeader()
        {
            if ( INFO_HEADER == null ) {
                final StringBuilder TORET = new StringBuilder( "time\thr\tmean rr\t#rrs" );

                for(int i = 0; i < MAX_NUM_RRS; ++i) {
                    TORET.append( '\t' );
                    TORET.append( "rr" );
                    TORET.append( i + 1 );
                }

                INFO_HEADER = TORET.toString();
            }

            return INFO_HEADER;
        }

        private final HashMap<Info, Long> data;
        private int[] rrs;
        private static String INFO_HEADER;
        private final static int[] EMPTY_RRS = new int[ 0 ];
    }

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
        String toret;

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

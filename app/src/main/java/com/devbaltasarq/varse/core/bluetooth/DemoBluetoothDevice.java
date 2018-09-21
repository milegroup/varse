package com.devbaltasarq.varse.core.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.devbaltasarq.varse.BuildConfig;

import java.util.Random;
import java.util.UUID;

public final class DemoBluetoothDevice {
    private static String LogTag = DemoBluetoothDevice.class.getSimpleName();
    private static String DemoDeviceName = "demo BT device";
    private static String DemoDeviceAddr = "00:00:00:00:00:00";

    /** Creates the bluetooth demo device. */
    private DemoBluetoothDevice()
    {
        this.handler = new Handler();
    }

    /** @return the name of the demo device. */
    public String getName()
    {
        return DemoDeviceName;
    }

    /** @return the address of the demo device. */
    public String getAddress()
    {
        return DemoDeviceAddr;
    }

    private BluetoothGattCharacteristic createHrGattCharacteristic()
    {
        // Flags: 16bit heart rate && rr presence
        final byte PROPERTIES_FLAGS = (byte) ( 1 << 4 | 1 );
        final UUID UUID_HR_MEASUREMENT_CHR = BluetoothUtils.UUID_HR_MEASUREMENT_CHR;
        final BluetoothGattCharacteristic GATT_HR_CHR =
                new BluetoothGattCharacteristic( UUID_HR_MEASUREMENT_CHR,
                        PROPERTIES_FLAGS,
                        BluetoothGattCharacteristic.PERMISSION_READ );
        final int newHR = 60 + ( rnd.nextInt( 30 ) );
        final int newRR = (int) ( ( (double) 60 / newHR ) * 1000 );
        final int adaptedRR = (int) ( ( (double) newRR / 1000 ) * 1024 );
        final byte[] bValue = new byte[ 5 ];
        bValue[ 0 ] = PROPERTIES_FLAGS;

        GATT_HR_CHR.setValue( bValue );
        GATT_HR_CHR.setValue( newHR, BluetoothGattCharacteristic.FORMAT_UINT16, 1 );
        GATT_HR_CHR.setValue( adaptedRR, BluetoothGattCharacteristic.FORMAT_UINT16, 3 );

        // Some valuable debug info
        if ( BuildConfig.DEBUG ) {
            final byte[] value = GATT_HR_CHR.getValue();
            final StringBuilder bytes = new StringBuilder( value.length * 3 );
            Log.d( LogTag, "Flags: " + GATT_HR_CHR.getProperties() );

            for (byte bt: value) {
                bytes.append( Integer.toString( (int) ( (char) bt ) ) );
                bytes.append( ' ' );
            }

            Log.d( LogTag, "HR: " + newHR + ", RR: " + newRR );
            Log.d( LogTag, "Adapted RR: " + adaptedRR  );
            Log.d( LogTag, "To send: { " + bytes.toString() + "}" );
        }

        return GATT_HR_CHR;
    }

    /** Connects to the demo device. */
    public void connect(final BluetoothGattCallback callBack)
    {
        this.handler = new Handler();

        this.sendHR = () -> {
            BluetoothGattCharacteristic GATT_HR_CHR = this.createHrGattCharacteristic();
            callBack.onCharacteristicRead( null, GATT_HR_CHR, BluetoothGatt.GATT_SUCCESS );
            this.handler.postDelayed( this.sendHR,1000);
        };

        this.handler.post( this.sendHR );
    }

    /** Eliminates the daemon sending fake HR data. */
    public void disconnect()
    {
        this.handler.removeCallbacksAndMessages( null );
    }

    /** @return gets the only copy of the demo device. */
    public static DemoBluetoothDevice get()
    {
        if ( rnd == null ) {
            rnd = new Random();
        }

        if ( device == null ) {
            device = new DemoBluetoothDevice();
        }

        return device;
    }

    private Handler handler;
    private Runnable sendHR;

    private static DemoBluetoothDevice device;
    private static Random rnd;
}

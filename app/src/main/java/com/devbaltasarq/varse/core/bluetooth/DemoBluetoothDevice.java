package com.devbaltasarq.varse.core.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Handler;

import java.util.UUID;

public final class DemoBluetoothDevice {
    private static String DemoDeviceName = "demo BT device";
    private static String DemoDeviceAddr = "00:00:00:00:00:00";

    /** Creates the bluetooth demo device. */
    private DemoBluetoothDevice()
    {
        this.handler = new Handler();
        btGattWrapper = BluetoothGattWrapper.get();
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

    /** Connects to the demo device. */
    public BluetoothGattWrapper connect(Context cntxt, BluetoothGattCallback callBack)
    {
        final UUID UUID_HR_MEASUREMENT_CHR = BluetoothUtils.UUID_HR_MEASUREMENT_CHR;
        final BluetoothGattCharacteristic HR_CHR = new BluetoothGattCharacteristic( UUID_HR_MEASUREMENT_CHR, 0, 0 );

        this.context = cntxt;
        readingGattCallback = callBack;
        this.handler = new Handler();

        callBack.onCharacteristicRead(
                null,
                HR_CHR,
                BluetoothGatt.GATT_SUCCESS
        );

        this.sendHR = () -> {
            callBack.onCharacteristicChanged( null, HR_CHR );
            this.handler.postDelayed( this.sendHR,2000);
        };

        return btGattWrapper;
    }

    /** @return gets the only copy of the demo device. */
    public static DemoBluetoothDevice get()
    {
        if ( device == null ) {
            device = new DemoBluetoothDevice();
        }

        return device;
    }

    private Handler handler;
    private Context context;
    private Runnable sendHR;
    private static BluetoothGattCallback readingGattCallback;
    private static BluetoothGattWrapper btGattWrapper;
    private static DemoBluetoothDevice device;
}

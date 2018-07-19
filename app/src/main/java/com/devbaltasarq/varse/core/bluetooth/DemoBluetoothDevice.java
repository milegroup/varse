package com.devbaltasarq.varse.core.bluetooth;

import android.bluetooth.BluetoothGattCallback;
import android.content.Context;

public final class DemoBluetoothDevice {
    private static String DemoDeviceName = "demo BT device";
    private static String DemoDeviceAddr = "00:00:00:00:00:00";

    /** Creates the bluetooth demo device. */
    private DemoBluetoothDevice()
    {
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
        context = cntxt;
        readingGattCallback = callBack;
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

    private static Context context;
    private static BluetoothGattCallback readingGattCallback;
    private static BluetoothGattWrapper btGattWrapper;
    private static DemoBluetoothDevice device;
}

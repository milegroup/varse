package com.devbaltasarq.varse.core.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;


public interface GattServiceConsumer {
    void consum(BluetoothDevice btDevice, BluetoothGattCharacteristic btChr);
}

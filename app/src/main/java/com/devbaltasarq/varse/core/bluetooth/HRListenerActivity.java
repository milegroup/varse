package com.devbaltasarq.varse.core.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.ServiceConnection;

public interface HRListenerActivity {
    ServiceConnection getServiceConnection();
    BroadcastReceiver getBroadcastReceiver();

    void setServiceConnection(ServiceConnection serviceConnection);
    void setBroadcastReceiver(BroadcastReceiver broadcastReceiver);

    BleService getService();
    void setService(BleService service);

    BluetoothDeviceWrapper getBtDevice();

    void showStatus(String msg);

    void receiveBpm(Intent intent);
}

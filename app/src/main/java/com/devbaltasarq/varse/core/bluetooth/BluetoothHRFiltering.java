package com.devbaltasarq.varse.core.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BluetoothHRFiltering {
    private static final String LOG_TAG = BluetoothHRFiltering.class.getSimpleName();
    private static int MAX_FILTERING_PERIOD = 25000;

    public BluetoothHRFiltering(ScannerUI ui)
    {
        this.ui = ui;
        this.filtering = false;
        this.handler = null;
        this.clearLists();
    }

    /** Start the process of filtering the devices.
      * @param btDevices An array of devices to manage.
      */
    public void filter(BluetoothDevice[] btDevices)
    {
        if ( !this.filtering ) {
            this.handler = new Handler( Looper.getMainLooper() );

            final int NUM_DEVICES = btDevices.length;

            this.btDevices = Arrays.copyOf( btDevices, NUM_DEVICES );

            this.clearLists();
            this.filtering = true;

            Log.d(LOG_TAG, "Start filtering for " + NUM_DEVICES + " devices." );

            if ( NUM_DEVICES > 0 ) {
                this.handler.postDelayed( this::finishedFiltering, MAX_FILTERING_PERIOD );

                for(BluetoothDevice btDevice: this.btDevices) {
                    if ( !filtering ) {
                        break;
                    }

                    this.filterByHRService( btDevice );
                }
            } else {
                this.finishedFiltering();
            }
        }

        return;
    }

    // Finished filtering
    private void finishedFiltering()
    {
        if ( this.filtering ) {
            Log.d(LOG_TAG, "Filtering forced finish." );
        }

        this.filtering = false;
        this.closeAllGattConnections();
        this.ui.filteringFinished();

        return;
    }

    /** Clears the list of open connections. */
    private void clearLists()
    {
        if ( this.openConnections == null ) {
            this.openConnections = new HashMap<>();
        }

        if ( this.acceptedDevices == null ) {
            this.acceptedDevices = new ArrayList<>();
        }

        if ( this.rejectedDevices == null ) {
            this.rejectedDevices = new ArrayList<>();
        }

        this.openConnections.clear();
        this.acceptedDevices.clear();
        this.rejectedDevices.clear();
    }

    /** @return the list of **all** devices filtered (accepted or rejected). */
    public BluetoothDevice[] getBtDevices()
    {
        return Arrays.copyOf( this.btDevices, this.btDevices.length );
    }

    /** @return the list of accepted devices. */
    public BluetoothDevice[] getAcceptedBtDevices()
    {
        return this.acceptedDevices.toArray( new BluetoothDevice[0] );
    }

    /** @return the list of rejected devices. */
    public BluetoothDevice[] getRejectedBtDevices()
    {
        return this.rejectedDevices.toArray( new BluetoothDevice[0] );
    }

    /** Closes all open GATT connections. */
    public void closeAllGattConnections()
    {
        for(BluetoothDevice btDevice: this.openConnections.keySet()) {
            this.closeGattConnection( btDevice, false );
        }

        this.openConnections.clear();

        if ( this.handler != null ) {
            this.handler.removeCallbacksAndMessages( null );
            this.handler = null;
        }

        return;
    }

    /** Closes an open GATT connection.
     * @param btDevice The device to close the GATT connection for.
     */
    private void closeGattConnection(BluetoothDevice btDevice)
    {
        this.closeGattConnection( btDevice, true );
    }

    /** Closes an open GATT connection.
     * @param btDevice The device to close the GATT connection for.
     * @param removeIt Whether to remove the device from the list or not.
     */
    private void closeGattConnection(BluetoothDevice btDevice, boolean removeIt)
    {
        final BluetoothGatt BT_GATT = this.openConnections.get( btDevice );

        if ( btDevice != null ) {
            if ( BT_GATT != null ) {
                BT_GATT.disconnect();
                BT_GATT.close();
            }

            if ( removeIt ) {
                this.openConnections.remove( btDevice );
            }

            Log.d(LOG_TAG, "Closed Gatt connection for: " + btDevice.getName() );
            Log.d(LOG_TAG, "Remaining connections: " + this.openConnections.size() );
        }

        return;
    }

    private void removeClosedGattConnections()
    {
        final ArrayList<BluetoothDevice> BT_DEVICES_TO_REMOVE = new ArrayList<>();
        final Set<Map.Entry<BluetoothDevice, BluetoothGatt>> ENTRIES = this.openConnections.entrySet();

        // Find those devices to remove
        for(Map.Entry<BluetoothDevice, BluetoothGatt> entry: ENTRIES)
        {
            final Context CONTEXT = this.ui.getContext();
            final BluetoothDevice BT_DEVICE = entry.getKey();
            final BluetoothManager BT_MANAGER = (BluetoothManager) CONTEXT.getSystemService( Context.BLUETOOTH_SERVICE );
            final int connectionStatus = BT_MANAGER.getConnectionState( BT_DEVICE, BluetoothGatt.GATT );

            if ( connectionStatus == BluetoothGatt.STATE_DISCONNECTED
              || connectionStatus == BluetoothGatt.STATE_DISCONNECTING )
            {
                BT_DEVICES_TO_REMOVE.add( BT_DEVICE );
            }
        }

        // Remove them
        for(BluetoothDevice btDevice: BT_DEVICES_TO_REMOVE) {
            this.openConnections.remove( btDevice );
        }

        return;
    }

    private void closeGattSignalWhenFinished(BluetoothDevice btDevice)
    {
        this.closeGattConnection( btDevice );
        this.removeClosedGattConnections();

        if ( this.openConnections.size() == 0 ) {
            Log.d(LOG_TAG, "Finished filtering, signaling UI..." );
            this.filtering = false;
            this.finishedFiltering();
        }

        return;
    }

    /** Accepts a device for it has passed the filter.
      * @param btDevice The device to accept.
      */
    private void filterInDevice(BluetoothDevice btDevice)
    {
        Log.d(LOG_TAG, "Device accepted: " + btDevice.getName() );

        this.acceptedDevices.add( btDevice );
        this.ui.addDeviceToListView( btDevice );
        this.closeGattSignalWhenFinished( btDevice );
    }

    /** Rejects a device for it has not passed the filter.
      * @param btDevice The device to reject.
      */
    private void filterOutDevice(BluetoothDevice btDevice)
    {
        Log.d(LOG_TAG, "Device rejected: " + btDevice.getName() );

        this.rejectedDevices.add( btDevice );
        this.ui.denyAdditionToList( btDevice );
        this.closeGattSignalWhenFinished( btDevice );
    }

    /** Does the filtering for a given device.
      * @param btDevice The device to filter.
      */
    private void filterByHRService(BluetoothDevice btDevice)
    {
        if ( this.openConnections.get( btDevice ) == null ) {
            Log.d(LOG_TAG, "Opening connection for: " + btDevice.getName() );

            final BluetoothGatt BT_GATT = btDevice.connectGatt(
                    this.ui.getContext(),false,
                    BluetoothUtils.createGattServiceFilteringCallback(
                            this.ui.getContext(),
                            ( device, gattChr ) -> this.filterInDevice( btDevice ),
                            ( device, gattChr ) -> this.filterOutDevice( btDevice )
                    ));

            this.openConnections.put( btDevice, BT_GATT );
        }

        return;
    }

    private HashMap<BluetoothDevice, BluetoothGatt> openConnections;
    private ScannerUI ui;
    private boolean filtering;
    private BluetoothDevice[] btDevices;
    private ArrayList<BluetoothDevice> acceptedDevices;
    private ArrayList<BluetoothDevice> rejectedDevices;
    private Handler handler;
}

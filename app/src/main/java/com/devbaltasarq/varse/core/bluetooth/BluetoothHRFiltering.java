package com.devbaltasarq.varse.core.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BluetoothHRFiltering {
    private static final String LogTag = BluetoothHRFiltering.class.getSimpleName();
    private static int MAX_FILTERING_PERIOD = 25000;

    public BluetoothHRFiltering(ScannerUI ui)
    {
        this.ui = ui;
        this.filtering = false;
        this.clearLists();
        this.handler = new Handler();
    }

    /** Start the process of filtering the devices.
      * @param btDevices An array of devices to manage.
      */
    public void filter(BluetoothDevice[] btDevices)
    {
        if ( !this.filtering ) {
            final int NUM_DEVICES = btDevices.length;

            this.btDevices = Arrays.copyOf( btDevices, NUM_DEVICES );

            this.clearLists();
            this.filtering = true;

            Log.d( LogTag, "Start filtering for " + NUM_DEVICES + " devices." );

            if ( this.btDevices.length > 0 ) {
                this.handler.postDelayed( () -> {
                    if ( this.filtering ) {
                        Log.d( LogTag, "Filtering forced finish." );

                        this.filtering = false;
                        this.closeAllGattConnections();
                        this.ui.filteringFinished();
                    }
                }, MAX_FILTERING_PERIOD );

                for(BluetoothDevice btDevice: this.btDevices) {
                    if ( !filtering ) {
                        break;
                    }

                    this.filterByHRService( btDevice );
                }
            }
        }

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
        this.handler.removeCallbacksAndMessages( null );

        for(BluetoothDevice btDevice: this.openConnections.keySet()) {
            this.closeGattConnection( btDevice, false );
        }

        this.openConnections.clear();
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
        final BluetoothGatt btGatt = this.openConnections.get( btDevice );

        if ( btDevice != null ) {
            if ( btGatt != null ) {
                btGatt.disconnect();
                btGatt.close();
            }

            if ( removeIt ) {
                this.openConnections.remove( btDevice );
            }

            Log.d( LogTag, "Closed Gatt connection for: " + btDevice.getName() );
            Log.d( LogTag, "Remaining connections: " + this.openConnections.size() );
        }

        return;
    }

    private void removeClosedGattConnections()
    {
        final ArrayList<BluetoothDevice> btDevicesToRemove = new ArrayList<>();
        final Set<Map.Entry<BluetoothDevice, BluetoothGatt>> entries = this.openConnections.entrySet();

        // Find those devices to remove
        for(Map.Entry<BluetoothDevice, BluetoothGatt> entry: entries)
        {
            final Context cntxt = this.ui.getContext();
            final BluetoothDevice btDevice = entry.getKey();
            final BluetoothManager btManager = (BluetoothManager) cntxt.getSystemService( Context.BLUETOOTH_SERVICE );
            final int connectionStatus = btManager.getConnectionState( btDevice, BluetoothGatt.GATT );

            if ( connectionStatus == BluetoothGatt.STATE_DISCONNECTED
              || connectionStatus == BluetoothGatt.STATE_DISCONNECTING )
            {
                btDevicesToRemove.add( btDevice );
            }
        }

        // Remove them
        for(BluetoothDevice btDevice: btDevicesToRemove) {
            this.openConnections.remove( btDevice );
        }

        return;
    }

    private void closeGattSignalWhenFinished(BluetoothDevice btDevice)
    {
        this.closeGattConnection( btDevice );
        this.removeClosedGattConnections();

        if ( this.openConnections.size() == 0 ) {
            Log.d( LogTag, "Finished filtering, signaling UI..." );
            filtering = false;
            this.ui.filteringFinished();
        }

        return;
    }

    /** Accepts a device for it has passed the filter.
      * @param btDevice The device to accept.
      */
    private void filterInDevice(BluetoothDevice btDevice)
    {
        Log.d( LogTag, "Device accepted: " + btDevice.getName() );

        this.acceptedDevices.add( btDevice );
        this.ui.addDeviceToListView( btDevice );
        this.closeGattSignalWhenFinished( btDevice );
    }

    /** Rejects a device for it has not passed the filter.
      * @param btDevice The device to reject.
      */
    private void filterOutDevice(BluetoothDevice btDevice)
    {
        Log.d( LogTag, "Device rejected: " + btDevice.getName() );

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
            Log.d( LogTag, "Opening connection for: " + btDevice.getName() );

            final BluetoothGatt btGatt = btDevice.connectGatt(
                    this.ui.getContext(),false,
                    BluetoothUtils.createGattServiceFilteringCallback(
                            this.ui.getContext(),
                            (device, gattChr ) -> this.filterInDevice( btDevice ),
                            (device, gattChr ) -> this.filterOutDevice( btDevice )
                    ));

            this.openConnections.put( btDevice, btGatt );
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

package com.devbaltasarq.varse.ui.performexperiment;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.bluetooth.BluetoothDeviceWrapper;
import com.devbaltasarq.varse.core.bluetooth.DemoBluetoothDevice;
import com.devbaltasarq.varse.ui.AppActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BluetoothLeScannerPerformExperimentActivity extends AppActivity {
    private final String LogTag = "PerformExperiment";
    private static final int REQUEST_ENABLE_BT = 367;
    private static final long SCAN_PERIOD = 20000;  // Stop scanning after seconds*1000

    // Adapter for holding devices found through scanning.
    private static class LeDeviceListAdapter extends ArrayAdapter<BluetoothDeviceWrapper> {
        LeDeviceListAdapter(@NonNull Context cntxt, @NonNull List<BluetoothDeviceWrapper> entries)
        {
            super( cntxt, 0, entries );
        }

        @Override
        public @NonNull View getView(int i, View view, @NonNull ViewGroup viewGroup)
        {
            final LayoutInflater inflater = LayoutInflater.from( this.getContext() );

            if ( view == null ) {
                view = inflater.inflate( R.layout.listview_device_entry, null );
            }

            final BluetoothDeviceWrapper device = this.getItem( i );
            final TextView lblDeviceName = view.findViewById( R.id.lblDeviceName );
            final TextView lblDeviceAddress = view.findViewById( R.id.lblDeviceAddress );
            String deviceName = this.getContext().getString( R.string.ErrUnknownDevice );
            String deviceAddress = "00:00:00:00:00:00";

            // Set device's name, if possible.
            if ( device != null ) {
                deviceName = device.getName();
                deviceAddress = device.getAddress();
            }

            // Check the final name, is it valid?
            if ( deviceName == null
              || deviceName.isEmpty() )
            {
                lblDeviceName.setText( R.string.ErrUnknownDevice );
            } else {
                lblDeviceName.setText( deviceName );
            }

            lblDeviceAddress.setText( deviceAddress );
            return view;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_perform_experiment );
        Toolbar toolbar = findViewById( R.id.toolbar );
        this.setSupportActionBar( toolbar );
        this.setTitle( "" );

        // Widgets
        final ImageButton btClosePerformExperiment = this.findViewById( R.id.btClosePerformExperiment );
        final ImageButton btStartScan = this.findViewById( R.id.btStartScan );
        final ImageButton btStopScan = this.findViewById( R.id.btStopScan );

        btClosePerformExperiment.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothLeScannerPerformExperimentActivity.this.finish();
            }
        });

        btStartScan.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothLeScannerPerformExperimentActivity.this.scanLeDevices( true );
            }
        });

        btStopScan.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothLeScannerPerformExperimentActivity.this.scanLeDevices( false );
            }
        });

        this.createList();
        this.configBtLaunched = false;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        this.isBleSupported = true;
        this.initLeBt();

        if ( this.isBleSupported ) {
            this.scanCallback = this.createScanCallBack( this );

            // Ensures Bluetooth is enabled on the device.
            // If Bluetooth is not currently enabled,
            // fire an intent to display a dialog asking the user
            // to grant permission to enable it.
            this.scanning = false;
            this.muted = false;

            if ( this.bluetoothAdapter == null ) {
                this.launchBtConfigPage();
            }
            else
            if ( !this.bluetoothAdapter.isEnabled() )
            {
                this.launchBtConfigPage();
            } else {
                // Obtains the data and scans devices
                this.obtainData();
                this.scanLeDevices( true );
            }
        }

        return;
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        this.muted = true;
        this.scanLeDevices( false );
        this.clearDeviceListView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if ( menu != null ) {
            this.scanMenu = menu;

            if ( this.isBleSupported ) {
                if ( menu.size() == 0 ) {
                    this.getMenuInflater().inflate( R.menu.menu_scan_devices, menu );
                }

                if ( !this.scanning ) {
                    menu.findItem( R.id.menu_start_scan ).setVisible( true );
                    menu.findItem( R.id.menu_stop_scan ).setVisible( false );
                    menu.findItem( R.id.menu_refresh_scan ).setVisible( false );
                } else {
                    menu.findItem( R.id.menu_start_scan ).setVisible( false );
                    menu.findItem( R.id.menu_stop_scan ).setVisible( true );
                    menu.findItem( R.id.menu_refresh_scan ).setVisible( true );
                    menu.findItem( R.id.menu_refresh_scan ).setActionView(
                            R.layout.actionbar_indeterminate_progress );
                }
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch ( item.getItemId() ) {
            case R.id.menu_start_scan:
                this.scanLeDevices( true );
                break;
            case R.id.menu_stop_scan:
                this.scanLeDevices( false );
                break;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult( requestCode, resultCode, data );

        if ( requestCode == REQUEST_ENABLE_BT ) {
            this.configBtLaunched = false;

            if ( resultCode == Activity.RESULT_CANCELED ) {
                this.finish();
            }
        }
    }

    /** Creates the list of devices. */
    private void createList()
    {
        final ListView lvDevices = this.findViewById( R.id.lvDevices );

        this.devices = new ArrayList<>();
        this.devicesListAdapter = new LeDeviceListAdapter( this, this.devices );

        lvDevices.setAdapter( this.devicesListAdapter );
        this.clearDeviceListView();
    }

    /** Removes all devices in the list. */
    private void clearDeviceListView()
    {
        this.devicesListAdapter.clear();
        this.devicesListAdapter.add( new BluetoothDeviceWrapper( DemoBluetoothDevice.get() ) );
    }

    /** Adds a given device to the list.
      * @param btDevice the bluetooth LE device.
      */
    private void addToDeviceListView(BluetoothDevice btDevice)
    {
        this.devicesListAdapter.add( new BluetoothDeviceWrapper( btDevice ) );
        this.devicesListAdapter.notifyDataSetChanged();

        this.showStatus( this.getString( R.string.lblDeviceFound ) + ": " + btDevice.getName() );
    }

    /** Initializes Low Energy Bluetooth. */
    private void initLeBt()
    {
        this.isBleSupported = false;
        this.handler = new Handler();

        // BLE supported on the device?
        if ( this.getPackageManager().hasSystemFeature( PackageManager.FEATURE_BLUETOOTH_LE ) ) {
            // Initializes a Bluetooth adapter
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService( Context.BLUETOOTH_SERVICE );

            if ( bluetoothManager != null ) {
                this.bluetoothAdapter = bluetoothManager.getAdapter();

                // Checks if Bluetooth is supported on the device.
                if ( this.bluetoothAdapter != null ) {
                    this.clearDeviceListView();
                    this.isBleSupported = true;
                } else {
                    this.disableScanForBleIsNotAvailable();
                }
            } else {
                this.disableScanForBleIsNotAvailable();
            }
        } else {
            this.disableScanForBleIsNotAvailable();
        }

        return;
    }

    /** No BLE available, disable scan. */
    private void disableScanForBleIsNotAvailable()
    {
        this.showStatus( Log.WARN, this.getString( R.string.ErrNoBluetooth) );
        this.disableLaunchAndFurtherScan();
    }

    /** Shows an info status on screen. */
    private void showStatus(String msg)
    {
        this.showStatus( Log.INFO, msg );
    }

    /** Shows a given status on screen. */
    private void showStatus(int priority, String msg)
    {
        int msgColor = Color.BLUE;
        int duration = Snackbar.LENGTH_LONG;

        if ( priority == Log.INFO ) {
            duration = Snackbar.LENGTH_SHORT;
        }
        else
        if ( priority == Log.ERROR ) {
            msgColor = Color.RED;
        }

        Log.println( priority, LogTag, msg );

        if ( !this.muted ) {
            Snackbar.make(
                    findViewById( android.R.id.content ), msg, duration )
                    .setActionTextColor( msgColor )
                    .show();
        }
    }

    /** Hides the stop scanning button and option menu, shows the opposite options. */
    private void disableScanUI()
    {
        this.activateScanUI( false );
    }

    /** Hides the start scanning button and option menu, shows the opposite options. */
    private void enableScanUI()
    {
        this.activateScanUI( true );
    }

    /** Hides ot shows the stop scanning button/start scanning button.
     * @param activate true to show start scan options and hide the stop scan options,
     *                  false to do the opposite. */
    private void activateScanUI(boolean activate)
    {
        final ImageButton btStartScan = this.findViewById( R.id.btStartScan );
        final ImageButton btStopScan = this.findViewById( R.id.btStopScan );


        if ( this.isBleSupported) {
            if ( !activate ) {
                btStartScan.setVisibility( View.VISIBLE );
                btStopScan.setVisibility( View.INVISIBLE );
            } else {
                btStartScan.setVisibility( View.INVISIBLE );
                btStopScan.setVisibility( View.VISIBLE );
            }
        } else {
            btStartScan.setVisibility( View.INVISIBLE );
            btStopScan.setVisibility( View.INVISIBLE );
        }

        this.onCreateOptionsMenu( this.scanMenu );
    }

    /** Launches the bluetooth configuration page */
    private void launchBtConfigPage()
    {
        if ( !this.configBtLaunched ) {
            this.showStatus( this.getString( R.string.lblActivateBluetooth ) );

            this.configBtLaunched = true;
            Intent enableBtIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
            this.startActivityForResult( enableBtIntent, REQUEST_ENABLE_BT );
        }

        return;
    }

    /** Launches scanning for a given period of time */
    private void startScanning()
    {
        // Scan only for a given period
        this.clearDeviceListView();
        this.handler.postDelayed( new Runnable() {
            @Override
            public void run()
            {
                final BluetoothLeScannerPerformExperimentActivity act = BluetoothLeScannerPerformExperimentActivity.this;

                act.stopScanning();

                runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        act.disableScanUI();
                    }
                });
            }
        }, SCAN_PERIOD );

        this.scanning = true;
        /*ScanSettings scanSettings = new ScanSettings.Builder()
                                    .setScanMode( ScanSettings.SCAN_MODE_BALANCED )
                                    .build();
        this.bluetoothAdapter.getBluetoothLeScanner().startScan(
                                    new ArrayList<ScanFilter>(){},
                                    scanSettings,
                                    this.scanCallback );
                                    */
        this.bluetoothAdapter.getBluetoothLeScanner().startScan( this.scanCallback );

        // Prepare UI
        this.enableScanUI();
        this.showStatus( this.getString( R.string.lblStartScan ) );
    }

    /** Stops scanning, removing all pending callbacks (that would stop scanning) */
    private void stopScanning()
    {
        this.scanning = false;

        this.handler.removeCallbacksAndMessages( null );
        this.bluetoothAdapter.getBluetoothLeScanner().flushPendingScanResults( this.scanCallback );
        this.bluetoothAdapter.getBluetoothLeScanner().stopScan( this.scanCallback );
        this.showStatus( this.getString( R.string.lblStopScan ) );
    }

    /** Enables the launch button or not. */
    private void disableLaunchAndFurtherScan()
    {
        final ImageButton btStartScan = this.findViewById( R.id.btStartScan );

        this.disableScanUI();
        btStartScan.setEnabled( false );
        btStartScan.setVisibility( View.INVISIBLE );
    }

    /** Reads the data from the ORM. */
    private void obtainData()
    {
        // Read experiment's and user's names
        try {
            final Orm dataStore = Orm.get();

            // Read user's names and experiment's names
            final String[] userNames = dataStore.enumerateUserNames();
            final String[] experimentNames = dataStore.enumerateExperimentNames();

            this.showStatus( "Retrieved users: "
                    + userNames.length
                    + " and "
                    + experimentNames.length
                    + " experiments" );

            // Spinner users
            final AutoCompleteTextView cbUsers = this.findViewById( R.id.cbUsers);
            final ArrayAdapter<String> adapterUsers = new ArrayAdapter<>( this,
                    android.R.layout.simple_spinner_item,
                    userNames );
            adapterUsers.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
            cbUsers.setAdapter( adapterUsers );

            if ( userNames.length > 0 ) {
                cbUsers.setText( userNames[ 0 ] );
                cbUsers.setSelection( 0, cbUsers.getText().toString().length() );
            }

            // Spinner experiments
            final Spinner cbExperiments = this.findViewById( R.id.cbExperiments );
            final ArrayAdapter<String> adapterExperiments = new ArrayAdapter<>( this,
                    android.R.layout.simple_spinner_item,
                    experimentNames );
            adapterExperiments.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
            cbExperiments.setAdapter( adapterExperiments );
        } catch(IOException exc) {
            this.disableLaunchAndFurtherScan();
            this.showStatus( Log.ERROR, this.getString( R.string.ErrIO ) );
        }
    }

    /** Starts scanning devices.
      * @param enable Determines whether to start scanning or stop it completely.
      */
    private void scanLeDevices(final boolean enable)
    {
        if ( this.isBleSupported ) {
            this.disableScanUI();

            if ( this.bluetoothAdapter != null
              && this.bluetoothAdapter.isEnabled() )
            {
                if ( enable ) {
                    this.startScanning();
                } else {
                    this.stopScanning();
                }

                this.invalidateOptionsMenu();
            }
        }

        return;
    }

    private ScanCallback createScanCallBack(final BluetoothLeScannerPerformExperimentActivity context)
    {
        return new ScanCallback() {
            @Override
            public void onBatchScanResults(final List<ScanResult> scanResults)
            {
                super.onBatchScanResults( scanResults );

                context.runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        context.showStatus( "Batched devices: " + scanResults.size() );
                    }
                });
            }

            @Override
            public void onScanFailed(int errorCode)
            {
                super.onScanFailed( errorCode );

                context.runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        context.showStatus( Log.ERROR, context.getString( R.string.ErrScanFailed ) );
                        context.scanLeDevices( false );
                    }
                });
            }

            @Override
            public void onScanResult(int callbackType, final ScanResult scanResult)
            {
                super.onScanResult( callbackType, scanResult );

                context.runOnUiThread( new Runnable() {
                    @Override
                    public void run() {
                        final BluetoothDevice btDevice = scanResult.getDevice();
                        final String infoMsg = context.getString( R.string.lblDeviceFound )
                                + ": " + btDevice.getAddress();

                        context.showStatus( infoMsg );
                        context.addToDeviceListView( btDevice );
                    }
                });
            }
        };
    }

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDeviceWrapper> devices;
    private LeDeviceListAdapter devicesListAdapter;
    private boolean scanning = false;
    private Handler handler;
    private Menu scanMenu;
    private boolean muted;
    private boolean isBleSupported;
    private boolean configBtLaunched;
    private ScanCallback scanCallback;
}

package com.devbaltasarq.varse.ui.performexperiment;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.PartialObject;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.core.User;
import com.devbaltasarq.varse.core.bluetooth.BluetoothDeviceWrapper;
import com.devbaltasarq.varse.core.bluetooth.BluetoothHRFiltering;
import com.devbaltasarq.varse.core.bluetooth.BluetoothUtils;
import com.devbaltasarq.varse.core.bluetooth.DemoBluetoothDevice;
import com.devbaltasarq.varse.core.bluetooth.ScannerUI;
import com.devbaltasarq.varse.ui.AppActivity;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PerformExperimentActivity extends AppActivity implements ScannerUI {
    private final String LogTag = "PerformExperiment";
    private static final int RQC_ENABLE_BT = 367;
    private static final int RQC_TEST_BT_DEVICE = 378;
    private static final int RQC_ASK_CLEARANCE_FOR_BLUETOOTH = 389;
    private static final int MAX_SCAN_PERIOD = 20000;

    // Adapter for holding hrDevices found through deviceSearch.
    private static class BtDeviceListAdapter extends ArrayAdapter<BluetoothDeviceWrapper> {
        BtDeviceListAdapter(@NonNull Context cntxt, @NonNull List<BluetoothDeviceWrapper> entries)
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

    /** @return the context of this activity. */
    public Context getContext()
    {
        return this;
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
        final ImageButton btTestHRDevice = this.findViewById( R.id.btTestHRDevice );
        final Spinner cbExperiments = this.findViewById( R.id.cbExperiments );
        final ListView lvDevices = this.findViewById( R.id.lvDevices );
        final FloatingActionButton fbLaunchExpr = this.findViewById( R.id.fbPerformExperiment );

        fbLaunchExpr.setOnClickListener( (v) -> this.performExperiment() );

        cbExperiments.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                        PerformExperimentActivity.this.onExperimentChosen( pos );
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        PerformExperimentActivity.this.onExperimentChosen( 0 );
                    }
        });

        btClosePerformExperiment.setOnClickListener((view) ->
                PerformExperimentActivity.this.finish()
        );

        btStartScan.setOnClickListener( (view) ->
                PerformExperimentActivity.this.startScanning()
        );

        btStopScan.setOnClickListener( (view) ->
                PerformExperimentActivity.this.cancelAllConnections()
        );

        btTestHRDevice.setOnClickListener( (view) ->
                PerformExperimentActivity.this.launchDeviceTester()
        );

        lvDevices.setOnItemClickListener( (adptView, view, pos, l) -> {
                final PerformExperimentActivity cntxt = PerformExperimentActivity.this;
                final BluetoothDeviceWrapper btDevice = cntxt.devicesListAdapter.getItem( pos );

                if ( btDevice != null ) {
                    cntxt.setChosenDevice( btDevice );
                } else {
                    cntxt.showStatus( cntxt.getString( R.string.ErrUnknownDevice ) );
                }
        });

        // Initialize
        this.handler = new Handler();
        this.configBtLaunched = false;
        this.btDefinitelyNotAvailable = false;
        this.deviceSearch = false;
        this.experimentsList = new PartialObject[ 0 ];
        this.createList();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        this.obtainData();

        if ( !this.btDefinitelyNotAvailable ) {
            this.initBluetooth();

            // Ensures Bluetooth is enabled on the device.
            // If Bluetooth is not currently enabled,
            // fire an intent to display a dialog asking the user
            // to grant permission to enable it.
            if ( this.bluetoothAdapter == null ) {
                this.launchBtConfigPage();
            }
            else
            if ( !this.bluetoothAdapter.isEnabled() ) {
                this.launchBtConfigPage();
            } else {
                // Scans health devices
                this.startScanning();
            }
        }

        return;
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        this.cancelAllConnections( false );

        if ( this.actionDeviceDiscovery != null ) {
            try {
                this.unregisterReceiver( this.actionDeviceDiscovery );
            } catch(IllegalArgumentException exc) {
                Log.e( LogTag, "the receiver for device discovery was not registered." );
            }
        }

        this.clearDeviceListView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if ( menu != null ) {
            this.scanMenu = menu;

            if ( this.bluetoothAdapter != null ) {
                PerformExperimentActivity.this.runOnUiThread( () -> {
                    if ( menu.size() == 0 ) {
                        this.getMenuInflater().inflate( R.menu.menu_scan_devices, menu );
                    }

                    if ( this.isLookingForDevices() ) {
                        menu.findItem( R.id.menu_start_scan ).setVisible( false );
                        menu.findItem( R.id.menu_stop_scan ).setVisible( true );
                        menu.findItem( R.id.menu_refresh_scan ).setVisible( true );
                        menu.findItem( R.id.menu_refresh_scan ).setActionView(
                                R.layout.actionbar_indeterminate_progress );
                    } else {
                        menu.findItem( R.id.menu_start_scan ).setVisible( true );
                        menu.findItem( R.id.menu_stop_scan ).setVisible( false );
                        menu.findItem( R.id.menu_refresh_scan ).setVisible( false );
                    }
                });
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch ( item.getItemId() ) {
            case R.id.menu_start_scan:
                this.startScanning();
                break;
            case R.id.menu_stop_scan:
                this.cancelAllConnections();
                break;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult( requestCode, resultCode, data );

        switch ( requestCode ) {
            case RQC_ENABLE_BT:
                this.configBtLaunched = false;
                this.initBluetooth();

                if ( this.bluetoothAdapter == null ) {
                    this.btDefinitelyNotAvailable = true;
                    this.disableFurtherScan();
                }
                break;
            default:
                final String MSG = "unknown request code was not managed" + requestCode;

                Log.e( LogTag, MSG );
                throw new InternalError( MSG );

        }

        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );

        switch( requestCode ) {
            case RQC_ASK_CLEARANCE_FOR_BLUETOOTH:
                int totalGrants = 0;

                for(int result: grantResults) {
                    if ( result == PackageManager.PERMISSION_GRANTED ) {
                        ++totalGrants;
                    }
                }

                if ( totalGrants == grantResults.length ) {
                    this.doStartScanning();
                } else {
                    final AlertDialog.Builder DLG = new AlertDialog.Builder( this );

                    DLG.setMessage( R.string.ErrNoBluetoothPermissions );
                    DLG.setPositiveButton(R.string.lblBack, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });

                    DLG.create().show();
                    this.btDefinitelyNotAvailable = true;
                }
                break;
            default:
                final String MSG = "unknown permission request code was not managed" + requestCode;

                Log.e( LogTag, MSG );
                throw new InternalError( MSG );
        }

        return;
    }

    /** Creates the list of hrDevices. */
    private void createList()
    {
        final ListView lvDevices = this.findViewById( R.id.lvDevices );

        // Create lists, if needed
        if ( this.hrDevices == null ) {
            this.hrDevices = new ArrayList<>();
        }

        if ( this.discoveredDevices == null ) {
            this.discoveredDevices = new ArrayList<>();
        }

        if ( this.addrFound == null ) {
            this.addrFound = new HashSet<>();
        }

        if ( this.bluetoothFiltering == null ) {
            this.bluetoothFiltering = new BluetoothHRFiltering( this );
        }

        // Set chosen device
        if ( demoDevice == null ) {
            demoDevice = new BluetoothDeviceWrapper( DemoBluetoothDevice.get() );
        }

        if ( chosenBtDevice == null ) {
            chosenBtDevice = demoDevice;
        }

        this.setChosenDevice( chosenBtDevice );

        // Clear devices found list
        this.devicesListAdapter = new BtDeviceListAdapter( this, this.hrDevices);
        lvDevices.setAdapter( this.devicesListAdapter );
        this.clearDeviceListView();
    }

    /** Removes all hrDevices in the list. */
    private void clearDeviceListView()
    {
        this.addrFound.clear();
        this.discoveredDevices.clear();
        this.devicesListAdapter.clear();
        this.devicesListAdapter.add( demoDevice );

        if ( chosenBtDevice != null
          && !chosenBtDevice.isDemo() )
        {
            this.addDeviceToListView( chosenBtDevice.getDevice() );
        }

        return;
    }

    /** Takes care of all open Gatt connections. */
    private void closeAllGattConnections()
    {
        if ( this.bluetoothFiltering != null ) {
            this.bluetoothFiltering.closeAllGattConnections();
        }

        return;
    }

    /** Adds a given device to the list.
      * @param btDevice the bluetooth LE device.
      */
    @Override
    public void onDeviceFound(BluetoothDevice btDevice)
    {
        final String addr = btDevice.getAddress();

        if ( btDevice != null
          && btDevice.getName() != null
          && btDevice.getAddress() != null
          && !this.addrFound.contains( addr ) )
        {
            this.addrFound.add( addr );
            this.discoveredDevices.add( btDevice );

            PerformExperimentActivity.this.runOnUiThread( () -> {
                this.showStatus( btDevice.getName()
                                 + " " + this.getString( R.string.lblDeviceFound ).toLowerCase()
                                 + "..." );

            });
        }

        return;
    }

    /** Selects the device the user wants to employ.
      * @param newChosenDevice The device the user wants.
      * @see BluetoothDeviceWrapper
      */
    private void setChosenDevice(BluetoothDeviceWrapper newChosenDevice)
    {
        final TextView lblChosenDevice = this.findViewById( R.id.lblChosenDevice );

        assert newChosenDevice != null: "FATAL: newChosenDevice is null!!!";

        chosenBtDevice = newChosenDevice;
        lblChosenDevice.setText( newChosenDevice.getName() );
    }

    /** Initializes Bluetooth. */
    private void initBluetooth()
    {
        // Getting the Bluetooth adapter
        this.bluetoothAdapter = BluetoothUtils.getBluetoothAdapter( this );

        if ( this.bluetoothAdapter != null ) {
            // Register the BroadcastReceiver
            IntentFilter discoverer = new IntentFilter( BluetoothDevice.ACTION_FOUND );
            discoverer.addAction( BluetoothAdapter.ACTION_DISCOVERY_STARTED );
            discoverer.addAction( BluetoothAdapter.ACTION_DISCOVERY_FINISHED );

            this.actionDeviceDiscovery = BluetoothUtils.createActionDeviceDiscoveryReceiver( this );
            this.registerReceiver( this.actionDeviceDiscovery, discoverer );
        } else {
            this.disableFurtherScan();
        }

        return;
    }

    /** Shows an info status on screen. */
    private void showStatus(String msg)
    {
        this.showStatus( LogTag, msg );
    }

    /** Hides the stop deviceSearch button and option menu, shows the opposite options. */
    private void disableScanUI()
    {
        this.activateScanUI( false );
    }

    /** Hides the start deviceSearch button and option menu, shows the opposite options. */
    private void enableScanUI()
    {
        this.activateScanUI( true );
    }

    /** Hides ot shows the stop deviceSearch button/start deviceSearch button.
     * @param activate true to show start scan options and hide the stop scan options,
     *                  false to do the opposite. */
    private void activateScanUI(boolean activate)
    {
        final ImageButton btStartScan = this.findViewById( R.id.btStartScan );
        final ImageButton btStopScan = this.findViewById( R.id.btStopScan );


        PerformExperimentActivity.this.runOnUiThread( () -> {
            if ( this.bluetoothAdapter != null ) {
                if ( !activate ) {
                    btStartScan.setVisibility( View.VISIBLE );
                    btStopScan.setVisibility( View.GONE );
                } else {
                    btStartScan.setVisibility( View.GONE );
                    btStopScan.setVisibility( View.VISIBLE );
                }
            } else {
                btStartScan.setVisibility( View.GONE );
                btStopScan.setVisibility( View.GONE );
            }

            this.onCreateOptionsMenu( this.scanMenu );
        });

        return;
    }

    /** Launches the bluetooth configuration page */
    private void launchBtConfigPage()
    {
        if ( !this.configBtLaunched
          && !this.btDefinitelyNotAvailable )
        {
            this.configBtLaunched = true;

            PerformExperimentActivity.this.runOnUiThread( () -> {
                this.showStatus( this.getString( R.string.lblActivateBluetooth ) );

                final Intent enableBtIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
                this.startActivityForResult( enableBtIntent, RQC_ENABLE_BT);
            });
        }

        return;
    }

    /** Launches a tester activity to check the device. */
    private void launchDeviceTester()
    {
        PerformExperimentActivity.this.runOnUiThread( () -> {
            this.showStatus( "Test " + this.getString(  R.string.lblDevice ) + ": " + chosenBtDevice.getName() );

            final Intent enableBtIntent = new Intent( this, TestHRDevice.class );
            this.startActivityForResult( enableBtIntent, RQC_TEST_BT_DEVICE);
        });
    }

    /** @return whether the device is looking for (scanning and filtering), hrDevices or not. */
    public boolean isLookingForDevices()
    {
        return this.deviceSearch;
    }

    /** Asks for permissions before start scanning. */
    @Override
    public void startScanning()
    {
        if ( !this.btDefinitelyNotAvailable ) {
            final String[] ALL_PERMISSIONS = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

            final ArrayList<String> PERMISSIONS_TO_ASK_FOR = new ArrayList<>( ALL_PERMISSIONS.length );


            // Check all permissions
            for(String permissionId: ALL_PERMISSIONS) {
                int askAnswerBluetooth = ContextCompat.checkSelfPermission(
                        this.getApplicationContext(),
                        permissionId );

                if (askAnswerBluetooth != PackageManager.PERMISSION_GRANTED) {
                    PERMISSIONS_TO_ASK_FOR.add( permissionId );
                }
            }

            // Launch scanning or ask for permissions
            if ( PERMISSIONS_TO_ASK_FOR.size() > 0 ) {
                ActivityCompat.requestPermissions(
                            this,
                                   PERMISSIONS_TO_ASK_FOR.toArray( new String[ 0 ] ),
                        RQC_ASK_CLEARANCE_FOR_BLUETOOTH);
            } else {
                doStartScanning();
            }

        }

        return;
    }

    /** Launches deviceSearch for a given period of time */
    public void doStartScanning()
    {
        if ( !this.isLookingForDevices() ) {
            this.deviceSearch = true;
            this.closeAllGattConnections();
            this.clearDeviceListView();

            this.handler.postDelayed( () -> {
                if ( this.bluetoothAdapter.isDiscovering() ) {
                    Log.d( LogTag, "Discovery forced finish." );
                    PerformExperimentActivity.this.stopScanning();
                }
            }, MAX_SCAN_PERIOD );

            this.bluetoothAdapter.startDiscovery();

            PerformExperimentActivity.this.runOnUiThread( () -> {
                this.enableScanUI();
                this.showStatus( this.getString( R.string.lblStartScan ) );
            });
        }

        return;
    }

    /** Cancels the discovering. */
    private void cancelDiscovery()
    {
        if ( this.bluetoothAdapter != null
          && this.bluetoothAdapter.isDiscovering() )
        {
            this.bluetoothAdapter.cancelDiscovery();
        }

        return;
    }

    /** Stops deviceSearch, starting the filtering for HR devices. */
    @Override
    public void stopScanning()
    {
        if ( this.isLookingForDevices() ) {
            this.cancelDiscovery();

            PerformExperimentActivity.this.runOnUiThread( () ->
                    this.showStatus( this.getString( R.string.lblFilteringByService ) + "..." ) );

            if ( this.discoveredDevices.size() >  0 ) {
                this.bluetoothFiltering.filter( this.discoveredDevices.toArray( new BluetoothDevice[ 0 ] ) );
            } else {
                this.filteringFinished();
            }
        }

        return;
    }

    /** Stops deviceSearch, removing all pending callbacks (that would stop deviceSearch) */
    private void cancelAllConnections()
    {
        cancelAllConnections( true );
    }

    private void cancelAllConnections(boolean warn)
    {
        this.handler.removeCallbacksAndMessages( null );
        this.cancelDiscovery();
        this.filteringFinished( warn );
    }

    public void addDeviceToListView(BluetoothDevice btDevice)
    {
        final String addr = btDevice.getAddress();
        final BluetoothDeviceWrapper btwDevice = new BluetoothDeviceWrapper( btDevice );

        PerformExperimentActivity.this.runOnUiThread( () -> {
            if ( !this.hrDevices.contains( btwDevice ) ) {
                this.devicesListAdapter.add( btwDevice );
                this.showStatus( this.getString( R.string.lblDeviceFound ) + ": " + btDevice.getName() );
            }
        });
    }

    public void denyAdditionToList(BluetoothDevice btDevice)
    {
        PerformExperimentActivity.this.runOnUiThread( () -> {
            this.showStatus( this.getString( R.string.ErrNoHR ) + ": " + btDevice.getName() );
        });
    }

    public void filteringFinished()
    {
        this.filteringFinished( true );
    }

    public void filteringFinished(final boolean warn)
    {
        this.deviceSearch = false;
        this.closeAllGattConnections();

        PerformExperimentActivity.this.runOnUiThread( () -> {
            if ( warn ) {
                this.showStatus( this.getString(R.string.lblStopScan) );
            }

            this.disableScanUI();
        });
    }

    /** Enables the launch button or not. */
    private void disableFurtherScan()
    {
        final ImageButton btStartScan = this.findViewById( R.id.btStartScan );

        if ( btStartScan.getVisibility() != View.INVISIBLE ) {
            this.disableScanUI();

            PerformExperimentActivity.this.runOnUiThread( () -> {
                btStartScan.setEnabled( false );
                btStartScan.setVisibility( View.INVISIBLE );

                this.showStatus( this.getString( R.string.ErrNoBluetooth) );
            });
        }

        return;
    }

    /** Reads the data from the ORM. */
    private void obtainData()
    {
        final FloatingActionButton fbLaunchExpr = this.findViewById( R.id.fbPerformExperiment );

        // Read experiment's and user's names
        try {
            final Orm dataStore = Orm.get();

            this.experimentsList = dataStore.enumerateExperiments();

            // Read user's names and experiment's names
            final String[] userNames = dataStore.enumerateUserNames();
            final String[] experimentNames = dataStore.enumerateObjNames( this.experimentsList );

            // Spinner users
            final AutoCompleteTextView cbUsers = this.findViewById( R.id.cbUsers);
            final ArrayAdapter<String> adapterUsers = new ArrayAdapter<>( this,
                    android.R.layout.simple_spinner_item,
                    userNames );
            adapterUsers.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
            cbUsers.setAdapter( adapterUsers );

            if ( userNames.length > 0 ) {
                cbUsers.setText( userNames[ 0 ] );
                cbUsers.setSelection( 0, userNames[ 0 ].length() );
            }

            // Spinner experiments
            final Spinner cbExperiments = this.findViewById( R.id.cbExperiments );
            final ArrayAdapter<String> adapterExperiments = new ArrayAdapter<>( this,
                    android.R.layout.simple_spinner_item,
                    experimentNames );
            adapterExperiments.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
            cbExperiments.setAdapter( adapterExperiments );

            // Select chosen experiment
            if ( chosenExperiment != null ) {
                int i = 0;
                for(String exprName: experimentNames) {
                    if ( chosenExperiment.getName().equals( exprName ) ) {
                        break;
                    }

                    ++i;
                }

                if ( i < experimentNames.length ) {
                    cbExperiments.setSelection( i );
                }
            }
        } catch(IOException exc) {
            this.disableFurtherScan();
            this.showStatus( this.getString( R.string.ErrIO ) );
        } finally {
            // Determine whether to allow to launch experiments or not
            fbLaunchExpr.setEnabled( this.experimentsList.length > 0 );
        }
    }

    /** The experiment should always previously exist. */
    private void onExperimentChosen(int pos)
    {
        if ( pos >= 0
          && pos < this.experimentsList.length )
        {
            final Orm dataStore = Orm.get();
            final PartialObject partialExperiment = this.experimentsList[ pos ];
            Experiment experiment = null;

            try {
                experiment = (Experiment) dataStore.retrieve(
                                                partialExperiment.getId(),
                                                Persistent.TypeId.Experiment );
            } catch(IOException exc)
            {
                Log.d(LogTag, exc.getMessage()
                                + "\n\tretrieving experiment: " + partialExperiment.toString() );
            }

            // Assign the chosen experiment
            if ( experiment != null ) {
                chosenExperiment = experiment;
                Log.d( LogTag, "Experiment chosen: " + chosenExperiment );
            }
        }

        return;
    }

    /** The user can previously exist or not. */
    private void onUserChosen(String userName)
    {
        User usr = null;

        if ( userName != null
          && !userName.trim().isEmpty() )
        {
            userName = userName.trim();

            try {
                usr = Orm.get().createOrRetrieveUserByName( userName );
            } catch(IOException exc)
            {
                Log.d(LogTag, exc.getMessage()
                        + "\n\tlooking for or creating user: " + userName.toString() );
            }
        }

        // Assign the chosen experiment
        if ( usr != null ) {
            chosenUser = usr;
            Log.d( LogTag, "Chosen user: " + usr );
        }

        return;
    }

    // Launches the experiment
    private void performExperiment()
    {
        final Spinner cbExperiment = this.findViewById( R.id.cbExperiments );
        final TextView edUsers = this.findViewById( R.id.cbUsers );
        final String userName = edUsers.getText().toString();

        this.onUserChosen( userName );

        if ( chosenUser == null) {
           this.showStatus( LogTag, "No chosen user." );
           return;
        }

        this.onExperimentChosen( cbExperiment.getSelectedItemPosition() );

        if ( chosenExperiment == null ) {
           this.showStatus( LogTag, "No chosen experiment." );
           return;
        }

        final Intent launchExperimentCfg = new Intent( this, ExperimentDirector.class );
        this.startActivity( launchExperimentCfg );
    }

    @Override
    public boolean askBeforeLeaving() {
        return false;
    }

    private Set<String> addrFound;
    private BroadcastReceiver actionDeviceDiscovery;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDeviceWrapper> hrDevices;
    private ArrayList<BluetoothDevice> discoveredDevices;
    private BtDeviceListAdapter devicesListAdapter;
    private BluetoothHRFiltering bluetoothFiltering;
    private Menu scanMenu;
    private Handler handler;
    private boolean configBtLaunched;
    private boolean deviceSearch;
    private boolean btDefinitelyNotAvailable;

    private PartialObject[] experimentsList;

    public static BluetoothDeviceWrapper chosenBtDevice;
    public static BluetoothDeviceWrapper demoDevice;
    public static Experiment chosenExperiment;
    public static User chosenUser;
}

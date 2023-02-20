// VARSE 2019/23 (c) Baltasar for MILEGroup MIT License <baltasarq@uvigo.es>


package com.devbaltasarq.varse.ui;


import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.View;
import com.google.android.material.navigation.NavigationView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.AppInfo;
import com.devbaltasarq.varse.core.PlainStringEncoder;
import com.devbaltasarq.varse.core.Ofm;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.core.Settings;
import com.devbaltasarq.varse.core.ofmcache.EntitiesCache;
import com.devbaltasarq.varse.ui.performexperiment.PerformExperimentActivity;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends AppActivity
        implements NavigationView.OnNavigationItemSelectedListener
{
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_main );

        final Toolbar TOOLBAR = this.findViewById( R.id.toolbar );
        final DrawerLayout DRAWER = this.findViewById( R.id.drawer_layout );

        final TextView LBL_APP_NAME = this.findViewById( R.id.lblAppName );
        final TextView LBL_APP_VERSION = this.findViewById( R.id.lblAppVersion );
        final TextView LBL_OPT_PERFORM_EXPERIMENT = this.findViewById( R.id.lblOptPerformExperiment );
        final ImageView IMG_OPT_PERFORM_EXPERIMENT = this.findViewById( R.id.imgOptPerformExperiment );
        final ImageView IV_ICON_APP = this.findViewById( R.id.ivIconApp );
        final TextView LBL_OPT_EXPERIMENTS = this.findViewById( R.id.lblOptExperiments );
        final TextView LBL_OPT_RESULTS = this.findViewById( R.id.lblOptResults );
        final ImageView IMG_OPT_EXPERIMENTS = this.findViewById( R.id.imgOptExperiments );
        final ImageView IMG_OPT_RESULTS = this.findViewById( R.id.imgOptResults );
        final NavigationView NAVIGATION_VIEW = this.findViewById( R.id.nav_view );
        final View.OnClickListener GENERAL_CLICK_LISTENER = (View v) ->
                MainActivity.this.goTo( v.getId() );

        this.setTitle( "" );
        this.setSupportActionBar( TOOLBAR );

        final ActionBarDrawerToggle TOGGLE = new ActionBarDrawerToggle(
                this, DRAWER, TOOLBAR, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        DRAWER.addDrawerListener( TOGGLE );
        TOGGLE.syncState();
        NAVIGATION_VIEW.setNavigationItemSelectedListener( this );
        LBL_APP_VERSION.setText( AppInfo.asString() );
        LBL_APP_NAME.setText( AppInfo.asShortString() );

        LBL_OPT_EXPERIMENTS.setOnClickListener( GENERAL_CLICK_LISTENER );
        LBL_OPT_RESULTS.setOnClickListener( GENERAL_CLICK_LISTENER );
        IMG_OPT_EXPERIMENTS.setOnClickListener( GENERAL_CLICK_LISTENER );
        IMG_OPT_RESULTS.setOnClickListener( GENERAL_CLICK_LISTENER );
        LBL_OPT_PERFORM_EXPERIMENT.setOnClickListener( GENERAL_CLICK_LISTENER );
        IMG_OPT_PERFORM_EXPERIMENT.setOnClickListener( GENERAL_CLICK_LISTENER );
        IV_ICON_APP.setOnClickListener( (v) -> this.toggleAppVersionShown() );
        LBL_APP_NAME.setOnClickListener( (v) -> this.toggleAppVersionShown() );
        LBL_APP_VERSION.setOnClickListener( (v) -> this.toggleAppVersionShown() );

        this.block = false;
    }

    @Override
    public void onStart()
    {
        super.onStart();

        // Initialize the database
        Ofm.init( this.getApplicationContext(), PlainStringEncoder.get() );

        // Initialize settings
        try {
            Settings.open();
        } catch(JSONException exc)
        {
            Log.i( LOG_TAG,
                    this.getString( R.string.errInitializingSettings)
                        + exc.getMessage() );
            Settings.create();
            Log.i( LOG_TAG, this.getString( R.string.msgResettingSettings ) );
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Initialize the database
        Ofm.get().removeCache();
    }

    private void toggleAppVersionShown()
    {
        final TableRow TR_APP_VERSION_ROW = this.findViewById( R.id.trAppVersionRow );

        if ( TR_APP_VERSION_ROW.getVisibility() == View.GONE ) {
            TR_APP_VERSION_ROW.setVisibility( View.VISIBLE );
        } else {
            TR_APP_VERSION_ROW.setVisibility( View.GONE );
        }

        return;
    }

    private boolean goTo(int id)
    {
        boolean toret = false;

        if ( !this.block ) {
            if ( id == R.id.imgOptPerformExperiment
              || id == R.id.lblOptPerformExperiment
              || id == R.id.nav_perform_experiment )
            {
                this.startActivity( new Intent( this, PerformExperimentActivity.class ) );
                toret = true;
            }
            else
            if ( id == R.id.imgOptExperiments
              || id ==  R.id.lblOptExperiments
              || id ==  R.id.nav_experiments
              || id ==  R.id.nav_export
              || id ==  R.id.action_export )
            {
                this.startActivity( new Intent( this, ExperimentsActivity.class ) );
                toret = true;
            }
            else
            if ( id == R.id.imgOptResults
              || id == R.id.lblOptResults
              || id == R.id.nav_results )
            {
                this.startActivity( new Intent( this, ResultsActivity.class ) );
                toret = true;
            }
            else
            if ( id == R.id.nav_settings ) {
                this.startActivity( new Intent( this, SettingsActivity.class ) );
                toret = true;
            }
            else
            if ( id == R.id.nav_import
              || id == R.id.action_import )
            {
                this.pickFile();
                toret = true;
            }
            else
            if ( id == R.id.nav_privacy_policy ) {
                Uri uri = Uri.parse( "https://milegroup.github.io/varse/privacy.html" );
                Intent intent = new Intent( Intent.ACTION_VIEW, uri );
                this.startActivity( intent );
                toret = true;
            }
        } else {
            Log.e( LOG_TAG, "Tried to operate in blocked app" );
            this.showStatus(LOG_TAG, this.getString( R.string.errIO) );
        }

        return toret;
    }

    @Override
    public void onBackPressed()
    {
        final DrawerLayout DRAWER = this.findViewById( R.id.drawer_layout );

        if ( DRAWER.isDrawerOpen( GravityCompat.START ) ) {
            DRAWER.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        this.getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        boolean toret = this.goTo( item.getItemId() );

        if ( !toret ) {
            toret = super.onOptionsItemSelected( item );
        }

        return toret;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
        final DrawerLayout DRAWER = this.findViewById( R.id.drawer_layout );

        this.goTo( item.getItemId() );
        DRAWER.closeDrawer( GravityCompat.START );

        return true;
    }

    @Override
    public boolean askBeforeLeaving()
    {
        return false;
    }

    /** Launch file browser. */
    private void pickFile()
    {
        this.SELECT_MEDIA.launch( "*/*" );
    }

    /** Import the experiment file. */
    private void importFile(Uri uri)
    {
        try {
            final Ofm db = Ofm.get();

            if ( uri != null
              && uri.getScheme() != null
              && ( uri.getScheme().equals( ContentResolver.SCHEME_CONTENT )
               ||  uri.getScheme().equals( ContentResolver.SCHEME_FILE ) ) )
            {
                final String URI_PATH_SEGMENT = uri.getLastPathSegment();
                final InputStream fileIn = this.getContentResolver().openInputStream( uri );
                final String FILE_EXT = Ofm.extractFileExt( URI_PATH_SEGMENT );

                if ( FILE_EXT.equalsIgnoreCase( EntitiesCache.getFileExtFor( Persistent.TypeId.Result ) ) )
                {
                    final String LBL_RESULT = this.getString( R.string.lblResult );

                    db.importResult( fileIn );
                    this.showStatus(LOG_TAG, this.getString( R.string.msgImported )
                            + ": " + LBL_RESULT );

                } else {
                    final String LBL_EXPERIMENT = this.getString( R.string.lblExperiment );

                    db.importExperiment( fileIn );
                    this.showStatus(LOG_TAG, this.getString( R.string.msgImported )
                            + ": " + LBL_EXPERIMENT );
                }
            } else {
                this.showStatus(LOG_TAG, this.getString( R.string.errUnsupportedFileType) );
            }
        } catch(IOException exc)
        {
            this.showStatus(LOG_TAG, this.getString( R.string.errIO) );
            Log.e(LOG_TAG, "Importing: '" + uri + "': " + exc.getMessage() );
        }
    }

    private boolean block;
    private final ActivityResultLauncher<String> SELECT_MEDIA = this.registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if ( uri != null ) {
                    final String FILE_EXTENSION = MimeTypeMap.getFileExtensionFromUrl(
                            uri.toString().toLowerCase() );
                    final String RES_FILE_EXT = EntitiesCache.getFileExtFor( Persistent.TypeId.Result ).toLowerCase();

                    if ( FILE_EXTENSION.equals( "zip" )
                      || FILE_EXTENSION.equals( RES_FILE_EXT ) )
                    {
                        this.importFile( uri );
                    } else {
                        this.showStatus( LOG_TAG, this.getString( R.string.errUnsupportedFileType) );
                    }
                }
            });
}

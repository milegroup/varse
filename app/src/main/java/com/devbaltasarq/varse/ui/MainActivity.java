package com.devbaltasarq.varse.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.AppInfo;
import com.devbaltasarq.varse.core.FileNameAdapter;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.core.Settings;
import com.devbaltasarq.varse.ui.performexperiment.PerformExperimentActivity;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppActivity
        implements NavigationView.OnNavigationItemSelectedListener
{
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int RQC_PICK_FILE = 0x813;

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
        Orm.init( this.getApplicationContext(), FileNameAdapter.get() );

        // Initialize settings
        try {
            Settings.open();
        } catch(JSONException exc)
        {
            this.showStatus(LOG_TAG,
                    this.getString( R.string.errInitializingSettings)
                        + exc.getMessage() );
            Settings.create();
            this.showStatus(LOG_TAG, this.getString( R.string.msgResettingSettings ) );
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Initialize the database
        Orm.get().removeCache();
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
            switch( id ) {
                case R.id.imgOptPerformExperiment:
                case R.id.lblOptPerformExperiment:
                case R.id.nav_perform_experiment:
                    this.startActivity( new Intent( this, PerformExperimentActivity.class ) );
                    toret = true;
                    break;
                case R.id.imgOptExperiments:
                case R.id.lblOptExperiments:
                case R.id.nav_experiments:
                case R.id.nav_export:
                case R.id.action_export:
                    this.startActivity( new Intent( this, ExperimentsActivity.class ) );
                    toret = true;
                    break;
                case R.id.imgOptResults:
                case R.id.lblOptResults:
                case R.id.nav_results:
                    this.startActivity( new Intent( this, ResultsActivity.class ) );
                    toret = true;
                    break;
                case R.id.nav_settings:
                    this.startActivity( new Intent( this, SettingsActivity.class ) );
                    toret = true;
                    break;
                case R.id.nav_import:
                case R.id.action_import:
                    this.pickFile();
                    toret = true;
                    break;
            }
        } else {
            Log.e(LOG_TAG, "Tried to operate in blocked app" );
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult( requestCode, resultCode, data );

        if ( requestCode == RQC_PICK_FILE
          && resultCode == RESULT_OK )
        {
            final Uri URI = data.getData();

            if ( URI != null ) {
                final String FILE_EXTENSION = MimeTypeMap.getFileExtensionFromUrl( URI
                        .toString().toLowerCase() );
                final String RES_FILE_EXT = Orm.getFileExtFor( Persistent.TypeId.Result ).toLowerCase();

                if ( FILE_EXTENSION.equals( "zip" )
                  || FILE_EXTENSION.equals( RES_FILE_EXT ) )
                {
                    this.importFile( URI );
                } else {
                    this.showStatus(LOG_TAG, this.getString( R.string.errUnsupportedFileType) );
                }
            } else {
                this.showStatus(LOG_TAG, this.getString( R.string.msgFileNotFound ) );
            }
        }

        return;
    }

    /** Launch file browser. */
    private void pickFile()
    {
        final Intent INTENT = new Intent();

        // Launch
        INTENT.setType( "*/*" );
        INTENT.setAction( Intent.ACTION_GET_CONTENT );

        this.startActivityForResult(
                Intent.createChooser( INTENT, this.getString( R.string.lblMediaSelection ) ),
                RQC_PICK_FILE );
    }

    /** Import the experiment file. */
    private void importFile(Uri uri)
    {
        try {
            final Orm db = Orm.get();

            if ( uri != null
              && uri.getScheme() != null
              && ( uri.getScheme().equals( ContentResolver.SCHEME_CONTENT )
               ||  uri.getScheme().equals( ContentResolver.SCHEME_FILE ) ) )
            {
                final String URI_PATH_SEGMENT = uri.getLastPathSegment();
                final InputStream fileIn = this.getContentResolver().openInputStream( uri );
                final String FILE_EXT = Orm.extractFileExt( URI_PATH_SEGMENT );

                if ( FILE_EXT.equalsIgnoreCase( Orm.getFileExtFor( Persistent.TypeId.Result ) ) )
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
}

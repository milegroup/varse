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
    private static final String LogTag = MainActivity.class.getSimpleName();
    private static final int RQC_PICK_FILE = 0x813;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_main );

        final Toolbar toolbar = this.findViewById( R.id.toolbar );
        final DrawerLayout drawer = this.findViewById( R.id.drawer_layout );

        final TextView lblAppName = this.findViewById( R.id.lblAppName );
        final TextView lblAppVersion = this.findViewById( R.id.lblAppVersion );
        final TextView lblOptPerformExperiment = this.findViewById( R.id.lblOptPerformExperiment );
        final ImageView imgOptPerformExperiment = this.findViewById( R.id.imgOptPerformExperiment );
        final ImageView ivIconApp = this.findViewById( R.id.ivIconApp );
        final TextView lblOptExperiments = this.findViewById( R.id.lblOptExperiments );
        final TextView lblOptResults = this.findViewById( R.id.lblOptResults );
        final ImageView imgOptExperiments = this.findViewById( R.id.imgOptExperiments );
        final ImageView imgOptResults = this.findViewById( R.id.imgOptResults );
        final NavigationView navigationView = this.findViewById( R.id.nav_view );
        final View.OnClickListener generalClickListener = (View v) ->
                MainActivity.this.goTo( v.getId() );

        this.setTitle( "" );
        this.setSupportActionBar( toolbar );

        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawer.addDrawerListener( toggle );
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener( this );
        lblAppVersion.setText( AppInfo.asString() );
        lblAppName.setText( AppInfo.asShortString() );

        lblOptExperiments.setOnClickListener( generalClickListener );
        lblOptResults.setOnClickListener( generalClickListener );
        imgOptExperiments.setOnClickListener( generalClickListener );
        imgOptResults.setOnClickListener( generalClickListener );
        lblOptPerformExperiment.setOnClickListener( generalClickListener );
        imgOptPerformExperiment.setOnClickListener( generalClickListener );
        ivIconApp.setOnClickListener( (v) -> this.toggleAppVersionShown() );
        lblAppName.setOnClickListener( (v) -> this.toggleAppVersionShown() );
        lblAppVersion.setOnClickListener( (v) -> this.toggleAppVersionShown() );

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
            this.showStatus( LogTag,
                    this.getString( R.string.errInitializingSettings)
                        + exc.getMessage() );
            Settings.create();
            this.showStatus( LogTag, this.getString( R.string.msgResettingSettings ) );
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Initialize the database
        Orm.get().removeCache( this.getApplicationContext() );
    }

    private void toggleAppVersionShown()
    {
        final TableRow trAppVersionRow = this.findViewById( R.id.trAppVersionRow );

        if ( trAppVersionRow.getVisibility() == View.GONE ) {
            trAppVersionRow.setVisibility( View.VISIBLE );
        } else {
            trAppVersionRow.setVisibility( View.GONE );
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
            Log.e( LogTag, "Tried to operate in blocked app" );
            this.showStatus( LogTag, this.getString( R.string.errIO) );
        }

        return toret;
    }

    @Override
    public void onBackPressed()
    {
        final DrawerLayout drawer = this.findViewById( R.id.drawer_layout );

        if ( drawer.isDrawerOpen( GravityCompat.START ) ) {
            drawer.closeDrawer(GravityCompat.START);
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
        final DrawerLayout drawer = this.findViewById( R.id.drawer_layout );

        this.goTo( item.getItemId() );
        drawer.closeDrawer( GravityCompat.START );

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
            final Uri uri = data.getData();

            if ( uri != null ) {
                final String FILE_EXTENSION = MimeTypeMap.getFileExtensionFromUrl( uri
                        .toString().toLowerCase() );
                final String RES_FILE_EXT = Orm.getFileExtFor( Persistent.TypeId.Result ).toLowerCase();

                if ( FILE_EXTENSION.equals( "zip" )
                  || FILE_EXTENSION.equals( RES_FILE_EXT ) )
                {
                    this.importFile( uri );
                } else {
                    this.showStatus( LogTag, this.getString( R.string.errUnsupportedFileType) );
                }
            } else {
                this.showStatus( LogTag, this.getString( R.string.msgFileNotFound ) );
            }
        }

        return;
    }

    /** Launch file browser. */
    private void pickFile()
    {
        final Intent intent = new Intent();

        // Launch
        intent.setType( "*/*" );
        intent.setAction( Intent.ACTION_GET_CONTENT );

        this.startActivityForResult(
                Intent.createChooser( intent, this.getString( R.string.lblMediaSelection ) ),
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
                    this.showStatus( LogTag, this.getString( R.string.msgImported )
                            + ": " + LBL_RESULT );

                } else {
                    final String LBL_EXPERIMENT = this.getString( R.string.lblExperiment );

                    db.importExperiment( fileIn );
                    this.showStatus( LogTag, this.getString( R.string.msgImported )
                            + ": " + LBL_EXPERIMENT );
                }
            } else {
                this.showStatus( LogTag, this.getString( R.string.errUnsupportedFileType) );
            }
        } catch(IOException exc)
        {
            this.showStatus( LogTag, this.getString( R.string.errIO) );
            Log.e( LogTag, "Importing: '" + uri + "': " + exc.getMessage() );
        }
    }

    private boolean block;
}

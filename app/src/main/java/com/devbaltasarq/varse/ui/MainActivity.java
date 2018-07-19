package com.devbaltasarq.varse.ui;

import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.AppInfo;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.ui.performexperiment.PerformExperimentActivity;

import java.io.IOException;

public class MainActivity extends AppActivity
        implements NavigationView.OnNavigationItemSelectedListener
{
    private static final String LogTag = MainActivity.class.getSimpleName();

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

        try {
            Orm.init( this.getApplicationContext() );
        } catch(IOException exc)
        {
            block = true;
            Log.e( LogTag, "app blocked, storage not ready; " + exc.getMessage() );
            this.showStatus( LogTag, this.getString( R.string.ErrStore ) );
        }

        return;
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
                case R.id.imgOptResults:
                case R.id.lblOptResults:
                case R.id.nav_results:
                case R.id.nav_export:
                case R.id.action_export:
                    this.startActivity( new Intent( this, ExperimentsActivity.class ) );
                    toret = true;
                    break;
                case R.id.nav_import:
                case R.id.action_import:
                    this.showStatus( LogTag, "Not implemented" );
                    toret = true;
                    break;
            }
        } else {
            Log.e( LogTag, "Tried to operate in blocked app" );
            this.showStatus( LogTag, this.getString( R.string.ErrIO ) );
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

    private boolean block;
}

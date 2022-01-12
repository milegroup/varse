package com.devbaltasarq.varse.ui.editexperiment.editgroup;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.ui.AppActivity;
import com.devbaltasarq.varse.ui.adapters.ListViewActivityArrayAdapter;


/** Parent class for group editor activities (i.e., EditMediaGroupActivity...) .*/
public abstract class EditGroupActivity extends AppActivity {
    private static final String LOG_TAG = EditGroupActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
    }

    public void showActivities()
    {
        final ListView LV_ACTS = this.findViewById( R.id.lvActs );
        final TextView LBL_NO_ENTRIES = this.findViewById( R.id.lblNoEntries );
        final int NUM_ENTRIES = group.size();

        Log.i(LOG_TAG, "starting showActivities()..." );
        Log.i(LOG_TAG, "entries: " + NUM_ENTRIES );

        if ( NUM_ENTRIES > 0 ) {
            final Group.Activity[] ACTS = group.get();
            final ListViewActivityArrayAdapter ACTS_ADAPTER;

            // Create adapter
            ACTS_ADAPTER = new ListViewActivityArrayAdapter( this, ACTS );

            LBL_NO_ENTRIES.setVisibility( View.GONE );
            LV_ACTS.setVisibility( View.VISIBLE );
            LV_ACTS.setAdapter( ACTS_ADAPTER );
        } else {
            LBL_NO_ENTRIES.setVisibility( View.VISIBLE );
            LV_ACTS.setVisibility( View.GONE );
            Log.i(LOG_TAG, "    no entries" );
        }

        Log.i(LOG_TAG, "finished showActivities()" );
    }

    public abstract void addActivity();

    public abstract void editActivity(Group.Activity act);

    public void deleteActivity(Group.Activity act)
    {
        group.remove( act );
        this.showActivities();
    }

    public void sortActivityUp(Group.Activity act)
    {
        group.sortActivityUp( act );
        this.showActivities();
    }

    public void sortActivityDown(Group.Activity act)
    {
        group.sortActivityDown( act );
        this.showActivities();
    }

    public static Group<? extends Group.Activity> group;
}

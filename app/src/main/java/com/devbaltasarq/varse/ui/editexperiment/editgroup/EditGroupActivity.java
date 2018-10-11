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
    private static final String LogTag = EditGroupActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
    }

    public void showActivities()
    {
        final ListView lvActs = this.findViewById( R.id.lvActs );
        final TextView lblNoEntries = this.findViewById( R.id.lblNoEntries );
        final int NUM_ENTRIES = group.size();

        Log.i( LogTag, "starting showActivities()..." );
        Log.i( LogTag, "entries: " + NUM_ENTRIES );

        if ( NUM_ENTRIES > 0 ) {
            final Group.Activity[] acts = group.get();
            final ListViewActivityArrayAdapter actsAdapter;

            // Create adapter
            actsAdapter = new ListViewActivityArrayAdapter( this, acts );

            lblNoEntries.setVisibility( View.GONE );
            lvActs.setVisibility( View.VISIBLE );
            lvActs.setAdapter( actsAdapter );
        } else {
            lblNoEntries.setVisibility( View.VISIBLE );
            lvActs.setVisibility( View.GONE );
            Log.i( LogTag, "    no entries" );
        }

        Log.i( LogTag, "finished showActivities()" );
    }

    public abstract void addActivity();

    public abstract void editActivity(Group.Activity act);

    @SuppressWarnings("unchecked")
    public void deleteActivity(Group.Activity act)
    {
        group.remove( act );
        this.showActivities();
    }

    @SuppressWarnings("unchecked")
    public void sortActivityUp(Group.Activity act)
    {
        group.sortActivityUp( act );
        this.showActivities();
    }

    @SuppressWarnings("unchecked")
    public void sortActivityDown(Group.Activity act)
    {
        group.sortActivityDown( act );
        this.showActivities();
    }

    public static Group group;
}

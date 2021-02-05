package com.devbaltasarq.varse.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.User;
import com.devbaltasarq.varse.ui.UsersActivity;
import com.devbaltasarq.varse.ui.performexperiment.PerformExperimentActivity;

/** Represents an adapter of the special items for the ListView of media files. */
public class ListViewUserArrayAdapter extends ArrayAdapter<User> {
    public ListViewUserArrayAdapter(Context cntxt, User[] entries)
    {
        super( cntxt, 0, entries );
    }

    @Override
    public @NonNull View getView(int position, View rowView, @NonNull ViewGroup parent)
    {
        final LayoutInflater LAYOUT_INFLATER = LayoutInflater.from( this.getContext() );
        final User USR = this.getItem( position );

        if ( USR == null ) {
            throw new Error( "User adapter: user is null" );
        }

        if ( rowView == null ) {
            rowView = LAYOUT_INFLATER.inflate( R.layout.listview_user_entry, null );
        }

        final ImageButton BT_LAUNCH = rowView.findViewById( R.id.btLaunch );
        final TextView LBL_USER_DESC = rowView.findViewById( R.id.lblUserDesc );

        LBL_USER_DESC.setText( USR.getName() );

        BT_LAUNCH.setOnClickListener( (v) -> {
                ListViewUserArrayAdapter self = ListViewUserArrayAdapter.this;
                UsersActivity cntxt = (UsersActivity) self.getContext();

                UsersActivity.selectedUser = USR;
                cntxt.startActivity( new Intent( cntxt, PerformExperimentActivity.class ) );
        });

        return rowView;
    }
}

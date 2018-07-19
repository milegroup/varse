package com.devbaltasarq.varse.ui.edituser;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.ui.UsersActivity;
import com.devbaltasarq.varse.ui.performexperiment.BluetoothLeScannerPerformExperimentActivity;

/** Represents an adapter of the special items for the ListView of media files. */
public class ListViewUserEntryArrayAdapter extends ArrayAdapter<ListViewUserEntry> {
    public ListViewUserEntryArrayAdapter(Context cntxt, ListViewUserEntry[] entries)
    {
        super( cntxt, 0, entries );
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final LayoutInflater layoutInflater = LayoutInflater.from( this.getContext() );
        final ListViewUserEntry entry = this.getItem( position );

        if ( convertView == null ) {
            convertView = layoutInflater.inflate( R.layout.listview_user_entry, null );
        }

        final ImageButton btLaunch = convertView.findViewById( R.id.btLaunch );
        final TextView lblUserDesc = convertView.findViewById( R.id.lblUserDesc );

        lblUserDesc.setText( entry.getUserDesc() );

        btLaunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ListViewUserEntryArrayAdapter self = ListViewUserEntryArrayAdapter.this;
                UsersActivity cntxt = (UsersActivity) self.getContext();

                UsersActivity.selectedUser = entry.getUser();
                cntxt.startActivity( new Intent( cntxt, BluetoothLeScannerPerformExperimentActivity.class ) );
            }
        });

        return convertView;
    }
}

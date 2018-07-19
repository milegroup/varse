package com.devbaltasarq.varse.ui.editexperiment;

import android.content.Context;
import android.support.v7.content.res.AppCompatResources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.experiment.ManualGroup;
import com.devbaltasarq.varse.core.experiment.VideoGroup;

/** Represents an adapter of the special items for the ListView of media files. */
public class ListViewGroupEntryArrayAdapter extends ArrayAdapter<ListViewGroupEntry> {
    public ListViewGroupEntryArrayAdapter(Context cntxt, ListViewGroupEntry[] entries)
    {
        super( cntxt, 0, entries );
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final EditExperimentActivity cntxt = (EditExperimentActivity) this.getContext();
        final LayoutInflater layoutInflater = LayoutInflater.from( this.getContext() );
        final ListViewGroupEntry entry = this.getItem( position );

        if ( convertView == null ) {
            convertView = layoutInflater.inflate( R.layout.listview_media_group_entry, null );
        }

        final ImageButton btSortUp = convertView.findViewById( R.id.btSortMediaGroupUp );
        final ImageButton btSortDown = convertView.findViewById( R.id.btSortMediaGroupDown );
        final ImageButton btEditMediaGroup = convertView.findViewById( R.id.btEditMediaGroup );
        final ImageButton btDeleteMediaGroup = convertView.findViewById( R.id.btDeleteMediaGroup );
        final TextView lblMediaGroupDesc = convertView.findViewById( R.id.lblMediaGroupDesc );
        final ImageView ivMediaGroupDesc = convertView.findViewById( R.id.ivMediaGroupDesc);

        // Set image and file name
        int groupDescImgId = R.drawable.ic_picture_group_button;

        if ( entry.getGroup() instanceof VideoGroup ) {
            groupDescImgId = R.drawable.ic_video_group_button;
        }
        else
        if ( entry.getGroup() instanceof ManualGroup) {
            groupDescImgId = R.drawable.ic_manual_button;
        }

        ivMediaGroupDesc.setImageDrawable( AppCompatResources.getDrawable( cntxt, groupDescImgId ) );
        lblMediaGroupDesc.setText( entry.getGroupDesc() );

        btSortUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cntxt.sortGroupUp( entry.getGroup() );
            }
        });

        btSortDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cntxt.sortGroupDown( entry.getGroup() );
            }
        });

        btEditMediaGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cntxt.editGroup( entry.getGroup() );
            }
        });

        btDeleteMediaGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cntxt.deleteGroup( entry.getGroup() );
            }
        });

        return convertView;
    }
}

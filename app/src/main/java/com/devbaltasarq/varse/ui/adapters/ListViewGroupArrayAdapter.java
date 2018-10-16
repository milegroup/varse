package com.devbaltasarq.varse.ui.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.content.res.AppCompatResources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.core.experiment.ManualGroup;
import com.devbaltasarq.varse.core.experiment.VideoGroup;
import com.devbaltasarq.varse.ui.editexperiment.EditExperimentActivity;

/** Represents an adapter of the special items for the ListView of media files. */
public class ListViewGroupArrayAdapter extends ArrayAdapter<Group> {
    public ListViewGroupArrayAdapter(Context cntxt, Group[] entries)
    {
        super( cntxt, 0, entries );
    }

    @Override
    public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent)
    {
        final EditExperimentActivity cntxt = (EditExperimentActivity) this.getContext();
        final LayoutInflater layoutInflater = LayoutInflater.from( this.getContext() );
        final Group group = this.getItem( position );

        if ( group == null ) {
            throw new Error( "Group adapter: group is null" );
        }

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

        if ( group instanceof VideoGroup ) {
            groupDescImgId = R.drawable.ic_video_group_button;
        }
        else
        if ( group instanceof ManualGroup) {
            groupDescImgId = R.drawable.ic_manual_button;
        }

        ivMediaGroupDesc.setImageDrawable( AppCompatResources.getDrawable( cntxt, groupDescImgId ) );
        lblMediaGroupDesc.setText( group.toString() );

        btSortUp.setOnClickListener( (v) -> cntxt.sortGroupUp( group ) );
        btSortDown.setOnClickListener( (v) -> cntxt.sortGroupDown( group ) );
        btEditMediaGroup.setOnClickListener( (v) -> cntxt.editGroup( group ) );
        btDeleteMediaGroup.setOnClickListener( (v) -> cntxt.deleteGroup( group ) );

        return convertView;
    }
}

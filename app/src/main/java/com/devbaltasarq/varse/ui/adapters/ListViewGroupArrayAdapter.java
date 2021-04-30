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

/** Represents an adapter of the special items for the ListView of groups inside experiments. */
public class ListViewGroupArrayAdapter extends ArrayAdapter<Group> {
    public ListViewGroupArrayAdapter(Context cntxt, Group[] entries)
    {
        super( cntxt, 0, entries );
    }

    @Override
    public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent)
    {
        final EditExperimentActivity CONTEXT = (EditExperimentActivity) this.getContext();
        final LayoutInflater LAYOUT_INFLATER = LayoutInflater.from( this.getContext() );
        final Group GROUP = this.getItem( position );

        if ( GROUP == null ) {
            throw new Error( "Group adapter: group is null" );
        }

        if ( convertView == null ) {
            convertView = LAYOUT_INFLATER.inflate( R.layout.listview_media_group_entry, null );
        }

        final ImageButton BT_SORT_UP = convertView.findViewById( R.id.btSortMediaGroupUp );
        final ImageButton BT_SORT_DOWN = convertView.findViewById( R.id.btSortMediaGroupDown );
        final ImageButton BT_EDIT_MEDIA_GRP = convertView.findViewById( R.id.btEditMediaGroup );
        final ImageButton BT_DELETE_MEDIA_GRP = convertView.findViewById( R.id.btDeleteMediaGroup );
        final TextView LBL_MEDIA_GROUP_DESC = convertView.findViewById( R.id.lblMediaGroupDesc );
        final ImageView IV_MEDIA_GROUP_DESC = convertView.findViewById( R.id.ivMediaGroupDesc);

        // Set image and file name
        int groupDescImgId = R.drawable.ic_picture_group_button;

        if ( GROUP instanceof VideoGroup ) {
            groupDescImgId = R.drawable.ic_video_group_button;
        }
        else
        if ( GROUP instanceof ManualGroup) {
            groupDescImgId = R.drawable.ic_manual_button;
        }

        IV_MEDIA_GROUP_DESC.setImageDrawable( AppCompatResources.getDrawable( CONTEXT, groupDescImgId ) );
        LBL_MEDIA_GROUP_DESC.setText( GROUP.toString() );

        BT_SORT_UP.setOnClickListener( (v) -> CONTEXT.sortGroupUp( GROUP ) );
        BT_SORT_DOWN.setOnClickListener( (v) -> CONTEXT.sortGroupDown( GROUP ) );
        BT_EDIT_MEDIA_GRP.setOnClickListener( (v) -> CONTEXT.editGroup( GROUP ) );
        BT_DELETE_MEDIA_GRP.setOnClickListener( (v) -> CONTEXT.deleteGroup( GROUP ) );

        return convertView;
    }
}

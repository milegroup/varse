package com.devbaltasarq.varse.ui.editexperiment.editgroup;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
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
import com.devbaltasarq.varse.core.experiment.MediaGroup;
import com.devbaltasarq.varse.core.experiment.MimeTools;
import com.devbaltasarq.varse.core.experiment.VideoGroup;

import java.io.File;

/** Represents an adapter of the special items for the ListView of media files. */
public class ListViewActivityEntryArrayAdapter extends ArrayAdapter<ListViewActivityEntry> {
    public ListViewActivityEntryArrayAdapter(Context cntxt, ListViewActivityEntry[] entries)
    {
        super( cntxt, 0, entries );
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final EditGroupActivity cntxt = (EditGroupActivity) this.getContext();
        final LayoutInflater layoutInflater = LayoutInflater.from( this.getContext() );
        final ListViewActivityEntry entry = this.getItem( position );
        Bitmap thumbnail = null;

        if ( convertView == null ) {
            convertView = layoutInflater.inflate( R.layout.listview_media_entry, null );
        }

        final ImageButton btSortFileUp = convertView.findViewById( R.id.btSortMediaUp );
        final ImageButton btSortFileDown = convertView.findViewById( R.id.btSortMediaDown );
        final ImageButton btEditMedia = convertView.findViewById( R.id.btEditMedia );
        final ImageButton btDeleteMedia = convertView.findViewById( R.id.btDeleteMedia );
        final TextView lblMediaDesc = convertView.findViewById( R.id.lblMediaDesc );
        final ImageView ivMediaDesc = convertView.findViewById( R.id.ivMediaDesc );
        final ImageView ivThumbnail = convertView.findViewById( R.id.ivThumbnail );

        // Set image and file name
        int groupDescImgId = R.drawable.ic_picture_button;

        if ( entry.getActivity() instanceof MediaGroup.MediaActivity ) {
            final MediaGroup.MediaActivity mediaActivity = (MediaGroup.MediaActivity) entry.getActivity();

            if ( MimeTools.isPicture( mediaActivity.getFile() ) ) {
                thumbnail = getImageThumbnail( cntxt, new File( entry.getFileName() ) );
            } else {
                groupDescImgId = R.drawable.ic_video_button;
                thumbnail = getVideoThumbnail( cntxt, new File( entry.getFileName() ) );
            }

            btEditMedia.setVisibility( View.GONE );
        }
        else
        if ( entry.getActivity() instanceof ManualGroup.ManualActivity ) {
            btEditMedia.setVisibility( View.VISIBLE );
            groupDescImgId = R.drawable.ic_manual_button;
        }

        ivMediaDesc.setImageDrawable( AppCompatResources.getDrawable( cntxt, groupDescImgId ) );
        lblMediaDesc.setText( entry.getFileName() );

        btSortFileUp.setOnClickListener( (v) -> cntxt.sortActivityUp( entry.getActivity() ) );
        btSortFileDown.setOnClickListener( (v) -> cntxt.sortActivityDown( entry.getActivity() ) );
        btDeleteMedia.setOnClickListener( (v) -> cntxt.deleteActivity( entry.getActivity() ) );
        btEditMedia.setOnClickListener( (v) -> cntxt.editActivity( entry.getActivity() ) );

        // Thumbnail
        if ( thumbnail == null ) {
            ivThumbnail.setVisibility( View.GONE );
        } else {
            ivThumbnail.setImageBitmap( thumbnail );
        }

        return convertView;
    }

    /** Gets the thumbnail associated to an image file.
      * @param c The context.
      * @param f The path to the file.
      * @return A Bitmap object.
      * @see Bitmap
      */
    public static Bitmap getImageThumbnail(Context c, File f)
    {
        final String[] projection = { MediaStore.Images.Media.DATA };
        final Cursor cursor = MediaStore.Images.Thumbnails.query( c.getContentResolver(),
                                                            Uri.fromFile( f ),
                                                            projection );
        Bitmap toret = null;

        if ( cursor != null ) {
            int column_index = cursor.getColumnIndex( MediaStore.Images.Media._ID );

            if ( column_index >= 0 ) {
                cursor.moveToFirst();
                long imageId = cursor.getLong( column_index );

                toret = MediaStore.Images.Thumbnails.getThumbnail(
                        c.getContentResolver(), imageId,
                        MediaStore.Images.Thumbnails.MICRO_KIND,
                        null );
            }
        }

        return toret;
    }

    /** Gets the thumbnail associated to an image file.
     * @param c The context.
     * @param f The path to the file.
     * @return A Bitmap object.
     * @see Bitmap
     */
    public static Bitmap getVideoThumbnail(Context c, File f)
    {
        return ThumbnailUtils.createVideoThumbnail( f.getAbsolutePath(),
                                             MediaStore.Images.Thumbnails.MICRO_KIND );
    }
}

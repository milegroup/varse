package com.devbaltasarq.varse.ui.editexperiment.editgroup;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.content.res.AppCompatResources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.core.experiment.ManualGroup;
import com.devbaltasarq.varse.core.experiment.MediaGroup;
import com.devbaltasarq.varse.core.experiment.MimeTools;
import com.devbaltasarq.varse.core.experiment.VideoGroup;

import java.io.File;

/** Represents an adapter of the special items for the ListView of media files. */
public class ListViewActivityEntryArrayAdapter extends ArrayAdapter<ListViewActivityEntry> {
    public static final String LogTag = ListViewActivityEntryArrayAdapter.class.getSimpleName();

    public ListViewActivityEntryArrayAdapter(Context cntxt, ListViewActivityEntry[] entries)
    {
        super( cntxt, 0, entries );
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final Context context = this.getContext();
        final LayoutInflater layoutInflater = LayoutInflater.from( this.getContext() );
        final ListViewActivityEntry entry = this.getItem( position );
        final Group.Activity entryActivity = entry.getActivity();
        Bitmap thumbnail = null;
        EditGroupActivity editGroupActivity = null;

        if ( context instanceof EditGroupActivity ) {
            editGroupActivity = (EditGroupActivity) context;
        }

        if ( entryActivity == null ) {
            final String ERROR_MSG = "the activity target of this entry is null!!!";

            Log.e( LogTag, ERROR_MSG );
            throw new InternalError( ERROR_MSG );
        }

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

        if ( entryActivity instanceof MediaGroup.MediaActivity ) {
            final Orm db = Orm.get();
            final Experiment expr = entryActivity.getExperimentOwner();
            final MediaGroup mediaGroup = (MediaGroup) entryActivity.getGroup();

            if ( mediaGroup.getFormat() == MediaGroup.Format.Picture ) {
                thumbnail = getImageThumbnail( db, expr, new File( entry.getFileName() ) );
            } else {
                groupDescImgId = R.drawable.ic_video_button;
                thumbnail = getVideoThumbnail( db, expr, new File( entry.getFileName() ) );
            }

            if ( thumbnail != null ) {
                ivThumbnail.setImageBitmap( thumbnail );
                convertView.findViewById( R.id.lyMediaDesc ).setVisibility( View.GONE );
                convertView.findViewById( R.id.lyThumbnail ).setVisibility( View.VISIBLE );
            } else {
                convertView.findViewById( R.id.lyMediaDesc ).setVisibility( View.VISIBLE );
                convertView.findViewById( R.id.lyThumbnail ).setVisibility( View.GONE );
            }

            btEditMedia.setVisibility( View.GONE );
        }
        else
        if ( entry.getActivity() instanceof ManualGroup.ManualActivity ) {
            btEditMedia.setVisibility( View.VISIBLE );
            groupDescImgId = R.drawable.ic_manual_button;
            convertView.findViewById( R.id.lyThumbnail ).setVisibility( View.VISIBLE );
        }

        lblMediaDesc.setText( entry.getFileName() );
        ivMediaDesc.setImageDrawable( AppCompatResources.getDrawable( context, groupDescImgId ) );

        if ( editGroupActivity != null ) {
            final EditGroupActivity editAct = editGroupActivity;

            btSortFileUp.setOnClickListener( (v) -> editAct.sortActivityUp( entryActivity ) );
            btSortFileDown.setOnClickListener( (v) -> editAct.sortActivityDown( entryActivity ) );
            btDeleteMedia.setOnClickListener( (v) -> editAct.deleteActivity( entryActivity ) );
            btEditMedia.setOnClickListener( (v) -> editAct.editActivity( entryActivity ) );
        } else {
            btSortFileDown.setVisibility( View.GONE );
            btSortFileUp.setVisibility( View.GONE );
            btDeleteMedia.setVisibility( View.GONE );
            btEditMedia.setVisibility( View.GONE );
        }

        return convertView;
    }

    /** Gets the thumbnail associated to an image file.
      * @param f The path to the file.
      * @return A Bitmap object.
      * @see Bitmap
      */
    public static Bitmap getImageThumbnail(Orm db, Experiment expr, File f)
    {
        final File mediaFile = new File( db.buildMediaDirectoryFor( expr ), f.getName() );

        return ThumbnailUtils.extractThumbnail(
                                        BitmapFactory.decodeFile( mediaFile.getAbsolutePath() ),
                                        128,
                                        128 );
    }

    /** Gets the thumbnail associated to an image file.
     * @param f The path to the file.
     * @return A Bitmap object.
     * @see Bitmap
     */
    public static Bitmap getVideoThumbnail(Orm db, Experiment expr, File f)
    {
        final File mediaFile = new File( db.buildMediaDirectoryFor( expr ), f.getName() );

        return ThumbnailUtils.createVideoThumbnail( mediaFile.getAbsolutePath(),
                                             MediaStore.Images.Thumbnails.MICRO_KIND );
    }
}

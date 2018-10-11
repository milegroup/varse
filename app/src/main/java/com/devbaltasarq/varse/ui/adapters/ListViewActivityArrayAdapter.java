package com.devbaltasarq.varse.ui.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
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
import com.devbaltasarq.varse.ui.editexperiment.editgroup.EditGroupActivity;

import java.io.File;

/** Represents an adapter of the special items for the ListView of media files. */
public class ListViewActivityArrayAdapter extends ArrayAdapter<Group.Activity> {
    public static final String LogTag = ListViewActivityArrayAdapter.class.getSimpleName();

    public ListViewActivityArrayAdapter(Context cntxt, Group.Activity[] entries)
    {
        super( cntxt, 0, entries );
    }

    @Override
    public View getView(int position, View rowView, @NonNull ViewGroup parent)
    {
        final Context context = this.getContext();
        final LayoutInflater layoutInflater = LayoutInflater.from( this.getContext() );
        final Group.Activity entryActivity = this.getItem( position );
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

        if ( rowView == null ) {
            rowView = layoutInflater.inflate( R.layout.listview_media_entry, null );
        }

        final ImageButton btSortFileUp = rowView.findViewById( R.id.btSortMediaUp );
        final ImageButton btSortFileDown = rowView.findViewById( R.id.btSortMediaDown );
        final ImageButton btEditMedia = rowView.findViewById( R.id.btEditMedia );
        final ImageButton btDeleteMedia = rowView.findViewById( R.id.btDeleteMedia );
        final TextView lblMediaDesc = rowView.findViewById( R.id.lblMediaDesc );
        final ImageView ivMediaDesc = rowView.findViewById( R.id.ivMediaDesc );
        final ImageView ivThumbnail = rowView.findViewById( R.id.ivThumbnail );

        // Set image and file name
        int groupDescImgId = R.drawable.ic_picture_button;

        if ( entryActivity instanceof MediaGroup.MediaActivity ) {
            final String FILE_NAME = getFileNameOf( entryActivity );
            final Orm DB = Orm.get();
            final Experiment EXPR = entryActivity.getExperimentOwner();
            final MediaGroup MEDIA_GROUP = (MediaGroup) entryActivity.getGroup();

            if ( MEDIA_GROUP.getFormat() == MediaGroup.Format.Picture ) {
                thumbnail = getImageThumbnail( DB, EXPR, new File( FILE_NAME ) );
            } else {
                groupDescImgId = R.drawable.ic_video_button;
                thumbnail = getVideoThumbnail( DB, EXPR, new File( FILE_NAME ) );
            }

            if ( thumbnail != null ) {
                ivThumbnail.setImageBitmap( thumbnail );
                rowView.findViewById( R.id.lyMediaDesc ).setVisibility( View.GONE );
                rowView.findViewById( R.id.lyThumbnail ).setVisibility( View.VISIBLE );
            } else {
                rowView.findViewById( R.id.lyMediaDesc ).setVisibility( View.VISIBLE );
                rowView.findViewById( R.id.lyThumbnail ).setVisibility( View.GONE );
            }

            btEditMedia.setVisibility( View.GONE );
            lblMediaDesc.setText( FILE_NAME );
        }
        else
        if ( entryActivity instanceof ManualGroup.ManualActivity ) {
            lblMediaDesc.setText( entryActivity.toString() );
            btEditMedia.setVisibility( View.VISIBLE );
            groupDescImgId = R.drawable.ic_manual_button;
            rowView.findViewById( R.id.lyThumbnail ).setVisibility( View.GONE );
        }

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

        return rowView;
    }

    /** Gets the thumbnail associated to an image file.
      * @param f The path to the file.
      * @return A Bitmap object.
      * @see Bitmap
      */
    private static Bitmap getImageThumbnail(Orm db, Experiment expr, File f)
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
    private static Bitmap getVideoThumbnail(Orm db, Experiment expr, File f)
    {
        final File mediaFile = new File( db.buildMediaDirectoryFor( expr ), f.getName() );

        return ThumbnailUtils.createVideoThumbnail( mediaFile.getAbsolutePath(),
                                             MediaStore.Images.Thumbnails.MICRO_KIND );
    }

    /** Gets the name of the file for a given activity,
      * @param activity the activity to extract the name from
      * @return the name of the file.
      */
    private static String getFileNameOf(Group.Activity activity)
    {
        String toret = activity.toString();

        if ( activity instanceof MediaGroup.MediaActivity ) {
            MediaGroup.MediaActivity mact = (MediaGroup.MediaActivity) activity;

            toret = mact.getFile().getName();
        } else {
            Log.e( LogTag, "not a media activity, no associated file" );
        }

        return toret;
    }
}

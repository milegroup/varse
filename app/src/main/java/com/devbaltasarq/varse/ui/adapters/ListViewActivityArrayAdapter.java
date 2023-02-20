package com.devbaltasarq.varse.ui.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Ofm;
import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.core.experiment.ManualGroup;
import com.devbaltasarq.varse.core.experiment.MediaGroup;
import com.devbaltasarq.varse.ui.editexperiment.editgroup.EditGroupActivity;

import java.io.File;
import java.io.IOException;
import java.util.Collection;


/** Represents an adapter of the special items for the ListView of media files. */
public class ListViewActivityArrayAdapter extends ArrayAdapter<Group.Activity> {
    public static final String LOG_TAG = ListViewActivityArrayAdapter.class.getSimpleName();

    public ListViewActivityArrayAdapter(Context cntxt, Collection<? extends Group.Activity> entries)
    {
        super( cntxt, 0, entries.toArray( new Group.Activity[ 0 ] ) );
    }

    @Override
    public @NonNull View getView(int position, View rowView, @NonNull ViewGroup parent)
    {
        final Context CONTEXT = this.getContext();
        final LayoutInflater LAYOUT_INFLATER = LayoutInflater.from( this.getContext() );
        final Group.Activity ENTRY_ACTIVITY = this.getItem( position );
        Bitmap thumbnail;
        EditGroupActivity editGroupActivity = null;

        if ( CONTEXT instanceof EditGroupActivity ) {
            editGroupActivity = (EditGroupActivity) CONTEXT;
        }

        if ( ENTRY_ACTIVITY == null ) {
            final String ERROR_MSG = "the activity target of this entry is null!!!";

            Log.e(LOG_TAG, ERROR_MSG );
            throw new InternalError( ERROR_MSG );
        }

        if ( rowView == null ) {
            rowView = LAYOUT_INFLATER.inflate( R.layout.listview_media_entry, null );
        }

        final ImageButton BT_SORT_FILE_UP = rowView.findViewById( R.id.btSortMediaUp );
        final ImageButton BT_SORT_FILE_DOWN = rowView.findViewById( R.id.btSortMediaDown );
        final ImageButton BT_EDIT_MEDIA = rowView.findViewById( R.id.btEditMedia );
        final ImageButton BT_DELETE_MEDIA = rowView.findViewById( R.id.btDeleteMedia );
        final TextView LBL_MEDIA_DESC = rowView.findViewById( R.id.lblMediaDesc );
        final ImageView IV_MEDIA_DESC = rowView.findViewById( R.id.ivMediaDesc );
        final ImageView IV_THUMBNAIL = rowView.findViewById( R.id.ivThumbnail );

        // Set image and file name
        int groupDescImgId = R.drawable.ic_picture_button;

        if ( ENTRY_ACTIVITY instanceof MediaGroup.MediaActivity ) {
            final String FILE_NAME = getAssociatedFileNameOf( ENTRY_ACTIVITY );
            final Ofm DB = Ofm.get();
            final Experiment EXPR = ENTRY_ACTIVITY.getExperimentOwner();
            final MediaGroup MEDIA_GROUP = (MediaGroup) ENTRY_ACTIVITY.getGroup();

            if ( MEDIA_GROUP.getFormat() == MediaGroup.Format.Picture ) {
                thumbnail = getImageThumbnail( DB, EXPR, new File( FILE_NAME ) );
            } else {
                groupDescImgId = R.drawable.ic_video_button;
                thumbnail = getVideoThumbnail( DB, EXPR, new File( FILE_NAME ) );
            }

            if ( thumbnail != null ) {
                final Bitmap BMP_WHOLE;

                if ( MEDIA_GROUP.getFormat() == MediaGroup.Format.Picture ) {
                    BMP_WHOLE = BitmapFactory.decodeFile(
                                    new File( DB.buildMediaDirectoryFor( EXPR ), FILE_NAME )
                                                .getAbsolutePath() );
                } else {
                    BMP_WHOLE = thumbnail;
                }

                IV_THUMBNAIL.setImageBitmap( thumbnail );
                IV_THUMBNAIL.setOnClickListener( (v) -> this.showBig( BMP_WHOLE ) );

                rowView.findViewById( R.id.lyMediaDesc ).setVisibility( View.GONE );
                rowView.findViewById( R.id.lyThumbnail ).setVisibility( View.VISIBLE );
            } else {
                rowView.findViewById( R.id.lyMediaDesc ).setVisibility( View.VISIBLE );
                rowView.findViewById( R.id.lyThumbnail ).setVisibility( View.GONE );
            }

            BT_EDIT_MEDIA.setVisibility( View.GONE );
            LBL_MEDIA_DESC.setText( FILE_NAME );
        }
        else
        if ( ENTRY_ACTIVITY instanceof ManualGroup.ManualActivity ) {
            LBL_MEDIA_DESC.setText( ENTRY_ACTIVITY.toString() );
            BT_EDIT_MEDIA.setVisibility( View.VISIBLE );
            groupDescImgId = R.drawable.ic_manual_button;
            rowView.findViewById( R.id.lyThumbnail ).setVisibility( View.GONE );
        } else {
            throw new Error( "Activity adapter: unsupported activity type" );
        }


        IV_MEDIA_DESC.setImageDrawable( AppCompatResources.getDrawable( CONTEXT, groupDescImgId ) );

        if ( editGroupActivity != null ) {
            final EditGroupActivity EDIT_ACT = editGroupActivity;

            BT_SORT_FILE_UP.setOnClickListener( (v) -> EDIT_ACT.sortActivityUp( ENTRY_ACTIVITY ) );
            BT_SORT_FILE_DOWN.setOnClickListener( (v) -> EDIT_ACT.sortActivityDown( ENTRY_ACTIVITY ) );
            BT_DELETE_MEDIA.setOnClickListener( (v) -> EDIT_ACT.deleteActivity( ENTRY_ACTIVITY ) );
            BT_EDIT_MEDIA.setOnClickListener( (v) -> EDIT_ACT.editActivity( ENTRY_ACTIVITY ) );
        } else {
            BT_SORT_FILE_DOWN.setVisibility( View.GONE );
            BT_SORT_FILE_UP.setVisibility( View.GONE );
            BT_DELETE_MEDIA.setVisibility( View.GONE );
            BT_EDIT_MEDIA.setVisibility( View.GONE );
        }

        return rowView;
    }

    /** Gets the thumbnail associated to an image file.
      * @param f The path to the file.
      * @return A Bitmap object.
      * @see Bitmap
      */
    private static Bitmap getImageThumbnail(Ofm db, Experiment expr, File f)
    {
        final File MEDIA_FILE = new File( db.buildMediaDirectoryFor( expr ), f.getName() );

        return ThumbnailUtils.extractThumbnail(
                                        BitmapFactory.decodeFile( MEDIA_FILE.getAbsolutePath() ),
                                        128,
                                        128 );
    }

    /** Gets the thumbnail associated to an image file.
     * @param f The path to the file.
     * @return A Bitmap object, or null if any problem arose.
     * @see Bitmap
     */
    private static Bitmap getVideoThumbnail(Ofm db, Experiment expr, File f)
    {
        final File MEDIA_FILE = new File( db.buildMediaDirectoryFor( expr ), f.getName() );
        Bitmap toret = null;

        try {
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ) {
                toret = ThumbnailUtils.createVideoThumbnail(
                                        MEDIA_FILE,
                                        Size.parseSize( "256x256" ),
                                        null );
            }
        } catch(IOException exc) {
            Log.e( LOG_TAG, "creating thumbail for: " + f.getName() );
        }

        return toret;
    }

    /** Gets the name of the file for a given activity,
      * @param activity the activity to extract the name from
      * @return the name of the file.
      */
    private static String getAssociatedFileNameOf(Group.Activity activity)
    {
        String toret = activity.toString();

        if ( activity instanceof MediaGroup.MediaActivity ) {
            MediaGroup.MediaActivity mact = (MediaGroup.MediaActivity) activity;

            toret = mact.getFile().getName();
        } else {
            Log.e(LOG_TAG, "not a media activity, no associated file" );
        }

        return toret;
    }

    /** Shows the bitmap associated with the thumnail. */
    private void showBig(Bitmap bmp)
    {
        final Context CNTXT = this.getContext();
        final AlertDialog.Builder DLG = new AlertDialog.Builder( CNTXT );
        final ImageView IMG_VIEWER = new ImageView( CNTXT );

        IMG_VIEWER.setImageBitmap( bmp );

        DLG.setView( IMG_VIEWER );
        DLG.create().show();
    }
}

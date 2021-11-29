package com.devbaltasarq.varse.ui.editexperiment.editgroup;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Ofm;
import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.core.experiment.MediaGroup;
import com.devbaltasarq.varse.core.experiment.PictureGroup;
import com.devbaltasarq.varse.core.experiment.VideoGroup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


public class EditMediaGroupActivity extends EditGroupActivity {
    private static final String LOG_TAG = EditMediaGroupActivity.class.getSimpleName();
    private final int RQC_PICK_MEDIA = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_edit_media_group );

        final EditText ED_TAG = this.findViewById( R.id.edTag );
        final EditText ED_TIME = this.findViewById( R.id.edDuration );
        final Spinner CB_TIME_UNIT = this.findViewById( R.id.cbTimeUnit );
        final LinearLayout LL_DURATION_GRP = this.findViewById( R.id.llDurationGroup );
        final FloatingActionButton FB_ADD_MEDIA = this.findViewById( R.id.fbAddMedia );
        final FloatingActionButton FB_SAVE = this.findViewById( R.id.fbSaveMediaGroup );
        final ImageButton BT_CLOSE_EDIT_MEDIA_GRP = this.findViewById( R.id.btCloseEditMediaGroup );

        // Spinner for time units
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource( this,
                R.array.vTimeUnitChoices, android.R.layout.simple_spinner_item );
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
        CB_TIME_UNIT.setAdapter( adapter );

        // Buttons
        FB_SAVE.setOnClickListener( (v) -> this.finishWithResultCode( RSC_SAVE_DATA ) );
        BT_CLOSE_EDIT_MEDIA_GRP.setOnClickListener( (v) -> this.finishWithResultCode( RSC_DISMISS_DATA ) );
        FB_ADD_MEDIA.setOnClickListener( (v) -> this.openMedia() );

        // Show the appropriate image
        int resId = R.drawable.ic_picture;
        final ImageView IV_GRP_FMT = this.findViewById( R.id.ivMediaGroupFormat );

        if ( group instanceof VideoGroup) {
            resId = R.drawable.ic_video;
            LL_DURATION_GRP.setVisibility( View.GONE );
        }
        else
        if ( group instanceof PictureGroup) {
            resId = R.drawable.ic_picture;
            LL_DURATION_GRP.setVisibility( View.VISIBLE );
        } else {
            assert false: "EditMediaGroup: not a media group?";
        }

        IV_GRP_FMT.setImageDrawable(ContextCompat.getDrawable( this, resId ) );

        // Answer to events
        CB_TIME_UNIT.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                EditMediaGroupActivity.this.writeDurationToPictureGroup();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
                CB_TIME_UNIT.setSelection( 0 );
            }
        });

        ED_TIME.addTextChangedListener( new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                EditMediaGroupActivity.this.writeDurationToPictureGroup();
            }
        });

        ED_TAG.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                fillInTagObj( ( (MediaGroup) group ).getTag(), editable.toString() );
            }
        });

        this.setTitle( "" );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        final EditText ED_TIME = this.findViewById( R.id.edDuration );
        final Spinner CB_TIME_UNIT = this.findViewById( R.id.cbTimeUnit );
        final CheckBox CB_RANDOM = this.findViewById( R.id.cbRandom );
        final EditText ED_TAG = this.findViewById( R.id.edTag );

        assert group != null: "Group cannot be null in EditMediaGroupActivity";

        final MediaGroup MEDIA_GRP = (MediaGroup) group;

        ED_TAG.setText( MEDIA_GRP.getTag().toString() );
        CB_RANDOM.setChecked( MEDIA_GRP.isRandom() );

        if ( group instanceof PictureGroup ) {
            final PictureGroup PICTURE_GRP = (PictureGroup) group;

            fillInDurationInUI( PICTURE_GRP.getTimeForEachActivity(), CB_TIME_UNIT, ED_TIME );
        }

        this.showActivities();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if ( requestCode == RQC_PICK_MEDIA
          && resultCode == RESULT_OK )
        {
            final Uri URI = data.getData();

            if ( URI != null ) {
                this.storeMedia( URI );
            } else {
                this.showStatus(LOG_TAG, this.getString( R.string.msgFileNotFound ) );
            }
        }

        return;
    }

    /** Writes the duration time into the object. */
    private void writeDurationToPictureGroup()
    {
        final EditText ED_DURATION = this.findViewById( R.id.edDuration );
        final Spinner CB_TIME_UNIT = this.findViewById( R.id.cbTimeUnit );

        if ( group instanceof PictureGroup ) {
            final PictureGroup PICTURE_GRP = (PictureGroup) group;

            fillInDurationInObj( PICTURE_GRP.getTimeForEachActivity(), CB_TIME_UNIT, ED_DURATION );
        }

        return;
    }

    /** Launch file browser. */
    private void openMedia()
    {
        final Intent INTENT_OPEN = new Intent();

        // Choose the intent type, video or image
        String intentTypeStr = "image/*";

        if ( group instanceof VideoGroup ) {
            intentTypeStr = "video/*";
        }

        // Launch
        INTENT_OPEN.setType( intentTypeStr );
        INTENT_OPEN.setAction( Intent.ACTION_GET_CONTENT );

        this.startActivityForResult(
                Intent.createChooser( INTENT_OPEN, this.getString( R.string.lblMediaSelection ) ),
                RQC_PICK_MEDIA);
    }

    /** Stores the media in the db, and creates the media activity
     * in the given media group.
     * @param db the orm to operate with
     * @param mediaGroup the mediagroup to create the media activity into
     */
    private void storeMediaFor(Ofm db, Uri uri, MediaGroup mediaGroup)
    {
        try {
            String mediaFileName = uri.getLastPathSegment();

            if ( mediaFileName == null
              || mediaFileName.trim().isEmpty() )
            {
                throw new IllegalArgumentException( "empty file name" );
            } else {
                final InputStream IN = this.getContentResolver().openInputStream( uri );

                mediaFileName = new File( mediaFileName.trim() ).getName();

                if ( IN != null ) {
                    final Experiment EXPERIMENT = mediaGroup.getExperimentOwner();

                    mediaFileName = Ofm.buildMediaFileNameForDbFromMediaFileName( mediaFileName );

                    if ( !db.existsMedia( EXPERIMENT, mediaFileName ) ) {
                        final File F = db.storeMedia( EXPERIMENT, mediaFileName, IN );
                        mediaGroup.add( new MediaGroup.MediaActivity( Id.create(), F ) );
                    } else {
                        this.showStatus( LOG_TAG, this.getString( R.string.msgMediaAlreadyAdded ) );
                    }
                } else {
                    this.showStatus( LOG_TAG, this.getString( R.string.msgFileNotFound ) );
                }
            }
        } catch(IOException exc) {
            this.showStatus( LOG_TAG, this.getString( R.string.errIO) );
        } catch(IllegalArgumentException exc) {
            this.showStatus( LOG_TAG, this.getString( R.string.errUnsupportedFileType) );
        }

        return;
    }

    /** Stores the media resource in the app's filesystem. */
    private void storeMedia(Uri uri)
    {
        final Ofm DB = Ofm.get();
        final MediaGroup MEDIA_GRP = (MediaGroup) group;

        if ( uri == null
          || uri.getScheme() == null )
        {
            this.showStatus( LOG_TAG, this.getString( R.string.msgFileNotFound ) );
        }
        else
        if ( uri.getScheme().equals( ContentResolver.SCHEME_CONTENT )
          || uri.getScheme().equals( ContentResolver.SCHEME_FILE ) )
        {
            this.storeMediaFor( DB, uri, MEDIA_GRP );
        } else {
            this.showStatus( LOG_TAG, this.getString( R.string.errUnsupportedFileType) );
        }

        return;
    }

    @Override
    public void addActivity()
    {
        this.openMedia();
    }

    @Override
    public void deleteActivity(Group.Activity act)
    {
        final Ofm DB = Ofm.get();
        final MediaGroup.MediaActivity MEDIA_ACTIVITY = (MediaGroup.MediaActivity) act;
        final String FILE_NAME = MEDIA_ACTIVITY.getFile().getName();
        final Experiment OWNER = group.getExperimentOwner();

        if ( DB.existsMedia( OWNER, FILE_NAME ) ) {
            try {
                DB.deleteMedia( OWNER, FILE_NAME );
            } catch(IOException exc) {
                this.showStatus(LOG_TAG, exc.getMessage() );
            }
        }

        super.deleteActivity( act );
    }

    @Override
    public void editActivity(Group.Activity act)
    {
        // Nothing to do here, media files cannot be edited.
    }
}

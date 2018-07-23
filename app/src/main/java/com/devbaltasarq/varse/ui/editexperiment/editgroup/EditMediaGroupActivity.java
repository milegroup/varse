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
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.core.experiment.MediaGroup;
import com.devbaltasarq.varse.core.experiment.MimeTools;
import com.devbaltasarq.varse.core.experiment.PictureGroup;
import com.devbaltasarq.varse.core.experiment.VideoGroup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


public class EditMediaGroupActivity extends EditGroupActivity {
    private static final String LogTag = EditMediaGroupActivity.class.getSimpleName();
    private final int RQC_PICK_MEDIA = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView( R.layout.activity_edit_media_group );

        final EditText edTag = this.findViewById( R.id.edTag );
        final EditText edTime = this.findViewById( R.id.edDuration );
        final Spinner cbTimeUnit = this.findViewById( R.id.cbTimeUnit );
        final LinearLayout llDurationGrp = this.findViewById( R.id.llDurationGroup );
        final FloatingActionButton fbAddMedia = this.findViewById( R.id.fbAddMedia );
        final FloatingActionButton fbSave = this.findViewById( R.id.fbSaveMediaGroup );
        final ImageButton btCloseEditMediaGroup = this.findViewById( R.id.btCloseEditMediaGroup );

        // Spinner for time units
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource( this,
                R.array.vTimeUnitChoices, android.R.layout.simple_spinner_item );
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
        cbTimeUnit.setAdapter( adapter );

        // Buttons
        fbSave.setOnClickListener( (v) -> this.finishWithResultCode( RSC_SAVE_DATA ) );
        btCloseEditMediaGroup.setOnClickListener( (v) -> this.finishWithResultCode( RSC_DISMISS_DATA ) );
        fbAddMedia.setOnClickListener( (v) -> this.openMedia() );

        // Show the appropriate image
        int resId = R.drawable.ic_picture;
        final ImageView ivGroupFormat = this.findViewById( R.id.ivMediaGroupFormat );

        if ( group instanceof VideoGroup) {
            resId = R.drawable.ic_video;
            llDurationGrp.setVisibility( View.GONE );
        }
        else
        if ( group instanceof PictureGroup) {
            resId = R.drawable.ic_picture;
            llDurationGrp.setVisibility( View.VISIBLE );
        } else {
            assert false: "EditMediaGroup: not a media group?";
        }

        ivGroupFormat.setImageDrawable(ContextCompat.getDrawable( this, resId ) );

        // Answer to events
        cbTimeUnit.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                EditMediaGroupActivity.this.writeDurationToPictureGroup();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
                cbTimeUnit.setSelection( 0 );
            }
        });

        edTime.addTextChangedListener( new TextWatcher() {
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

        edTag.addTextChangedListener(new TextWatcher() {
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

        final EditText edTime = this.findViewById( R.id.edDuration );
        final Spinner cbTimeUnit = this.findViewById( R.id.cbTimeUnit );
        final CheckBox cbRandom = this.findViewById( R.id.cbRandom );
        final EditText edTag = this.findViewById( R.id.edTag );

        assert group != null: "Group cannot be null in EditMediaGroupActivity";

        final MediaGroup mediaGroup = (MediaGroup) group;

        edTag.setText( mediaGroup.getTag().toString() );
        cbRandom.setChecked( mediaGroup.isRandom() );

        if ( group instanceof PictureGroup ) {
            final PictureGroup pictureGroup = (PictureGroup) group;

            fillInDurationInUI( pictureGroup.getTimeForPic(), cbTimeUnit, edTime );
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
            final Uri uri = data.getData();

            if ( uri != null ) {
                this.storeMedia( uri );
            } else {
                this.showStatus( LogTag, this.getString( R.string.msgFileNotFound ) );
            }
        }

        return;
    }

    /** Writes the duration time into the object. */
    private void writeDurationToPictureGroup()
    {
        final EditText edDuration = this.findViewById( R.id.edDuration );
        final Spinner cbTimeUnit = this.findViewById( R.id.cbTimeUnit );

        if ( group instanceof PictureGroup ) {
            final PictureGroup pictureGroup = (PictureGroup) group;

            fillInDurationInObj( pictureGroup.getTimeForPic(), cbTimeUnit, edDuration );
        }

        return;
    }

    /** Launch file browser. */
    private void openMedia()
    {
        final Intent intent = new Intent();

        // Choose the intent type, video or image
        String intentTypeStr = "image/*";

        if ( group instanceof VideoGroup ) {
            intentTypeStr = "video/*";
        }

        // Launch
        intent.setType( intentTypeStr );
        intent.setAction( Intent.ACTION_GET_CONTENT );

        this.startActivityForResult(
                Intent.createChooser( intent, this.getString( R.string.lblMediaSelection ) ),
                RQC_PICK_MEDIA);
    }

    /** Stores the media file in the app's filesystem. */
    private void storeMedia(Uri uri)
    {
        if ( uri != null
          && ( uri.getScheme().equals( ContentResolver.SCHEME_CONTENT )
            || uri.getScheme().equals( ContentResolver.SCHEME_FILE ) ) )
        {
            try {
                final Orm db = Orm.get();
                final File mediaFile = new File( uri.getLastPathSegment() );
                final InputStream in = this.getContentResolver().openInputStream( uri );
                final MediaGroup mediaGroup = (MediaGroup) group;
                final boolean isVideoGroup = mediaGroup.getFormat() == MediaGroup.Format.Video;
                final Experiment experiment = group.getExperimentOwner();

                if ( !db.existsMedia( experiment, mediaFile.getName() ) ) {
                    if ( MimeTools.isVideo( mediaFile ) == isVideoGroup ) {
                        final File f = db.storeMedia( experiment, mediaFile.getName(), in );
                        mediaGroup.add( new MediaGroup.MediaActivity( Id.create(), f ) );
                    } else {
                        this.showStatus( LogTag, this.getString( R.string.msgIncompatibleMedia ) );
                    }
                } else {
                    this.showStatus( LogTag, this.getString( R.string.msgMediaAlreadyAdded ) );
                }
            } catch(IOException exc) {
                this.showStatus( LogTag, this.getString( R.string.ErrIO ) );
            }
        } else {
            this.showStatus( LogTag, this.getString( R.string.msgFileNotFound ) );
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
        final Orm db = Orm.get();
        final MediaGroup.MediaActivity mediaActivity = (MediaGroup.MediaActivity) act;
        final String fileName = mediaActivity.getFile().getName();
        final Experiment owner = group.getExperimentOwner();

        if ( db.existsMedia( owner, fileName ) ) {
            try {
                db.deleteMedia( owner, fileName );
            } catch(IOException exc) {
                this.showStatus( LogTag, exc.getMessage() );
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

package com.devbaltasarq.varse.core.experiment;

import android.util.JsonWriter;

import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Ofm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Represents a group of Pictures. */
public class PictureGroup extends MediaGroup {
    public static final String LOG_TAG = PictureGroup.class.getSimpleName();

    public PictureGroup(Id id, Experiment expr)
    {
        this( id, Tag.NO_TAG, DEFAULT_TIME_FOR_ACTIVITY, false, expr, new ArrayList<>() );
    }

    public PictureGroup(Id id, Tag tag, Duration timesForPic, Experiment expr)
    {
        this( id, tag, timesForPic, false, expr, new ArrayList<>() );
    }

    public PictureGroup(Id id, Tag tag, Duration timesForPic, Experiment expr, boolean rnd)
    {
        this( id, tag, timesForPic, rnd, expr, new ArrayList<>() );
    }

    public PictureGroup(Id id, Tag tag, Experiment expr, List<MediaActivity> files)
    {
        this( id, tag, DEFAULT_TIME_FOR_ACTIVITY, false, expr, files );
    }

    public PictureGroup(Id id, Tag tag, Duration timesForPic, Experiment expr, List<MediaActivity> files)
    {
        this( id, tag, timesForPic, false, expr, files );
    }

    public PictureGroup(Id id, Tag tag, Duration timesForPics, boolean rnd, Experiment expr, List<MediaActivity> files)
    {
        super( id, Format.Picture, tag, rnd, expr, files );

        this.setTimeForEachActivity( timesForPics );
    }

    @Override
    public TypeId getTypeId()
    {
        return TypeId.PictureGroup;
    }


    @Override
    public void add(MediaActivity mact)
    {
        final File F = mact.getFile();

        if ( !Ofm.extractFileExt( F ).isEmpty() ) {
            if ( !MimeTools.isPicture( mact.getFile() ) ) {
                throw new Error( mact.getFile() + " not a picture." );
            }
        }

        super.add(mact);
    }

    @Override
    public void writeToJSON(JsonWriter jsonWriter) throws IOException
    {
        super.writeToJSON( jsonWriter );

        jsonWriter.name( Duration.FIELD ).value( this.getTimeForEachActivity().getTimeInSeconds() );
    }

    @Override
    protected PictureGroup copy(Id id)
    {
        return new PictureGroup( id,
                                 this.getTag(),
                                 this.getTimeForEachActivity(),
                                 this.isRandom(),
                                 this.getExperimentOwner(),
                                 this.get() );
    }
}

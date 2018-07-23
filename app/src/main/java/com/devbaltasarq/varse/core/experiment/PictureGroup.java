package com.devbaltasarq.varse.core.experiment;

import android.util.JsonWriter;

import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;

import org.json.JSONException;

import java.io.IOException;

/** Represents a group of Pictures. */
public class PictureGroup extends MediaGroup {
    public static final String LogTag = PictureGroup.class.getSimpleName();

    public PictureGroup(Id id, Experiment expr)
    {
        this( id, Tag.NO_TAG, DEFAULT_TIME_FOR_ACTIVITY, false, expr, new MediaActivity[] {} );
    }

    public PictureGroup(Id id, Tag tag, Experiment expr, MediaActivity[] files)
    {
        this( id, tag, DEFAULT_TIME_FOR_ACTIVITY, false, expr, files );
    }

    public PictureGroup(Id id, Tag tag, Duration timesForPic, Experiment expr)
    {
        this( id, tag, timesForPic, false, expr, new MediaActivity[] {} );

    }

    public PictureGroup(Id id, Tag tag, Duration timesForPic, Experiment expr, MediaActivity[] files)
    {
        this( id, tag, timesForPic, false, expr, files );
    }

    public PictureGroup(Id id, Tag tag, Duration timesForPic, boolean rnd, Experiment expr)
    {
        this( id, tag, timesForPic, rnd, expr, new MediaActivity[] {} );
    }

    public PictureGroup(Id id, Tag tag, Duration timesForPic, boolean rnd, Experiment expr, MediaActivity[] files)
    {
        super( id, Format.Picture, tag, rnd, expr, files );

        this.timeForPic = timesForPic;
    }

    @Override
    public TypeId getTypeId()
    {
        return TypeId.PictureGroup;
    }


    @Override
    public void add(MediaActivity act)
    {
        if ( !MimeTools.isPicture( act.getFile() ) ) {
            throw new Error( act.getFile() + " not a picture." );
        }

        super.add(act);
    }

    /** @return The number of seconds each picture is shown for. */
    public Duration getTimeForPic()
    {
        return timeForPic;
    }

    /** Changes the amount of seconds each picture is shown for. */
    public void setTimeForPic(Duration timeForPic)
    {
        this.timeForPic = timeForPic;
    }

    @Override
    public void writeToJSON(JsonWriter jsonWriter) throws IOException
    {
        super.writeToJSON( jsonWriter );

        jsonWriter.name( Duration.FIELD ).value( this.getTimeForPic().getTimeInSeconds() );
    }

    @Override
    public PictureGroup copy(Id id)
    {
        return new PictureGroup( id,
                                 this.getTag().copy(),
                                 this.getTimeForPic().copy(),
                                 this.isRandom(),
                                 this.getExperimentOwner(),
                                 this.copyActivities() );
    }

    private Duration timeForPic;
}

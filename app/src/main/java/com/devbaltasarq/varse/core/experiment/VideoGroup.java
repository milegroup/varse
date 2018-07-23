package com.devbaltasarq.varse.core.experiment;

import android.util.JsonWriter;

import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;

import java.io.IOException;

/** Represents a group of various videos. */
public class VideoGroup extends MediaGroup {
    /** Creates a new, empty video group. */
    public VideoGroup(Id id, Experiment expr)
    {
        this( id, Tag.NO_TAG, false, expr, new MediaActivity[] {} );
    }

    /** Creates a new, empty video group, with a given tag. */
    public VideoGroup(Id id, Tag tag, Experiment expr)
    {
        this( id, tag, false, expr, new MediaActivity[] {} );
    }

    /** Creates a new, empty video group, with a given tag and randomness. */
    public VideoGroup(Id id, Tag tag, boolean rnd, Experiment expr)
    {
        this( id, tag, rnd, expr, new MediaActivity[] {} );
    }

    /** Creates a new, empty video group, with a given tag and a few files. */
    public VideoGroup(Id id, Tag tag, Experiment expr, MediaActivity[] acts)
    {
        this( id, tag, false, expr, acts );
    }

    /** Creates a new video group with a given tag, random or not, and a few files. */
    public VideoGroup(Id id, Tag tag, boolean rnd, Experiment expr, MediaActivity[] files)
    {
        super( id, Format.Video, tag, rnd, expr, files );
    }

    @Override
    public TypeId getTypeId()
    {
        return TypeId.VideoGroup;
    }

    @Override
    public void add(MediaActivity act)
    {
        if ( !MimeTools.isVideo( act.getFile() ) ) {
            throw new Error( act.getFile() + " not a video." );
        }

        super.add(act);
    }

    @Override
    public VideoGroup copy(Id id)
    {
        return new VideoGroup( id,
                               this.getTag().copy(),
                               this.isRandom(),
                               this.getExperimentOwner(),
                               this.copyActivities() );
    }
}

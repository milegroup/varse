package com.devbaltasarq.varse.core.experiment;


import android.media.MediaMetadataRetriever;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Orm;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


/** Represents a group of various media files. */
public abstract class MediaGroup extends Group<MediaGroup.MediaActivity> {
    final static String LogTag = "MediaGroup";
    public enum Format { Picture, Video }

    public static class MediaActivity extends Group.Activity {
        public MediaActivity(Id id, File f)
        {
            super( id, new Tag( f.getName() ) );
            this.file = new File( Orm.buildMediaFileNameForDbFromMediaFileName( f.getName() ) );
        }

        @Override
        public int hashCode()
        {
            return ( 13 * this.getId().hashCode() ) + ( 17 * this.getFile().hashCode() );
        }

        @Override
        public boolean equals(Object o)
        {
            boolean toret = false;

            if ( o instanceof MediaActivity ) {
                final MediaActivity mao = (MediaActivity) o;

                toret = this.getTag().equals( mao.getTag() );
            }

            return toret;
        }

        @Override
        public TypeId getTypeId()
        {
            return TypeId.MediaActivity;
        }

        @Override
        public Duration getTime()
        {
            final MediaGroup MEDIA_GROUP = ( (MediaGroup) this.getGroup() );
            Duration toret;

            if ( MEDIA_GROUP.getFormat() == Format.Picture ) {
                toret = MEDIA_GROUP.getTimeForEachActivity();
            } else {
                toret = new Duration( calculateVideoDuration( Orm.get(), this ) );
            }


            return toret;
        }

        /** @return the file subject to this activity. */
        public File getFile()
        {
            return this.file;
        }

        @Override
        public String toString()
        {
            return this.getFile().getName();
        }

        @Override
        public File[] enumerateMediaFiles()
        {
            return new File[]{ this.getFile() };
        }

        /** Writes the common properties of a media activity to JSON.
         * @param jsonWriter The JSON writer to write to.
         * @throws IOException throws it when there are problems with the stream.
         */
        @Override
        public void writeToJSON(JsonWriter jsonWriter) throws IOException
        {
            this.writeIdToJSON( jsonWriter );
            jsonWriter.name( Orm.FIELD_FILE ).value( this.getFile().getName() );
        }

        /** Creates a new media activity, from JSON data.
         * @param id The id of the future activity.
         * @param jsonReader The reader from which to extract data.
         * @throws JSONException if data is not found or invalid.
         */
        public static MediaActivity fromJSON(Id id, JsonReader jsonReader) throws JSONException
        {
            File file = null;

            // Load data
            try {
                while ( jsonReader.hasNext() ) {
                    final String token = jsonReader.nextName();

                    if ( token.equals( Orm.FIELD_FILE ) ) {
                        file = new File( jsonReader.nextString() );
                    }
                }
            } catch(IOException exc)
            {
                Log.e( LogTag, "ManualActivity.fromJSON(): " + exc.getMessage() );
            }

            // Chk
            if ( file == null ) {
                final String msg = "ManualActivity.fromJSON(): invalid or missing file.";

                Log.e( LogTag, msg );
                throw new JSONException( msg );
            }

            return new MediaActivity( id, file );
        }

        @Override
        public MediaActivity copy(Id id)
        {
            MediaActivity toret = new MediaActivity( id, this.getFile() );

            this.copyBasicAttributesTo( toret );
            return toret;
        }

        /** @return the time in seconds for the duration of this video.
          * @param orm The ORM object (in order to qualify the file).
          * @param mact The media activity,
          * @return the time of the video, in seconds.
          * @see Orm
          */
        public static int calculateVideoDuration(Orm orm, MediaActivity mact)
        {
            final MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            final File mediaFile = new File(
                    orm.buildMediaDirectoryFor( mact.getExperimentOwner() ),
                    mact.getFile().getName() );
            int toret = 5;

            try {
                retriever.setDataSource( mediaFile.getPath() );
                String strTime = retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_DURATION );
                toret = Integer.parseInt(strTime) / 1000;
            } catch(IllegalArgumentException exc) {
                Log.e( LogTag, "unable to calculate video length for' "
                                + mediaFile.getPath()
                                + "' invalid path: " + exc.getMessage() );
            }

            retriever.release();
            return toret;
        }

        private File file;
    }

    /** Creates a new group with an empty list. */
    protected MediaGroup(Id id, Format fmt, Tag tag, Experiment expr)
    {
        this( id, fmt, tag, false, expr, new MediaActivity[] {} );
    }

    /** Creates a new group filled with various files. */
    protected MediaGroup(Id id, Format fmt, Tag tag, Experiment expr, MediaActivity[] files)
    {
        this( id, fmt, tag, false, expr, files );
    }

    /** Creates a new group filled with various macts. */
    protected MediaGroup(Id id, Format fmt, Tag tag, boolean rnd, Experiment expr, MediaActivity[] macts)
    {
        super( id, rnd, expr, macts );

        this.tag = tag;
        this.format = fmt;
    }

    /** @return The format of this media type. */
    public Format getFormat()
    {
        return this.format;
    }

    /** Sets the tag to be the given value. */
    public void setTag(Tag tag)
    {
        this.tag = tag;
    }

    /** @return The tag. */
    public Tag getTag() {
        return tag;
    }

    @Override
    public int hashCode()
    {
        int toret = super.hashCode();

        toret += 31 * this.getTag().hashCode();
        toret += 37 * this.getFormat().hashCode();

        return toret;
    }

    @Override
    public boolean equals(Object o)
    {
        boolean toret = false;

        if ( o instanceof MediaGroup ) {
            final MediaGroup mgo = (MediaGroup) o;

            toret = super.equals( o )
                 && this.getTag().equals( mgo.getTag() )
                 && this.getFormat().equals( mgo.getFormat() );
        }

        return toret;
    }

    /** Copies the activities in this group.
     * @return a new vector with copied activities.
     */
    public MediaActivity[] copyActivities()
    {
        final Activity[] acts = super.copyActivities();

        return Arrays.copyOf( acts, acts.length, MediaActivity[].class );
    }

    @Override
    public MediaActivity[] get()
    {
        final Activity[] toret = super.get();

        return Arrays.copyOf( toret, toret.length, MediaActivity[].class );
    }

    @Override
    public String toString()
    {
        final Object[] acts = this.get();
        String toret = this.getTag().toString() + " - ";

        if ( acts.length == 1 ) {
            toret += acts[ 0 ].toString();
        } else {
            toret += super.toString();
        }

        return toret;
    }

    @Override
    public File[] enumerateMediaFiles()
    {
        final MediaActivity[] activities = this.get();
        final int size = activities.length;
        final ArrayList<File> toret = new ArrayList<>( size );

        for(int i = 0; i < size; ++i) {
            toret.addAll( Arrays.asList( activities[ i ].enumerateMediaFiles() ) );
        }

        return toret.toArray( new File[ 0 ] );
    }

    @Override
    public void writeToJSON(JsonWriter jsonWriter) throws IOException
    {
        super.writeToJSON( jsonWriter );

        jsonWriter.name( Tag.FIELD ).value( this.getTag().toString() );
    }

    private Tag tag;
    private Format format;
}

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
    final static String LOG_TAG = "MediaGroup";
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
                final MediaActivity MA_OBJ = (MediaActivity) o;

                toret = this.getTag().equals( MA_OBJ.getTag() );
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
                    final String TOKEN = jsonReader.nextName();

                    if ( TOKEN.equals( Orm.FIELD_FILE ) ) {
                        file = new File( jsonReader.nextString() );
                    }
                }
            } catch(IOException exc)
            {
                Log.e(LOG_TAG, "ManualActivity.fromJSON(): " + exc.getMessage() );
            }

            // Chk
            if ( file == null ) {
                final String MSG_ERROR = "ManualActivity.fromJSON(): invalid or missing file.";

                Log.e(LOG_TAG, MSG_ERROR );
                throw new JSONException( MSG_ERROR );
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
            final MediaMetadataRetriever RETRIEVER = new MediaMetadataRetriever();
            final File MEDIA_FILE = new File(
                    orm.buildMediaDirectoryFor( mact.getExperimentOwner() ),
                    mact.getFile().getName() );
            int toret = 5;

            try {
                RETRIEVER.setDataSource( MEDIA_FILE.getPath() );
                String strTime = RETRIEVER.extractMetadata( MediaMetadataRetriever.METADATA_KEY_DURATION );
                toret = Integer.parseInt(strTime) / 1000;
            } catch(IllegalArgumentException exc) {
                Log.e(LOG_TAG, "unable to calculate video length for' "
                                + MEDIA_FILE.getPath()
                                + "' invalid path: " + exc.getMessage() );
            }

            RETRIEVER.release();
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

        this.tag = tag.copy();
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
        this.tag = tag.copy();
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
            final MediaGroup MG_OBJ = (MediaGroup) o;

            toret = super.equals( o )
                 && this.getTag().equals( MG_OBJ.getTag() )
                 && this.getFormat().equals( MG_OBJ.getFormat() );
        }

        return toret;
    }

    /** Copies the activities in this group.
     * @return a new vector with copied activities.
     */
    public MediaActivity[] copyActivities()
    {
        final Activity[] ACTS = super.copyActivities();

        return Arrays.copyOf( ACTS, ACTS.length, MediaActivity[].class );
    }

    @Override
    public MediaActivity[] get()
    {
        final Activity[] TORET = super.get();

        return Arrays.copyOf( TORET, TORET.length, MediaActivity[].class );
    }

    @Override
    public String toString()
    {
        final Object[] ACTS = this.get();
        String toret = this.getTag().toString() + " - ";

        if ( ACTS.length == 1 ) {
            toret += ACTS[ 0 ].toString();
        } else {
            toret += super.toString();
        }

        return toret;
    }

    @Override
    public File[] enumerateMediaFiles()
    {
        final MediaActivity[] ACTIVITIES = this.get();
        final int SIZE = ACTIVITIES.length;
        final ArrayList<File> TORET = new ArrayList<>( SIZE );

        for(int i = 0; i < SIZE; ++i) {
            TORET.addAll( Arrays.asList( ACTIVITIES[ i ].enumerateMediaFiles() ) );
        }

        return TORET.toArray( new File[ 0 ] );
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

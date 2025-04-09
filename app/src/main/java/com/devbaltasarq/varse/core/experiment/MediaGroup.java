// VARSE 2019/23 (c) Baltasar for MILEGroup MIT License <baltasarq@uvigo.es>


package com.devbaltasarq.varse.core.experiment;


import android.media.MediaMetadataRetriever;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Ofm;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/** Represents a group of various media files. */
public abstract class MediaGroup extends Group<MediaGroup.MediaActivity> {
    final static String LOG_TAG = "MediaGroup";
    public enum Format { Picture, Video }

    public static class MediaActivity extends Group.Activity {
        public MediaActivity(Id id, File f)
        {
            super( id, new Tag( f.getName() ) );

            this.file = new File( Ofm.get().getFileManager().
                                    buildMediaFileNameForDbFromMediaFileName( f.getName() ) );
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

            if ( o instanceof MediaActivity MA_OBJ) {
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
                toret = new Duration( calculateVideoDuration( Ofm.get(), this ) );
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
        public List<File> enumerateMediaFiles()
        {
            final List<File> TORET = new ArrayList<>( 1 );

            TORET.add( this.getFile() );
            return TORET;
        }

        /** Writes the common properties of a media activity to JSON.
         * @param jsonWriter The JSON writer to write to.
         * @throws IOException throws it when there are problems with the stream.
         */
        @Override
        public void writeToJSON(JsonWriter jsonWriter) throws IOException
        {
            this.writeIdToJSON( jsonWriter );
            jsonWriter.name( Ofm.FIELD_FILE ).value( this.getFile().getName() );
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

                    if ( TOKEN.equals( Ofm.FIELD_FILE ) ) {
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

        /** Returns the time in seconds for the duration of this video.
          * @param ofm The ORM object (in order to qualify the file).
          * @param mact The media activity,
          * @return the time of the video, in seconds.
          * @see Ofm
          */
        public static int calculateVideoDuration(Ofm ofm, MediaActivity mact)
        {
            int toret = 0;
            try (MediaMetadataRetriever RETRIEVER = new MediaMetadataRetriever()) {
                final File MEDIA_FILE = new File(
                        Ofm.get().getFileManager().buildMediaDirectoryFor( mact.getExperimentOwner() ),
                        mact.getFile().getName());
                toret = 5;

                try {
                    RETRIEVER.setDataSource( MEDIA_FILE.getPath() );
                    String strTime = RETRIEVER.extractMetadata( MediaMetadataRetriever.METADATA_KEY_DURATION );

                    if ( strTime != null ) {
                        toret = Integer.parseInt( strTime ) / 1000;
                    }
                } catch (IllegalArgumentException exc) {
                    Log.e(LOG_TAG, "unable to calculate video length for' "
                            + MEDIA_FILE.getPath()
                            + "' invalid path (?): " + exc.getMessage() );
                }
            } catch(IOException exc) {
                Log.e( LOG_TAG, "calculateVideoDuration.MediaMetadataRetriever error: "
                                    + exc.getMessage() );
            }

            return toret;
        }

        private final File file;
    }

    /** Creates a new group with an empty list. */
    protected MediaGroup(Id id, Format fmt, Tag tag, Experiment expr)
    {
        this( id, fmt, tag, false, expr, new ArrayList<>() );
    }

    /** Creates a new group filled with various files. */
    protected MediaGroup(Id id, Format fmt, Tag tag, Experiment expr, List<MediaActivity> files)
    {
        this( id, fmt, tag, false, expr, files );
    }

    /** Creates a new group filled with various macts. */
    protected MediaGroup(Id id, Format fmt, Tag tag, boolean rnd, Experiment expr, List<MediaActivity> macts)
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

        if ( o instanceof MediaGroup MG_OBJ) {
            toret = super.equals( o )
                 && this.getTag().equals( MG_OBJ.getTag() )
                 && this.getFormat().equals( MG_OBJ.getFormat() );
        }

        return toret;
    }

    @Override
    public String toString()
    {
        final List<MediaActivity> ACTS = this.get();
        String toret = this.getTag().toString() + " - ";

        if ( ACTS.size() == 1 ) {
            toret += ACTS.get( 0 ).toString();
        } else {
            toret += super.toString();
        }

        return toret;
    }

    @Override
    public List<File> enumerateMediaFiles()
    {
        final List<File> TORET = new ArrayList<>( this.getNumActivities() );

        for (MediaActivity activity: this.get()) {
            TORET.addAll( activity.enumerateMediaFiles() );
        }

        return TORET;
    }

    @Override
    public void writeToJSON(JsonWriter jsonWriter) throws IOException
    {
        super.writeToJSON( jsonWriter );

        jsonWriter.name( Tag.FIELD ).value( this.getTag().toString() );
    }

    /** This is needed to ensure that the activities loaded for example from JSON
      * are actually MediaActivity's.
      * @param acts The activities, as a list of Activity instance.
      * @return A list of MediaActivity instances.
      */
    public static List<MediaActivity> MediaActListFromActList(List<Activity> acts)
    {
        final List<MediaActivity> TORET = new ArrayList<>( acts.size() );

        for(Activity act: acts) {
            TORET.add( (MediaActivity) act );
        }

        return TORET;
    }

    private Tag tag;
    private final Format format;
}

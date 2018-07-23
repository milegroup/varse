package com.devbaltasarq.varse.core.experiment;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Orm;

import org.json.JSONException;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;


/** Represents a group made of manual activities. */
public class ManualGroup extends Group<ManualGroup.ManualActivity> {
    /** Represents a manual activity, running for a given time. */
    public static class ManualActivity extends Group.Activity {
        public static String LogTag = "ManualActivity";

        public ManualActivity()
        {
            this( Id.createFake(), DEFAULT_TAG_FOR_ACTIVITY, DEFAULT_TIME_FOR_ACTIVITY );
        }

        public ManualActivity(Id id, Tag tag, Duration time)
        {
            super( id, tag );

            this.time = time;
        }

        @Override
        public TypeId getTypeId()
        {
            return TypeId.ManualActivity;
        }

        @Override
        public int hashCode()
        {
            return ( 11 * this.getTag().hashCode() ) + ( 13 * this.getTime().hashCode() );
        }

        @Override
        public boolean equals(Object o)
        {
            boolean toret = false;

            if ( o instanceof ManualActivity ) {
                final ManualActivity mao = (ManualActivity) o;

                toret = this.getTime().equals( mao.getTime() )
                     && this.getTag().equals( mao.getTag() );
            }

            return toret;
        }

        public Duration getTime()
        {
            return time;
        }

        public void setTime(Duration time)
        {
            this.time = time;
        }

        @Override
        public void writeToJSON(JsonWriter jsonWriter) throws IOException
        {
            super.writeToJSON( jsonWriter );
            jsonWriter.name( Orm.FIELD_TIME ).value( this.getTime().getTimeInSeconds() );
        }

        /** Creates a new manual activity, from JSON data.
         * @param id The id of the future activity.
         * @param jsonReader The reader from which to extract data.
         * @throws JSONException if data is not found or invalid.
         */
        public static ManualActivity fromJSON(Id id, JsonReader jsonReader) throws JSONException
        {
            Tag tag = null;
            Duration duration = null;

            // Load data
            try {
                while ( jsonReader.hasNext() ) {
                    final String token = jsonReader.nextName();

                    if ( token.equals( Orm.FIELD_TAG ) ) {
                        tag = new Tag( jsonReader.nextString() );
                    }
                    else
                    if ( token.equals( Orm.FIELD_TIME ) ) {
                        duration = new Duration( jsonReader.nextInt() );
                    }
                }
            } catch(IOException exc)
            {
                Log.e( LogTag, "ManualActivity.fromJSON(): " + exc.getMessage() );
            }

            // Chk
            if ( tag == null
              || duration == null )
            {
                final String msg = "ManualActivity.fromJSON(): invalid or missing data.";

                Log.e( LogTag, msg );
                throw new JSONException( msg );
            }

            return new ManualActivity( id, tag, duration );
        }

        @Override
        public ManualActivity copy(Id id)
        {
            return new ManualActivity( id, this.getTag().copy(), this.getTime().copy() );
        }

        @Override
        public String toString()
        {
            return this.getTag() + " (" + this.getTime() + ")";
        }

        private Duration time;
    }

    /** Creates a new manual group.
     * @param id The id for this group.
     */
    public ManualGroup(Id id, Experiment expr)
    {
        this( id, false, expr, new ManualActivity[] {} );
    }

    /** Creates a new manual group.
     * @param id The id for this group.
     * @param rnd Whether this group is random or not.
     */
    public ManualGroup(Id id, boolean rnd, Experiment expr)
    {
        this( id, rnd, expr, new ManualActivity[] {} );
    }

    /** Creates a new manual group.
     * @param id The id for this group.
     * @param rnd Whether this group is random or not.
     * @param acts The activities.
     */
    public ManualGroup(Id id, boolean rnd, Experiment expr, ManualActivity[] acts)
    {
        super( id, rnd, expr, acts );
    }

    @Override
    public TypeId getTypeId()
    {
        return TypeId.ManualGroup;
    }

    @Override
    public ManualGroup copy(Id id)
    {
        return new ManualGroup( id, this.isRandom(), null, (ManualActivity[]) this.copyActivities() );
    }

    /** Copies the activities in this group.
     * @return a new vector with copied activities.
     */
    public ManualActivity[] copyActivities()
    {
        final Activity[] acts = super.copyActivities();

        return Arrays.copyOf( acts, acts.length, ManualActivity[].class );
    }

    @Override
    public ManualActivity[] get()
    {
        final Activity[] toret = super.get();

        return Arrays.copyOf( toret, toret.length, ManualActivity[].class );
    }
}

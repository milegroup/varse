// VARSE 2019/23 (c) Baltasar for MILEGroup MIT License <baltasarq@uvigo.es>


package com.devbaltasarq.varse.core.experiment;


import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Ofm;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/** Represents a group made of manual activities. */
public class ManualGroup extends Group<ManualGroup.ManualActivity> {
    public static final String LOG_TAG = ManualGroup.class.getSimpleName();

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

            this.time = time.copy();
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
                final ManualActivity MACT_OBJ = (ManualActivity) o;

                toret = this.getTime().equals( MACT_OBJ.getTime() )
                     && this.getTag().equals( MACT_OBJ.getTag() );
            }

            return toret;
        }

        @Override
        public Duration getTime()
        {
            return time;
        }

        public void setTime(Duration time)
        {
            this.time = time.copy();
        }

        @Override
        public void writeToJSON(JsonWriter jsonWriter) throws IOException
        {
            super.writeToJSON( jsonWriter );
            jsonWriter.name( Ofm.FIELD_TIME ).value( this.getTime().getTimeInSeconds() );
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
                    final String TOKEN = jsonReader.nextName();

                    if ( TOKEN.equals( Ofm.FIELD_TAG ) ) {
                        tag = new Tag( jsonReader.nextString() );
                    }
                    else
                    if ( TOKEN.equals( Ofm.FIELD_TIME ) ) {
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
                final String MSG_ERROR = "ManualActivity.fromJSON(): invalid or missing data.";

                Log.e( LogTag, MSG_ERROR );
                throw new JSONException( MSG_ERROR );
            }

            return new ManualActivity( id, tag, duration );
        }

        @Override
        public ManualActivity copy(Id id)
        {
            ManualActivity toret = new ManualActivity( id, this.getTag().copy(), this.getTime().copy() );

            this.copyBasicAttributesTo( toret );
            return toret;
        }

        @Override
        public String toString()
        {
            return this.getTag().getHumanReadable() + " (" + this.getTime() + ")";
        }

        private Duration time;
    }

    /** Creates a new manual group.
     * @param id The id for this group.
     */
    public ManualGroup(Id id, Experiment expr)
    {
        this( id, false, expr, new ArrayList<>() );
    }

    /** Creates a new manual group.
     * @param id The id for this group.
     * @param rnd Whether this group is random or not.
     */
    public ManualGroup(Id id, boolean rnd, Experiment expr)
    {
        this( id, rnd, expr, new ArrayList<>() );
    }

    /** Creates a new manual group.
     * @param id The id for this group.
     * @param rnd Whether this group is random or not.
     * @param acts The activities.
     */
    public ManualGroup(Id id, boolean rnd, Experiment expr, List<ManualActivity> acts)
    {
        super( id, rnd, expr, acts );
    }

    @Override
    public void setTimeForEachActivity(Duration time) throws NoSuchMethodError
    {
        final String MSG_ERROR = "manual group cannot change its time as a whole";

        Log.e(LOG_TAG, MSG_ERROR );
        throw new NoSuchMethodError( MSG_ERROR );
    }

    @Override
    public Duration getTimeForEachActivity() throws NoSuchMethodError
    {
        final String MSG_ERROR = "manual group holds activities with individual times";

        Log.e(LOG_TAG, MSG_ERROR );
        throw new NoSuchMethodError( MSG_ERROR );
    }

    @Override
    public Duration calculateTimeNeeded()
    {
        int toret = 0;

        for(ManualActivity mact: this.get()) {
            toret += mact.getTime().getTimeInSeconds();
        }

        return new Duration( toret );
    }

    @Override
    public TypeId getTypeId()
    {
        return TypeId.ManualGroup;
    }

    @Override
    protected ManualGroup copy(Id id)
    {
        return new ManualGroup( id, this.isRandom(), this.getExperimentOwner(), this.get() );
    }

    /** This is needed to ensure that the activities for example loaded from JSON
      * are actually ManualActivity's.
      * @param acts The activities, as a list of Activity instance.
      * @return A list of ManualActivity instances.
      */
    public static List<ManualActivity> ManualActListFromActList(List<Activity> acts)
    {
        final ArrayList<ManualActivity> TORET = new ArrayList<>( acts.size() );

        for(Activity act: acts) {
            TORET.add( (ManualActivity) act );
        }

        return TORET;
    }
}

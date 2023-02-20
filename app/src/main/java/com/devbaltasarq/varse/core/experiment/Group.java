// VARSE 2019/23 (c) Baltasar for MILEGroup MIT License <baltasarq@uvigo.es>


package com.devbaltasarq.varse.core.experiment;


import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Ofm;
import com.devbaltasarq.varse.core.Persistent;

import org.json.JSONException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;


/** Groups of activities, many times related to media files. */
public abstract class Group<T extends Group.Activity> extends Persistent {
    private final static String LOG_TAG = "Group";
    protected final static Duration DEFAULT_TIME_FOR_ACTIVITY = new Duration( 5 );
    protected final static Tag DEFAULT_TAG_FOR_ACTIVITY = new Tag( "tag" );

    /** Represents a given activity (ManualActivity, MediaActivity). */
    public static abstract class Activity extends Persistent {
        /** Creates a new Activity
          * The group is always null until this activity is added to a ...group.
          * @param id The id for the activity.
          * @param tag The tag for this activity.
          * @see Group
          */
        protected Activity(Id id, Tag tag)
        {
            super( id );

            this.group = null;
            this.tag = tag.copy();
        }

        /** @return The group this activity pertains to. */
        public Group<? extends Group.Activity> getGroup()
        {
            return this.group;
        }

        /** @return The tag for this activity. */
        public Tag getTag()
        {
            return tag;
        }

        /** Change the tag for this activity.
          * @param tag The tag, as a Tag object.
          * @see Tag
          */
        public void setTag(Tag tag)
        {
            this.tag = tag.copy();
        }

        /** @return the time needed for this activity to complete. */
        public abstract Duration getTime();

        @Override
        public Experiment getExperimentOwner()
        {
            return this.group.getExperimentOwner();
        }

        /** Writes the common properties of an Activity to JSON.
         * @param jsonWriter The JSON writer to write to.
         * @throws IOException throws it when there are problems with the stream.
         */
        @Override
        public void writeToJSON(JsonWriter jsonWriter) throws IOException
        {
            this.writeIdToJSON( jsonWriter );
            jsonWriter.name( Ofm.FIELD_TAG ).value( this.getTag().toString() );
        }

        public static Activity fromJSON(JsonReader jsonReader) throws JSONException
        {
            TypeId typeId = null;
            Id id = null;
            Activity toret;

            try {
                jsonReader.beginObject();
                while ( ( id == null
                       || typeId == null )
                     && jsonReader.hasNext() )
                {
                    final String NEXT_NAME = jsonReader.nextName();

                    if ( NEXT_NAME.equals( Id.FIELD) ) {
                        id = readIdFromJSON( jsonReader );
                    }
                    else
                    if ( NEXT_NAME.equals( Ofm.FIELD_TYPE_ID ) ) {
                        typeId = readTypeIdFromJson( jsonReader );
                    }
                }

                // Chk
                if ( id == null
                  || typeId == null )
                {
                    throw new JSONException( "Activity.fromJSON: id or type id was not found" );
                }

                // Read the remaining data
                switch ( typeId ) {
                    case ManualActivity:
                        toret = ManualGroup.ManualActivity.fromJSON( id, jsonReader );
                        break;
                    case MediaActivity:
                        toret = MediaGroup.MediaActivity.fromJSON( id, jsonReader );
                        break;
                    default:
                        throw new JSONException( "Activity.fromJSON: unrecognized type id" );
                }

                jsonReader.endObject();
            } catch(IOException exc)
            {
                throw new JSONException( "Activity.fromJSON: " + exc.getMessage() );
            }

            return toret;
        }

        public Activity copy()
        {
            return this.copy( this.getId().copy() );
        }

        protected abstract Activity copy(Id id);

        protected void copyBasicAttributesTo(Activity dest)
        {
            dest.setTag( this.getTag() );
            dest.group = this.getGroup();
        }

        private Tag tag;
        Group<? extends Activity> group;
    }

    /** Creates a new group. given a tag and a random display of terms. */
    protected Group(Id id, boolean rnd, Experiment expr)
    {
        this( id, rnd, expr, new ArrayList<>() );
    }

    /** Creates a new group. given a tag and a random display of terms. */
    protected Group(Id id, boolean rnd, Experiment expr, List<T> acts)
    {
        super( id );

        this.random = rnd;
        this.activities = new ArrayList<>( acts );
        this.experiment = expr;
    }

    @Override
    public void updateIds()
    {
        super.updateIds();

        for(Activity act: this.activities) {
            act.updateIds();
        }

        return;
    }

    /** @return the time needed for each activity. */
    public Duration getTimeForEachActivity()
    {
        return this.timeForEachActivity;
    }

    /** Changes the time for each activity in this group.
      * @param time The new time to spend in this activity.
      * @see Duration
      */
    public void setTimeForEachActivity(Duration time)
    {
        this.timeForEachActivity = time.copy();
    }

    /** Removes the given activity.
     * @param act The activity.
     */
    public void remove(Activity act)
    {
        this.activities.remove( act );
    }

    /** Appends the given activity.
     * @param act The activity.
     */
    public void add(T act)
    {
        act.group = this;
        this.activities.add( act );
    }

    /** Substitutes a given activity.
      * @param act The activity to substitute.
      */
    public void substituteActivity(T act)
    {
        for(int i = 0; i < this.activities.size(); ++i) {
            if ( this.activities.get( i ).getId().equals( act.getId() ) ) {
                this.activities.set( i, act );
            }
        }

        act.group = this;
    }

    /** @return all activities in this group. */
    public List<T> get()
    {
        return new ArrayList<>( this.activities );
    }

    /** @return the number of activities in this group. */
    public int getNumActivities()
    {
        return this.activities.size();
    }

    public void replaceActivities(T[] acts)
    {
        this.activities.clear();

        for(T act: acts) {
            this.add( act );
        }

        return;
    }

    /** @return the number of activities inside this group. */
    public int size()
    {
        return this.activities.size();
    }

    /** Remove all stored ativities. */
    public void clear()
    {
        this.activities.clear();
    }

    /** Swaps a given media file with the previous one.
     * @param act the activity to swap.
     */
    public void sortActivityUp(Activity act)
    {
        final int POS = this.activities.indexOf( act );

        // Nothing to do if not found or is the first one.
        if ( POS >= 1 ) {
            T backUp = this.activities.get( POS - 1 );
            this.activities.set( POS - 1, this.activities.get( POS ) );
            this.activities.set( POS, backUp );
        }

        return;
    }

    /** Swaps a given media file with the previous one.
      * @param act the activity to swap.
      */
    public void sortActivityDown(Activity act)
    {
        final int LENGTH = this.activities.size();
        final int POS = this.activities.indexOf( act );

        // Nothing to do if not found or is the last one.
        if ( POS < ( LENGTH - 1 ) ) {
            T backUp = this.activities.get( POS + 1 );
            this.activities.set( POS + 1, this.activities.get( POS ) );
            this.activities.set( POS, backUp );
        }

        return;
    }

    @Override
    public int hashCode()
    {
        int toret = 19 * Boolean.valueOf( this.isRandom() ).hashCode();

        int actsHashCode = 0;
        for(int i = 0; i < this.activities.size(); ++i) {
            actsHashCode += this.activities.get( i ).hashCode();
        }

        return toret + ( 29 * actsHashCode );
    }

    @Override
    public boolean equals(Object o)
    {
        boolean toret = false;

        if ( o instanceof Group<?> ) {
            final Group<? extends Activity> OTHER_GROUP = (Group<?>) o;
            final List<? extends Activity> OTHER_ACTIVITIES = OTHER_GROUP.get();
            final int THIS_ACTIVITIES_SIZE = this.getNumActivities();
            final int OTHER_ACTIVITIES_SIZE = OTHER_GROUP.getNumActivities();

            if ( THIS_ACTIVITIES_SIZE == OTHER_ACTIVITIES_SIZE ) {
                int i = 0;
                for(; i < this.activities.size(); ++i) {
                    if ( !this.activities.get( i ).equals( OTHER_ACTIVITIES.get( i ) ) )
                    {
                        break;
                    }
                }

                toret = ( ( i >= OTHER_ACTIVITIES_SIZE )
                       && ( this.isRandom() == OTHER_GROUP.isRandom() ) );
            }
        }

        return toret;
    }

    /** @return Whether the media in this group will be shown randomly. */
    public boolean isRandom()
    {
        return random;
    }

    /** Sets whether or not the entries in this group will be shown randomly. */
    public void setRandom(boolean random) {
        this.random = random;
    }

    /** @return the experiment this group pertains to. */
    @Override
    public Experiment getExperimentOwner()
    {
        return this.experiment;
    }

    /** Sets the experiment this group pertains to. */
    public void setExperiment(Experiment expr)
    {
        this.experiment = expr;
    }

    @Override
    public String toString()
    {
        return this.activities.size() + " item(s)";
    }

    /** Writes the common properties of a group to JSON
     * @param jsonWriter The JSON stream to write to.
     * @throws IOException When things go bad with the stream.
     */
    @Override
    public void writeToJSON(JsonWriter jsonWriter) throws IOException
    {
        this.writeIdToJSON( jsonWriter );
        jsonWriter.name( Ofm.FIELD_RANDOM ).value( this.isRandom() );

        jsonWriter.name( Ofm.FIELD_ACTIVITIES ).beginArray();
        for(Activity act: this.activities) {
            jsonWriter.beginObject();
            act.writeToJSON( jsonWriter );
            jsonWriter.endObject();
        }
        jsonWriter.endArray();
    }

    /** @return the number of seconds that this experiment will take.
     *  @see Duration
     */
    public Duration calculateTimeNeeded()
    {
        return new Duration( this.activities.size()
                              * this.getTimeForEachActivity().getTimeInSeconds() );
    }

    public static Group<? extends Activity> fromJSON(Reader rd) throws JSONException
    {
        final JsonReader JSON_READER = new JsonReader( rd );

        return fromJSON( JSON_READER );
    }

    public static Group<? extends Activity> fromJSON(JsonReader jsonReader) throws JSONException
    {
        final ArrayList<Activity> ACTS = new ArrayList<>();
        final Group<? extends Activity> TORET;
        TypeId typeId = null;
        Tag tag = null;
        Duration duration = null;
        boolean rnd = false;
        Id id = null;

        // Load data
        try {
            jsonReader.beginObject();
            while ( jsonReader.hasNext() ) {
                final String NEXT_NAME = jsonReader.nextName();

                if ( NEXT_NAME.equals( Ofm.FIELD_RANDOM ) ) {
                    rnd = jsonReader.nextBoolean();
                }
                else
                if ( NEXT_NAME.equals( Ofm.FIELD_TYPE_ID ) ) {
                    typeId = readTypeIdFromJson( jsonReader );
                }
                else
                if ( NEXT_NAME.equals( Ofm.FIELD_TAG ) ) {
                    tag = new Tag( jsonReader.nextString() );
                }
                else
                if ( NEXT_NAME.equals( Ofm.FIELD_TIME ) ) {
                    duration = new Duration( jsonReader.nextInt() );
                }
                else
                if ( NEXT_NAME.equals( Id.FIELD) ) {
                    id = readIdFromJSON( jsonReader );
                }
                else
                if ( NEXT_NAME.equals( Ofm.FIELD_ACTIVITIES ) ) {
                    jsonReader.beginArray();
                    while ( jsonReader.hasNext() ) {
                        ACTS.add( Activity.fromJSON( jsonReader ) );
                    }
                    jsonReader.endArray();
                }
            }

            jsonReader.endObject();
        } catch(IOException exc)
        {
            Log.e( LOG_TAG, "Creating group from JSON: " + exc.getMessage() );
        }

        // Chk
        if ( typeId == null
          || id == null )
        {
            final String MSG_ERROR = "Creating user from JSON: invalid or missing data.";

            Log.e(LOG_TAG, MSG_ERROR );
            throw new JSONException( MSG_ERROR );
        }

        try {
            if ( typeId == TypeId.PictureGroup
              || typeId == TypeId.VideoGroup )
            {
                if ( typeId == TypeId.PictureGroup ) {
                    if ( tag == null ) {
                        throw new JSONException( "Group.fromJSON: picture group missing tag" );
                    }

                    if ( duration == null ) {
                        throw new JSONException( "Group.fromJSON: picture group missing time" );
                    }

                    TORET = new PictureGroup( id, tag, duration, rnd, null,
                                              MediaGroup.MediaActListFromActList( ACTS ) );
                }
                else
                if ( typeId == TypeId.VideoGroup ) {
                    if ( tag == null ) {
                        throw new JSONException( "Group.fromJSON: video group missing tag" );
                    }


                    TORET = new VideoGroup( id, tag, rnd, null,
                                            MediaGroup.MediaActListFromActList( ACTS ) );
                } else {
                    throw new JSONException( "unknown media group typeId: " + typeId.toString() );
                }
            }
            else
            if ( typeId == TypeId.ManualGroup ) {
                TORET = new ManualGroup( id, rnd, null,
                                         ManualGroup.ManualActListFromActList( ACTS ) );
            } else {
                throw new JSONException( "unable to create appropriate group for: " + typeId );
            }
        } catch(ClassCastException exc)
        {
            throw new JSONException( "Group.fromJSON(): group type and activities mismatch" );
        }

        return TORET;
    }

    /** @return a copy of this group, with the same id. */
    public Group<T> copy()
    {
        return this.copy( this.getId().copy() );
    }

    /** @return a copy of this group, with a given id.
     * @param id A new id for the copy of the group.
     * @see Id
     */
    protected abstract Group<T> copy(Id id);

    private boolean random;
    private Duration timeForEachActivity;
    private final ArrayList<T> activities;
    private Experiment experiment;
}

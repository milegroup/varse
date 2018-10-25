package com.devbaltasarq.varse.core.experiment;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.Persistent;

import org.json.JSONException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;

/** Groups of activities, many times related to media files. */
public abstract class Group<T extends Group.Activity> extends Persistent {
    private final static String LogTag = "Group";
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
        public Group getGroup()
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
            jsonWriter.name( Orm.FIELD_TAG ).value( this.getTag().toString() );
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
                    final String nextName = jsonReader.nextName();

                    if ( nextName.equals( Id.FIELD) ) {
                        id = readIdFromJSON( jsonReader );
                    }
                    else
                    if ( nextName.equals( Orm.FIELD_TYPE_ID ) ) {
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

        public abstract Activity copy(Id id);

        protected void copyBasicAttributesTo(Activity dest)
        {
            dest.setTag( this.getTag() );
            dest.group = this.getGroup();
        }

        private Tag tag;
        Group group;
    }

    /** Creates a new group. given a tag and a random display of terms. */
    @SuppressWarnings("unchecked")
    protected Group(Id id, boolean rnd, Experiment expr)
    {
        this( id, rnd, expr, (T[]) new Activity[] {} );
    }

    /** Creates a new group. given a tag and a random display of terms. */
    protected Group(Id id, boolean rnd, Experiment expr, T[] acts)
    {
        super( id );

        this.random = rnd;
        this.activities = new ArrayList<>();
        this.experiment = expr;

        for(T act: acts) {
            this.add( act );
        }

        return;
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
    public void remove(T act)
    {
        this.activities.remove( act );
    }

    /** Removes the given activity.
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
    public void substituteActivity(Activity act)
    {
        for(int i = 0; i < this.activities.size(); ++i) {
            if ( this.activities.get( i ).getId().equals( act.getId() ) ) {
                this.activities.set( i, act );
            }
        }

        act.group = this;
    }

    /** Returns all activities in this group. */
    @SuppressWarnings("unchecked")
    public T[] get()
    {
        return (T[]) this.activities.toArray( new Activity[ this.size() ] );
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

    /** @return the activities inside this group. */
    public Activity[] getActivities()
    {
        return this.activities.toArray( new Activity[ 0 ] );
    }

    /** Swaps a given media file with the previous one. */
    public void sortActivityUp(T act)
    {
        final int pos = this.activities.indexOf( act );

        // Nothing to do if not found or is the first one.
        if ( pos >= 1 ) {
            Activity backUp = this.activities.get( pos - 1 );
            this.activities.set( pos - 1, this.activities.get( pos ) );
            this.activities.set( pos, backUp );
        }

        return;
    }

    /** Swaps a given media file with the previous one. */
    public void sortActivityDown(T act)
    {
        final int length = this.activities.size();
        final int pos = this.activities.indexOf( act );

        // Nothing to do if not found or is the last one.
        if ( pos < ( length - 1 ) ) {
            Activity backUp = this.activities.get( pos + 1 );
            this.activities.set( pos + 1, this.activities.get( pos ) );
            this.activities.set( pos, backUp );
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

        if ( o instanceof Group ) {
            final Group go = (Group) o;
            final Activity[] oacts = go.get();

            if ( this.activities.size() == oacts.length ) {
                int i = 0;
                for(; i < this.activities.size(); ++i) {
                    if ( !this.activities.get( i ).equals( oacts[ i ] ) ) {
                        break;
                    }
                }

                toret = ( i >= oacts.length ) && ( this.isRandom() == go.isRandom() );
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
        jsonWriter.name( Orm.FIELD_RANDOM ).value( this.isRandom() );

        jsonWriter.name( Orm.FIELD_ACTIVITIES ).beginArray();
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

    public static Group fromJSON(Reader rd) throws JSONException
    {
        final JsonReader jsonReader = new JsonReader( rd );

        return fromJSON( jsonReader );
    }

    public static Group fromJSON(JsonReader jsonReader) throws JSONException
    {
        final ArrayList<Activity> acts = new ArrayList<>();
        Group toret;
        TypeId typeId = null;
        Tag tag = null;
        Duration duration = null;
        boolean rnd = false;
        Id id = null;

        // Load data
        try {
            jsonReader.beginObject();
            while ( jsonReader.hasNext() ) {
                final String nextName = jsonReader.nextName();

                if ( nextName.equals( Orm.FIELD_RANDOM ) ) {
                    rnd = jsonReader.nextBoolean();
                }
                else
                if ( nextName.equals( Orm.FIELD_TYPE_ID ) ) {
                    typeId = readTypeIdFromJson( jsonReader );
                }
                else
                if ( nextName.equals( Orm.FIELD_TAG ) ) {
                    tag = new Tag( jsonReader.nextString() );
                }
                else
                if ( nextName.equals( Orm.FIELD_TIME ) ) {
                    duration = new Duration( jsonReader.nextInt() );
                }
                else
                if ( nextName.equals( Id.FIELD) ) {
                    id = readIdFromJSON( jsonReader );
                }
                else
                if ( nextName.equals( Orm.FIELD_ACTIVITIES ) ) {
                    jsonReader.beginArray();
                    while ( jsonReader.hasNext() ) {
                        acts.add( Activity.fromJSON( jsonReader ) );
                    }
                    jsonReader.endArray();
                }
            }

            jsonReader.endObject();
        } catch(IOException exc)
        {
            Log.e( LogTag, "Creating group from JSON: " + exc.getMessage() );
        }

        // Chk
        if ( typeId == null
          || id == null )
        {
            final String msg = "Creating user from JSON: invalid or missing data.";

            Log.e( LogTag, msg );
            throw new JSONException( msg );
        }

        final Group.Activity[] activities = acts.toArray( new Group.Activity[ acts.size() ] );

        try {
            if ( typeId == TypeId.PictureGroup
              || typeId == TypeId.VideoGroup )
            {
                final MediaGroup.MediaActivity[] mediaActs = Arrays.copyOf(
                                                            activities, activities.length,
                                                            MediaGroup.MediaActivity[].class );

                if ( typeId == TypeId.PictureGroup ) {
                    if ( tag == null ) {
                        throw new JSONException( "Group.fromJSON: picture group missing tag" );
                    }

                    if ( duration == null ) {
                        throw new JSONException( "Group.fromJSON: picture group missing time" );
                    }

                    toret = new PictureGroup( id, tag, duration, rnd, null, mediaActs );
                }
                else
                if ( typeId == TypeId.VideoGroup ) {
                    if ( tag == null ) {
                        throw new JSONException( "Group.fromJSON: video group missing tag" );
                    }


                    toret = new VideoGroup( id, tag, rnd, null, mediaActs );
                } else {
                    throw new JSONException( "unknown media group typeId: " + typeId.toString() );
                }
            }
            else
            if ( typeId == TypeId.ManualGroup ) {
                final ManualGroup.ManualActivity[] manualActs = Arrays.copyOf(
                                                        activities, activities.length,
                                                        ManualGroup.ManualActivity[].class );

                toret = new ManualGroup( id, rnd, null, manualActs );
            } else {
                throw new JSONException( "unable to create appropriate group for: " + typeId.toString() );
            }
        } catch(ClassCastException exc)
        {
            throw new JSONException( "Group.fromJSON(): group type and activities mismatch" );
        }

        return toret;
    }

    /** @return a copy of this group, with the same id. */
    public Group copy()
    {
        return this.copy( this.getId().copy() );
    }

    /** @return a copy of this group, with a given id.
     * @param id A new id for the copy of the group.
     * @see Id
     */
    public abstract Group copy(Id id);

    /** Copies the activities in this group.
      * @return a new vector with copied activities.
      */
    public Activity[] copyActivities()
    {
        Activity[] myActivities = this.getActivities();
        Activity[] toret = new Group.Activity[ this.size() ];

        for(int i = 0; i < toret.length; ++i) {
            toret[ i ] = myActivities[ i ].copy();
        }

        return toret;
    }

    private boolean random;
    private Duration timeForEachActivity;
    private ArrayList<Activity> activities;
    private Experiment experiment;
}

package com.devbaltasarq.varse.core;


import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.ArrayList;

/** Represents the results of a given experiment. */
public class Result extends Persistent {
    public static final String LogTag = Result.class.getSimpleName();

    /** Base class for events. */
    public static abstract class Event {
        protected Event(long millis)
        {
            this.millis = millis;
        }

        public long getMillis()
        {
            return this.millis;
        }

        @Override
        public int hashCode()
        {
            return Long.valueOf( this.getMillis() ).hashCode();
        }

        @Override
        public boolean equals(Object obj2)
        {
            boolean toret = false;

            if ( obj2 instanceof Event ) {
                final Event evt = (Event) obj2;

                toret = this.getMillis() == evt.getMillis();
            }

            return toret;
        }

        public void writeToJSON(JsonWriter jsonWriter) throws IOException
        {
            jsonWriter.name( Orm.FIELD_ELAPSED_TIME )
                    .value( this.getMillis() );
        }

        protected void writeEventTypeToJSON(JsonWriter jsonWriter, String type) throws IOException
        {
            jsonWriter.name( Orm.FIELD_EVENT_TYPE ).value( type );
        }

        public static Event fromJSON(JsonReader jsonReader) throws JSONException
        {
            Event toret;
            long millis = -1L;
            long heartbeat = -1L;
            String tag = null;
            String eventType = null;

            // Load data
            try {
                jsonReader.beginObject();
                while ( jsonReader.hasNext() ) {
                    final String nextName = jsonReader.nextName();

                    if ( nextName.equals( Orm.FIELD_EVENT_TYPE ) ) {
                        eventType = jsonReader.nextString();
                    }
                    else
                    if ( nextName.equals( Orm.FIELD_ELAPSED_TIME ) ) {
                        millis = jsonReader.nextLong();
                    }
                    else
                    if ( nextName.equals( Orm.FIELD_HEART_BEAT_AT ) ) {
                        heartbeat = jsonReader.nextLong();
                    }
                    else
                    if ( nextName.equals( Orm.FIELD_TAG ) ) {
                        tag = jsonReader.nextString();
                    }
                }

                jsonReader.endObject();
            } catch(IOException exc)
            {
                final String ERROR_MSG = "Creating event from JSON: " + exc.getMessage();

                Log.e( LogTag, ERROR_MSG );
                throw new JSONException( ERROR_MSG );
            }

            // Chk
            if ( eventType == null
              || millis < 0 )
            {
                final String msg = "Creating event from JSON: invalid or missing data.";

                Log.e( LogTag, msg );
                throw new JSONException( msg );
            } else {
                if ( eventType.equals( Orm.FIELD_EVENT_ACTIVITY_CHANGE ) ) {
                    if ( tag == null ) {
                        final String msg = "Creating change activity event from JSON: invalid or missing data.";

                        Log.e( LogTag, msg );
                        throw new JSONException( msg );
                    } else {
                        toret = new ActivityChangeEvent( millis, tag );
                    }
                }
                else
                if ( eventType.equals( Orm.FIELD_EVENT_HEART_BEAT ) ) {
                    if ( heartbeat < 0 ) {
                        final String msg = "Creating new hear beat event from JSON: invalid or missing data.";

                        Log.e( LogTag, msg );
                        throw new JSONException( msg );
                    } else {
                        toret = new BeatEvent( millis, heartbeat );
                    }
                } else {
                    final String msg = "Creating event from JSON: unknown event.";

                    Log.e( LogTag, msg );
                    throw new JSONException( msg );
                }
            }

            return toret;
        }

        private long millis;
    }

    /** Event: a heart beat. */
    public static class BeatEvent extends Event {
        public BeatEvent(long millis, long timeOfNewHeartBeat)
        {
            super( millis );
            this.timeOfNewHeartBeat = timeOfNewHeartBeat;
        }

        public long getTimeOfNewHeartBeat()
        {
            return this.timeOfNewHeartBeat;
        }

        @Override
        public boolean equals(Object obj2)
        {
            boolean toret = false;

            if ( super.equals( obj2 )
              && obj2 instanceof BeatEvent )
            {
                final BeatEvent evt = (BeatEvent) obj2;

                toret = this.getTimeOfNewHeartBeat() == evt.getTimeOfNewHeartBeat();
            }

            return toret;
        }

        private void writeEventTypeToJSON(JsonWriter jsonWriter) throws IOException
        {
            super.writeEventTypeToJSON( jsonWriter, Orm.FIELD_EVENT_HEART_BEAT );
        }

        @Override
        public void writeToJSON(JsonWriter jsonWriter) throws IOException
        {
            jsonWriter.beginObject();

            super.writeToJSON( jsonWriter );

            this.writeEventTypeToJSON( jsonWriter );
            jsonWriter.name( Orm.FIELD_HEART_BEAT_AT)
                    .value( Long.toString( this.getTimeOfNewHeartBeat() ) );

            jsonWriter.endObject();
        }

        private long timeOfNewHeartBeat;
    }

    /** Event: new activity. */
    public static class ActivityChangeEvent extends Event {
        public ActivityChangeEvent(long millis, String tag)
        {
            super( millis );
            this.tag = tag;
        }

        public String getTag()
        {
            return this.tag;
        }

        @Override
        public boolean equals(Object obj2)
        {
            boolean toret = false;

            if ( super.equals( obj2 )
              && obj2 instanceof ActivityChangeEvent )
            {
                final ActivityChangeEvent evt = (ActivityChangeEvent) obj2;

                toret = this.getTag().equals( evt.getTag() );
            }

            return toret;
        }

        private void writeEventTypeToJSON(JsonWriter jsonWriter) throws IOException
        {
            super.writeEventTypeToJSON( jsonWriter, Orm.FIELD_EVENT_ACTIVITY_CHANGE );
        }

        @Override
        public void writeToJSON(JsonWriter jsonWriter) throws IOException
        {
            jsonWriter.beginObject();

            super.writeToJSON( jsonWriter );

            this.writeEventTypeToJSON( jsonWriter );
            jsonWriter.name( Orm.FIELD_TAG )
                    .value( this.getTag() );

            jsonWriter.endObject();
        }

        private String tag;
    }

    public Result(Id id, long dateTime, User usr, Experiment expr)
    {
        super( id );
        this.dateTime = dateTime;
        this.user = usr;
        this.experiment = expr;
        this.events = new ArrayList<>();
    }

    @Override
    public TypeId getTypeId()
    {
        return TypeId.Result;
    }

    /** @return the date for this results. */
    public long getTime()
    {
        return this.dateTime;
    }

    /** @return the user the results belong to. */
    public User getUser()
    {
        return this.user;
    }

    /** @return the id of the experiment the results belong to. */
    public Experiment getExperiment()
    {
        return this.experiment;
    }

    @Override
    public Experiment getExperimentOwner()
    {
        return this.getExperiment();
    }

    @Override
    public int hashCode()
    {
        return ( 17 * this.getId().hashCode() )
                + ( 27 * this.getUser().hashCode() )
                + ( 37 * this.getExperiment().hashCode() );
    }

    @Override
    public boolean equals(Object o)
    {
        boolean toret = false;

        if ( o instanceof Result ) {
            Result ro = (Result) o;

            if ( this.getUser().equals( ro.getUser() )
              && this.getExperiment().equals( ro.getExperiment() ) )
            {
                toret = true;
            }
        }

        return toret;
    }

    @Override
    public File[] enumerateAssociatedFiles()
    {
        return new File[ 0 ];
    }

    /** Add a new event.
      * @param evt the new event.
      * @see Event
      */
    public void add(Event evt)
    {
        this.events.add( evt );
    }

    /** Adds a new list of events. */
    public void addAll(Collection<Event> v)
    {
        this.events.addAll( v );
    }

    /** Removes all stored events. */
    public void clearEvents()
    {
        this.events.clear();
    }

    /** @return all events in this result. Warning: the list can be huge. */
    public Event[] buildEventsList()
    {
        return this.events.toArray( new Event[ 0 ] );
    }

    /** @return a list with all activity changes. */
    public ActivityChangeEvent[] buildActivityChangesList()
    {
        ActivityChangeEvent[] toret = new ActivityChangeEvent[ this.experiment.getNumActivities() ];
        int i = 0;

        for(Event evt: this.events) {
            if ( evt instanceof ActivityChangeEvent ) {
                toret[ i ] = (ActivityChangeEvent) evt;
                ++i;
            }
        }

        return toret;
    }

    /** @return the heart beats time distance, as a long[]. */
    public long[] buildHeartBeatsList()
    {
        final ArrayList<Long> beatDistanceTimes = new ArrayList<>( this.size() );

        // Create
        for(Event evt: this.events) {
            if ( evt instanceof BeatEvent ) {
                beatDistanceTimes.add( ( (BeatEvent) evt ).getTimeOfNewHeartBeat() );
            }
        }

        // Convert to primitive (come on, Java...).
        long[] toret = new long[ beatDistanceTimes.size() ];
        int pos = 0;
        for(long l: beatDistanceTimes) {
            toret[ pos ] = beatDistanceTimes.get( pos );
            ++pos;
        }

        beatDistanceTimes.clear();
        return toret;
    }

    /** Creates the standard pair of text files, one for heatbeats,
      * and another one to know when the activity changed.
      */
    public void exportToStdTextFormat(Writer tagsStream, Writer beatsStream)
                                                        throws IOException
    {
        long timeLastActivity = 0;

        tagsStream.write( "Init_time\tTag\tDurat" );

        // Run all over the events and scatter them on files
        for(Event evt: this.events) {
            if ( evt instanceof BeatEvent ) {
                beatsStream.write( Long.toString( ( (BeatEvent) evt ).getTimeOfNewHeartBeat() ) );
                beatsStream.write( '\n' );
            } else {
                final ActivityChangeEvent actEvt = ( (ActivityChangeEvent) evt );
                final long time = evt.getMillis();
                final long timeSinceLastAct = time - timeLastActivity;

                // Experiment's elapsed time
                final double totalSecs = ( (double) time ) / 1000;
                int hours = (int) ( totalSecs / 3600 );
                double remaining = totalSecs % 3600;
                int mins = (int) ( remaining / 60 );
                double secs = remaining % 60;
                final String timeStamp = String.format( "%02d:%02d:%05.2f", hours, mins, secs );
                final String timeDuration = String.format( "%05.2f", ( (double) timeSinceLastAct ) / 1000 );

                tagsStream.write( '\t' );
                tagsStream.write( timeStamp );

                // Activity tag
                tagsStream.write( '\t' );
                tagsStream.write( actEvt.getTag() );

                // Duration
                tagsStream.write( '\t' );
                tagsStream.write( timeDuration );

                // Update time since last activity and finish
                timeLastActivity = time;
                tagsStream.write( '\n' );
            }
        }

        return;
    }

    /** @return the number of events stored. */
    public int size()
    {
        return this.events.size();
    }

    /** @return the time spent in this experiment. */
    public long getEventsTimeLength()
    {
        long toret = 0;

        if ( this.size() > 0 ) {
            toret = this.events.get( this.events.size() - 1 ).getMillis();
        }

        return toret;
    }

    @Override
    public void writeToJSON(JsonWriter jsonWriter) throws IOException
    {
        final String RESULT_NAME = this.getResultName();

        this.writeIdToJSON( jsonWriter );
        jsonWriter.name( Orm.FIELD_NAME ).value( RESULT_NAME );
        jsonWriter.name( Orm.FIELD_DATE ).value( this.getTime() );
        jsonWriter.name( Orm.FIELD_EXPERIMENT_ID )
                    .value( this.getExperiment().getId().get() );
        jsonWriter.name( Orm.FIELD_USER_ID )
                    .value( this.getUser().getId().get() );

        jsonWriter.name( Orm.FIELD_EVENTS ).beginArray();
        for(Event event: this.events) {
            event.writeToJSON( jsonWriter );
        }

        jsonWriter.endArray();
    }

    public String getResultName()
    {
        return buildResultName( this );
    }

    @Override
    public String toString()
    {
        return
            this.getId() + "@" + this.getTime() + ": "
            + this.getExperiment().getName()
                + "(" + this.getExperiment().getId() + ")"
            + " " + this.getUser().getName() + "(" + this.getUser().getId() + ")";
    }

    public static Result fromJSON(Reader reader) throws JSONException
    {
        final JsonReader jsonReader = new JsonReader( reader );
        final ArrayList<Event> events = new ArrayList<>();
        Result toret;
        TypeId typeId = null;
        Id id = null;
        Id userId = null;
        Id experimentId = null;
        long dateTime = -1L;

        // Load data
        try {
            jsonReader.beginObject();
            while ( jsonReader.hasNext() ) {
                final String nextName = jsonReader.nextName();

                if ( nextName.equals( Orm.FIELD_DATE ) ) {
                    dateTime = jsonReader.nextLong();
                }
                else
                if ( nextName.equals( Orm.FIELD_NAME ) ) {
                    jsonReader.nextString();
                }
                else
                if ( nextName.equals( Orm.FIELD_TYPE_ID ) ) {
                    typeId = readTypeIdFromJson( jsonReader );
                }
                else
                if ( nextName.equals( Id.FIELD ) ) {
                    id = readIdFromJSON( jsonReader );
                }
                else
                if ( nextName.equals( Orm.FIELD_USER_ID ) ) {
                    userId = readIdFromJSON( jsonReader );
                }
                else
                if ( nextName.equals( Orm.FIELD_EXPERIMENT_ID ) ) {
                    experimentId = readIdFromJSON( jsonReader );
                }
                else
                if ( nextName.equals( Orm.FIELD_EVENTS ) ) {
                    jsonReader.beginArray();
                    while( jsonReader.hasNext() ) {
                        events.add( Event.fromJSON( jsonReader ) );
                    }

                    jsonReader.endArray();
                }
            }
        } catch(IOException exc)
        {
            final String ERROR_MSG = "Creating result from JSON: " + exc.getMessage();

            Log.e( LogTag, ERROR_MSG );
            throw new JSONException( ERROR_MSG );
        }

        // Chk
        if ( id == null
          || userId == null
          || experimentId == null
          || dateTime < 0
          || typeId != TypeId.Result )
        {
            final String msg = "Creating result from JSON: invalid or missing data.";

            Log.e( LogTag, msg );
            throw new JSONException( msg );
        } else {
            final Orm orm = Orm.get();

            try {
                final Experiment expr = (Experiment) orm.retrieve( experimentId, TypeId.Experiment );
                final User usr = orm.createOrRetrieveUserById( userId );

                toret = new Result( id, dateTime, usr, expr );
                toret.addAll( events );
            } catch(IOException exc) {
                final String ERROR_MSG = "Retrieving results' experiment or user data set: " + exc.getMessage();

                Log.e( LogTag, ERROR_MSG );
                throw new JSONException( ERROR_MSG );
            }
        }

        return toret;
    }

    /** Creates the result name. This name contains important info.
      * @param res The result to build a name for.
      */
    public static String buildResultName(Result res)
    {
        return TypeId.Result.toString().toLowerCase()
                + "-i" + res.getId()
                + "-t" + res.getTime()
                + "-u" + res.getUser().getId().get()
                + "-e" + res.getExperiment().getId().get();
    }

    /** @return the result's id, reading it from its name.
     * @param resName the name of the result to extract the time from.
     */
    public static long parseIdFromName(String resName)
    {
        final String strId = parseName( resName )[ 1 ];

        if ( strId.charAt( 0 ) != 'i' ) {
            throw new Error( "malformed result name looking for id: "
                    + strId
                    + "/" + resName );
        }

        return Long.parseLong( strId.substring( 1 ) );
    }

    /** @return the result's time - date, reading it from its name.
      * @param resName the name of the result to extract the time from.
      */
    public static long parseTimeFromName(String resName)
    {
        final String strTime = parseName( resName )[ 2 ];

        if ( strTime.charAt( 0 ) != 't' ) {
            throw new Error( "malformed result name looking for time: "
                    + strTime
                    + "/" + resName );
        }

        return Long.parseLong( strTime.substring( 1 ) );
    }

    /** @return the user's id from this result, reading it from its name.
     * @param resName the name of the result to extract the time from.
     */
    public static long parseUserIdFromName(String resName)
    {
        final String strUserId = parseName( resName )[ 3 ];

        if ( strUserId.charAt( 0 ) != 'u' ) {
            throw new Error( "malformed result name looking for user's id: "
                    + strUserId
                    + "/" + resName );
        }

        return Long.parseLong( strUserId.substring( 1 ) );
    }

    /** @return the user's id from this result, reading it from its name.
     * @param resName the name of the result to extract the time from.
     */
    public static long parseExperimentIdFromName(String resName)
    {
        final String strExperimentId = parseName( resName )[ 4 ];

        if ( strExperimentId.charAt( 0 ) != 'e' ) {
            throw new Error( "malformed result name looking for experiment's id: "
                                + strExperimentId
                                + "/" + resName );
        }

        return Long.parseLong( strExperimentId.substring( 1 ) );
    }

    private static String[] parseName(String resName)
    {
        if ( resName == null
          || resName.isEmpty() )
        {
            resName = "";
        }

        resName = resName.trim();
        final String[] toret = resName.split( "-" );

        if ( toret.length != 5 ) {
            throw new Error( "dividing result name in parts" );
        }

        return toret;
    }

    private User user;
    private Experiment experiment;
    private long dateTime;
    private ArrayList<Event> events;
}

package com.devbaltasarq.varse.core;


import com.devbaltasarq.varse.core.experiment.Tag;
import com.devbaltasarq.varse.core.ofmcache.EntitiesCache;
import com.devbaltasarq.varse.core.ofmcache.FileCache;

import androidx.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Consumer;


/** Represents the results of a given experiment. */
public class Result extends Persistent {
    public static final String LOG_TAG = Result.class.getSimpleName();

    public static class Builder {
        public Builder(String rec, Experiment expr, long dateTime)
        {
            this.rec = rec;
            this.experiment = expr;
            this.dateTime = dateTime;
            this.events = new ArrayList<>();
        }

        /** Adds a new Event to the list.
          * @param event the new Event.
          * @see Event
          */
        public void add(Event event)
        {
            this.events.add( event );
        }

        /** Adds all the given events.
          * @param events a collection of events.
          * @see Event
          */
        public void addAll(Collection<Event> events)
        {
            this.events.addAll( events );
        }

        /** Clears all the stored events. */
        public void clear()
        {
            this.events.clear();
        }

        /** @return all stored events up to this moment. */
        public Event[] getEvents()
        {
            return this.events.toArray( new Event[ 0 ] );
        }

        /** @return the appropriate Result object, given the current data.
          * @see Result
          */
        public Result build(long elapsedMillis)
        {
            return new Result(
                            Id.create(),
                            this.dateTime,
                            elapsedMillis,
                            this.rec,
                            this.experiment,
                            this.events.toArray( new Event[ 0 ] ) );
        }

        private final String rec;
        private final Experiment experiment;
        private final long dateTime;
        private final ArrayList<Event> events;
    }

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
                final Event EVT = (Event) obj2;

                toret = ( this.getMillis() == EVT.getMillis() );
            }

            return toret;
        }

        public void writeToJSON(JsonWriter jsonWriter) throws IOException
        {
            jsonWriter.name( Ofm.FIELD_ELAPSED_TIME )
                    .value( this.getMillis() );
        }

        protected void writeEventTypeToJSON(JsonWriter jsonWriter, String type) throws IOException
        {
            jsonWriter.name( Ofm.FIELD_EVENT_TYPE ).value( type );
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
                    final String NEXT_NAME = jsonReader.nextName();

                    if ( NEXT_NAME.equals( Ofm.FIELD_EVENT_TYPE ) ) {
                        eventType = jsonReader.nextString();
                    }
                    else
                    if ( NEXT_NAME.equals( Ofm.FIELD_ELAPSED_TIME ) ) {
                        millis = jsonReader.nextLong();
                    }
                    else
                    if ( NEXT_NAME.equals( Ofm.FIELD_HEART_BEAT_AT ) ) {
                        heartbeat = jsonReader.nextLong();
                    }
                    else
                    if ( NEXT_NAME.equals( Ofm.FIELD_TAG ) ) {
                        tag = jsonReader.nextString();
                    }
                }

                jsonReader.endObject();
            } catch(IOException exc)
            {
                final String ERROR_MSG = "Creating event from JSON: " + exc.getMessage();

                Log.e(LOG_TAG, ERROR_MSG );
                throw new JSONException( ERROR_MSG );
            }

            // Chk
            if ( eventType == null
              || millis < 0 )
            {
                final String MSG = "Creating event from JSON: invalid or missing data.";

                Log.e(LOG_TAG, MSG );
                throw new JSONException( MSG );
            } else {
                if ( eventType.equals( Ofm.FIELD_EVENT_ACTIVITY_CHANGE ) ) {
                    if ( tag == null ) {
                        final String MSG = "Creating change activity event from JSON: invalid or missing data.";

                        Log.e(LOG_TAG, MSG );
                        throw new JSONException( MSG );
                    } else {
                        toret = new ActivityChangeEvent( millis, new Tag( tag ) );
                    }
                }
                else
                if ( eventType.equals( Ofm.FIELD_EVENT_HEART_BEAT ) ) {
                    if ( heartbeat < 0 ) {
                        final String MSG = "Creating new hear beat event from JSON: invalid or missing data.";

                        Log.e(LOG_TAG, MSG );
                        throw new JSONException( MSG );
                    } else {
                        toret = new BeatEvent( millis, heartbeat );
                    }
                } else {
                    final String MSG = "Creating event from JSON: unknown event.";

                    Log.e(LOG_TAG, MSG );
                    throw new JSONException( MSG );
                }
            }

            return toret;
        }

        private final long millis;
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
                final BeatEvent EVT = (BeatEvent) obj2;

                toret = this.getTimeOfNewHeartBeat() == EVT.getTimeOfNewHeartBeat();
            }

            return toret;
        }

        private void writeEventTypeToJSON(JsonWriter jsonWriter) throws IOException
        {
            super.writeEventTypeToJSON( jsonWriter, Ofm.FIELD_EVENT_HEART_BEAT );
        }

        @Override
        public void writeToJSON(JsonWriter jsonWriter) throws IOException
        {
            jsonWriter.beginObject();

            super.writeToJSON( jsonWriter );

            this.writeEventTypeToJSON( jsonWriter );
            jsonWriter.name( Ofm.FIELD_HEART_BEAT_AT)
                    .value( Long.toString( this.getTimeOfNewHeartBeat() ) );

            jsonWriter.endObject();
        }

        private final long timeOfNewHeartBeat;
    }

    /** Event: new activity. */
    public static class ActivityChangeEvent extends Event {
        public ActivityChangeEvent(long millis, Tag tag)
        {
            super( millis );
            this.tag = tag;
        }

        public Tag getTag()
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
                final ActivityChangeEvent EVT = (ActivityChangeEvent) obj2;

                toret = this.getTag().equals( EVT.getTag() );
            }

            return toret;
        }

        private void writeEventTypeToJSON(JsonWriter jsonWriter) throws IOException
        {
            super.writeEventTypeToJSON( jsonWriter, Ofm.FIELD_EVENT_ACTIVITY_CHANGE );
        }

        @Override
        public void writeToJSON(JsonWriter jsonWriter) throws IOException
        {
            jsonWriter.beginObject();

            super.writeToJSON( jsonWriter );

            this.writeEventTypeToJSON( jsonWriter );
            jsonWriter.name( Ofm.FIELD_TAG ).value( this.getTag().toString() );

            jsonWriter.endObject();
        }

        private final Tag tag;
    }

    /** Creates a new Result, in which the events of the experiment will be stored.
     * @param id   the id of the result.
     * @param dateTime the moment (in millis) this experiment was collected.
     * @param durationInMillis the duration of the experiment in milliseconds.
     * @param rec the record label.
     * @param expr the experiment that was performed.
     */
    public Result(Id id, long dateTime, long durationInMillis,
                  String rec, Experiment expr, Event[] events)
    {
        super( id );

        this.durationInMillis = durationInMillis;
        this.dateTime = dateTime;
        this.rec = rec;
        this.experiment = expr;
        this.events = events;
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

    /** @return the record label */
    public String getRec()
    {
        return this.rec;
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
                + ( 27 * this.getRec().hashCode() )
                + ( 37 * this.getExperiment().hashCode() );
    }

    @Override
    public boolean equals(Object o)
    {
        boolean toret = false;

        if ( o instanceof Result ) {
            Result ro = (Result) o;

            if ( this.getRec().equals( ro.getRec() )
              && this.getExperiment().equals( ro.getExperiment() ) )
            {
                toret = true;
            }
        }

        return toret;
    }

    @Override
    public File[] enumerateMediaFiles()
    {
        return new File[ 0 ];
    }

    /** @return all events in this result. Warning: the list can be huge. */
    public Event[] buildEventsList()
    {
        return Arrays.copyOf( this.events, this.events.length );
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
        final ArrayList<Long> BEAT_DISTANCE_TIMES = new ArrayList<>( this.size() );

        // Create
        for(Event evt: this.events) {
            if ( evt instanceof BeatEvent ) {
                BEAT_DISTANCE_TIMES.add( ( (BeatEvent) evt ).getTimeOfNewHeartBeat() );
            }
        }

        // Convert to primitive (come on, Java...).
        long[] toret = new long[ BEAT_DISTANCE_TIMES.size() ];
        int pos = 0;
        for(long l: BEAT_DISTANCE_TIMES) {
            toret[ pos ] = l;
            ++pos;
        }

        BEAT_DISTANCE_TIMES.clear();
        return toret;
    }

    /** @return the next event of type activity change counting from position i + 1. */
    private ActivityChangeEvent locateNextActivityChangeEvent(int i)
    {
        ActivityChangeEvent toret = null;
        final int NUM_EVENTS = this.events.length;

        for( ++i; i < NUM_EVENTS; ++i ) {
            final Event EVT = this.events[ i ];

            if ( EVT instanceof ActivityChangeEvent ) {
                toret = (ActivityChangeEvent) EVT;
                break;
            }
        }

        return toret;
    }

    /** Creates the standard pair of text files, one for heatbeats,
      * and another one to know when the activity changed.
      */
    public void exportToStdTextFormat(final Writer TAGS_STREAM,
                                      final Writer BEATS_STREAM)
                                                        throws IOException
    {
        final int NUM_EVENTS = this.events.length;

        if ( TAGS_STREAM != null ) {
            TAGS_STREAM.write( "Init_time\tTag\tDurat\n" );
        }

        // Run all over the events and scatter them on files
        for(int i = 0; i < NUM_EVENTS; ++i) {
            final Event EVT = this.events[ i ];

            if ( EVT instanceof BeatEvent
              && BEATS_STREAM != null )
            {
                BEATS_STREAM.write( Long.toString( ( (BeatEvent) EVT ).getTimeOfNewHeartBeat() ) );
                BEATS_STREAM.write( '\n' );
            }
            if ( EVT instanceof ActivityChangeEvent
              && TAGS_STREAM != null )
            {
                final ActivityChangeEvent ACT_EVT = ( (ActivityChangeEvent) EVT );
                final long millis = EVT.getMillis();
                long timeActWillLast;

                // Determine duration
                final ActivityChangeEvent NEXT_ACTIVITY_CHANGE_EVT =
                        this.locateNextActivityChangeEvent( i );

                if ( NEXT_ACTIVITY_CHANGE_EVT != null ) {
                    timeActWillLast = NEXT_ACTIVITY_CHANGE_EVT.getMillis() - millis;
                } else {
                    timeActWillLast = this.getDurationInMillis() - millis;
                }

                // Experiment's elapsed time
                final double TOTAL_SECS = ( (double) millis ) / 1000;
                int hours = (int) ( TOTAL_SECS / 3600 );
                double remaining = TOTAL_SECS % 3600;
                int mins = (int) ( remaining / 60 );
                double secs = remaining % 60;
                final String TIME_STAMP = String.format( Locale.US,
                                                        "%02d:%02d:%05.2f", hours, mins, secs );
                final String TIME_DURATION = String.format( Locale.US,
                                                        "%.2f", ( (double) timeActWillLast ) / 1000 );

                TAGS_STREAM.write( TIME_STAMP );

                // Activity tag
                TAGS_STREAM.write( '\t' );
                TAGS_STREAM.write( ACT_EVT.getTag().toString() );

                // Duration
                TAGS_STREAM.write( '\t' );
                TAGS_STREAM.write( TIME_DURATION );

                // Finish this entry
                TAGS_STREAM.write( '\n' );
            }
        }

        return;
    }

    /** @return the number of events stored. */
    public int size()
    {
        return this.events.length;
    }

    @Override
    public void writeToJSON(JsonWriter jsonWriter) throws IOException
    {
        final String RESULT_NAME = this.buildResultName();

        this.writeIdToJSON( jsonWriter );
        jsonWriter.name( Ofm.FIELD_NAME ).value( RESULT_NAME );
        jsonWriter.name( Ofm.FIELD_DATE ).value( this.getTime() );
        jsonWriter.name( Ofm.FIELD_TIME ).value( this.getDurationInMillis() );
        jsonWriter.name( EntitiesCache.FIELD_EXPERIMENT_ID )
                    .value( this.getExperiment().getId().get() );
        jsonWriter.name( Ofm.FIELD_REC )
                    .value( this.getRec() );

        jsonWriter.name( Ofm.FIELD_EVENTS ).beginArray();
        for(Event event: this.events) {
            event.writeToJSON( jsonWriter );
        }

        jsonWriter.endArray();
    }

    /** @return the duration in millis. Will throw if the experiment is not finished yet. */
    public long getDurationInMillis()
    {
        return this.durationInMillis;
    }

    @Override @NonNull
    public String toString()
    {
        return
                this.getId() + "@" + this.getTime()
                        + ": " + this.getExperiment().getName()
                        + "(" + this.getExperiment().getId()
                        + ") (" + this.getRec() + ")";
    }

    private static final String ID_PART = "i";
    private static final String TIME_PART = "t";
    private static final String REC_PART = "rk";
    private static final String EXPERIMENT_ID_PART = "e";

    /** @return the result name (NOT the file name, the name inside JSON).
     * This is used inside the JSON file.
     * This name contains important info.
     * The name structure must be made consistent with the static parse_XXX functions below.
     */
    public String buildResultName()
    {
        return Persistent.TypeId.Result.toString().toLowerCase()
                + FileCache.FILE_NAME_PART_SEPARATOR + ID_PART + this.getId()
                + FileCache.FILE_NAME_PART_SEPARATOR + TIME_PART + this.getTime()
                + FileCache.FILE_NAME_PART_SEPARATOR + REC_PART + PlainStringEncoder.get().encode( this.getRec() )
                + FileCache.FILE_NAME_PART_SEPARATOR + EXPERIMENT_ID_PART + this.getExperiment().getId();
    }

    /** @return the rec name (NOT the file name, the rec inside the name inside JSON).
     * @param PART_NAME the part to look for.
     */
    private static String parseStrFromName(final String NAME, final String PART_NAME)
    {
        final String[] PARTS = NAME.split( FileCache.FILE_NAME_PART_SEPARATOR );
        String toret = "";

        for(final String PART: PARTS) {
            if ( PART.startsWith( PART_NAME ) ) {
                toret = PART.substring( PART_NAME.length() );
                break;
            }
        }

        if ( toret.isEmpty() ) {
            throw new Error( "missing " + PART_NAME + " in result name: " + NAME );
        }

        return toret;
    }

    /** @return the result's time - date, reading it from its name
      * (NOT the file name, the name inside JSON).
      * @param resName the name of the result to extract the time from.
      */
    public static long parseTimeFromName(String resName)
    {
        return parseLongFromName( resName, TIME_PART );
    }

    /** @return the result's record, reading it from its name
     * (NOT the file name, the name inside JSON).
     * @param resName the name of the result to extract the time from.
     */
    public static String parseRecFromName(String resName)
    {
        return parseStrFromName( resName, REC_PART );
    }

    /** @return the result's time - date, reading it from its name (NOT file name).
      * @param RES_NAME the name of the result to extract the time from.
      * @param PART_NAME the name of the part to extract.
      */
    private static long parseLongFromName(final String RES_NAME, final String PART_NAME)
    {
        String StrToret = parseStrFromName( RES_NAME, PART_NAME );
        long toret = 0;

        try {
            toret = Long.parseLong( StrToret );
        } catch(NumberFormatException exc)
        {
            throw new Error( "malformed result name looking for time, found: "
                    + StrToret + "/" + RES_NAME );
        }

        return toret;
    }

    public static Result fromJSON(Reader reader) throws JSONException
    {
        final JsonReader JSON_READER = new JsonReader( reader );
        final ArrayList<Event> EVENTS = new ArrayList<>();
        Result toret;
        long durationInMillis = -1L;
        TypeId typeId = null;
        Id id = null;
        String rec = "";
        Id experimentId = null;
        long dateTime = -1L;

        // Load data
        try {
            JSON_READER.beginObject();
            while ( JSON_READER.hasNext() ) {
                final String nextName = JSON_READER.nextName();

                if ( nextName.equals( Ofm.FIELD_DATE ) ) {
                    dateTime = JSON_READER.nextLong();
                }
                else
                if ( nextName.equals( Ofm.FIELD_TIME ) ) {
                    durationInMillis = JSON_READER.nextLong();
                }
                else
                if ( nextName.equals( Ofm.FIELD_NAME ) ) {
                    JSON_READER.nextString();
                }
                else
                if ( nextName.equals( Ofm.FIELD_TYPE_ID ) ) {
                    typeId = readTypeIdFromJson( JSON_READER );
                }
                else
                if ( nextName.equals( Id.FIELD ) ) {
                    id = readIdFromJSON( JSON_READER );
                }
                else
                if ( nextName.equals( Ofm.FIELD_REC ) ) {
                    rec = JSON_READER.nextString();
                }
                else
                if ( nextName.equals( EntitiesCache.FIELD_EXPERIMENT_ID ) ) {
                    experimentId = readIdFromJSON( JSON_READER );
                }
                else
                if ( nextName.equals( Ofm.FIELD_EVENTS ) ) {
                    JSON_READER.beginArray();
                    while( JSON_READER.hasNext() ) {
                        EVENTS.add( Event.fromJSON( JSON_READER ) );
                    }

                    JSON_READER.endArray();
                } else {
                    JSON_READER.skipValue();
                }
            }
        } catch(IOException exc)
        {
            final String ERROR_MSG = "Creating result from JSON: " + exc.getMessage();

            Log.e(LOG_TAG, ERROR_MSG );
            throw new JSONException( ERROR_MSG );
        }

        // Chk
        if ( id == null
          || experimentId == null
          || dateTime < 0
          || durationInMillis < 0
          || typeId != TypeId.Result )
        {
            final String MSG_ERROR = "Creating result from JSON: invalid or missing data.";

            Log.e( LOG_TAG, MSG_ERROR );
            throw new JSONException( MSG_ERROR );
        } else {
            final Ofm OFM = Ofm.get();

            if ( rec.isEmpty() ) {
                rec = "r";
            }

            try {
                final Experiment EXPR = (Experiment) OFM.retrieve( experimentId, TypeId.Experiment );

                toret = new Result( id,
                                    dateTime,
                                    durationInMillis,
                                    rec,
                                    EXPR,
                                    EVENTS.toArray( new Event[ 0 ] ) );
            } catch(IOException exc) {
                final String ERROR_MSG = "Retrieving results' experiment or user data set: " + exc.getMessage();

                Log.e(LOG_TAG, ERROR_MSG );
                throw new JSONException( ERROR_MSG );
            }
        }

        return toret;
    }

    private final long durationInMillis;
    private final String rec;
    private final Experiment experiment;
    private final long dateTime;
    private final Event[] events;
}

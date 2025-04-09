package com.devbaltasarq.varse.core;


import com.devbaltasarq.varse.core.experiment.Tag;
import com.devbaltasarq.varse.core.ofm.FileManager;

import androidx.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


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

            if ( obj2 instanceof final Event EVT) {
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
                jsonReader.setLenient( true );
                jsonReader.beginObject();
                while ( jsonReader.hasNext() ) {
                    final String NEXT_NAME = jsonReader.nextName();

                    switch (NEXT_NAME) {
                        case Ofm.FIELD_EVENT_TYPE -> eventType = jsonReader.nextString();
                        case Ofm.FIELD_ELAPSED_TIME -> millis = jsonReader.nextLong();
                        case Ofm.FIELD_HEART_BEAT_AT -> heartbeat = jsonReader.nextLong();
                        case Ofm.FIELD_TAG -> tag = jsonReader.nextString();
                        default -> jsonReader.skipValue();
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
              && obj2 instanceof final BeatEvent EVT)
            {
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
              && obj2 instanceof final ActivityChangeEvent EVT)
            {
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
        this.events = Arrays.asList( events );
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

    @Override
    public String getName()
    {
        return this.getRec();
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

        if ( o instanceof final Result ro) {
            if ( this.getRec().equals( ro.getRec() )
              && this.getExperiment().equals( ro.getExperiment() ) )
            {
                toret = true;
            }
        }

        return toret;
    }

    /** @return all events in this result. Warning: the list can be huge. */
    public List<Event> buildEventsList()
    {
        return new ArrayList<>( this.events );
    }

    /** @return a list with all activity changes. */
    public List<ActivityChangeEvent> buildActivityChangesList()
    {
        final List<ActivityChangeEvent> toret = new ArrayList<>( this.experiment.getNumActivities() );

        for(Event evt: this.events) {
            if ( evt instanceof final ActivityChangeEvent EVT) {
                toret.add( EVT );
            }
        }

        return toret;
    }

    /** @return the heart beats time distance, as a long[]. */
    public long[] buildHeartBeatsList()
    {
        final List<Long> BEAT_DISTANCE_TIMES = new ArrayList<>( this.size() );

        // Create
        for(Event evt: this.events) {
            if ( evt instanceof final BeatEvent BEAT_EVT) {
                BEAT_DISTANCE_TIMES.add( BEAT_EVT.getTimeOfNewHeartBeat() );
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
        final int NUM_EVENTS = this.events.size();

        for( ++i; i < NUM_EVENTS; ++i ) {
            final Event EVT = this.events.get( i );

            if ( EVT instanceof final ActivityChangeEvent ACTIVITY_EVT) {
                toret = ACTIVITY_EVT;
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
        final int NUM_EVENTS = this.events.size();

        if ( TAGS_STREAM != null ) {
            TAGS_STREAM.write( "Init_time\tTag\tDurat\n" );
        }

        // Run all over the events and scatter them on files
        for(int i = 0; i < NUM_EVENTS; ++i) {
            final Event EVT = this.events.get( i );

            if ( EVT instanceof final BeatEvent BEAT_EVT
              && BEATS_STREAM != null )
            {
                BEATS_STREAM.write( Long.toString( BEAT_EVT.getTimeOfNewHeartBeat() ) );
                BEATS_STREAM.write( '\n' );
            }
            else
            if ( EVT instanceof final ActivityChangeEvent ACT_EVT
              && TAGS_STREAM != null )
            {
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
        return this.events.size();
    }

    @Override
    public void writeToJSON(JsonWriter jsonWriter) throws IOException
    {
        final String RESULT_NAME = this.buildName();

        this.writeIdToJSON( jsonWriter );
        jsonWriter.name( Ofm.FIELD_NAME ).value( RESULT_NAME );
        jsonWriter.name( Ofm.FIELD_DATE ).value( this.getTime() );
        jsonWriter.name( Ofm.FIELD_TIME ).value( this.getDurationInMillis() );
        jsonWriter.name( EXPERIMENT_ID_PART )
                    .value( this.getExperiment().getId().get() );
        jsonWriter.name( REC_PART )
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

    /** Returns the name for this result.
      * (internal or file name).
      * @return the name for this result.
      * @see Result::buildNameWith
      */
    public String buildName()
    {
        return buildNameWith(
                this.getId(),
                this.getExperiment().getId(),
                this.getExperiment().getName(),
                this.getRec(),
                this.getTime());
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

    private static final String TIME_PART = "tm";
    private static final String REC_PART = "rec";
    private static final String EXPERIMENT_NAME_PART = "expr";
    private static final String EXPERIMENT_ID_PART = "exprid";

    /** The structure for file name and JSON's name inside. */
    private static final String NAME_FORMAT =
                    Id.FILE_NAME_PART
                    + FileManager.FILE_NAME_PART_SEPARATOR + "$" + Id.FILE_NAME_PART
                    + FileManager.FILE_NAME_PART_SEPARATOR + EXPERIMENT_NAME_PART
                    + FileManager.FILE_NAME_PART_SEPARATOR + "$" + EXPERIMENT_NAME_PART
                    + FileManager.FILE_NAME_PART_SEPARATOR + REC_PART
                    + FileManager.FILE_NAME_PART_SEPARATOR + "$" + REC_PART
                    + FileManager.FILE_NAME_PART_SEPARATOR + EXPERIMENT_ID_PART
                    + FileManager.FILE_NAME_PART_SEPARATOR + "$" + EXPERIMENT_ID_PART
                    + FileManager.FILE_NAME_PART_SEPARATOR + TIME_PART
                    + FileManager.FILE_NAME_PART_SEPARATOR + "$" + TIME_PART
                    + ".$" + FileManager.FIELD_EXT;

    /** @return the name for a the corresponding result file.
     * @param id The id of the result file.
     * @param exprId The experiment id for the result data set file.
     * @param experimentName the name of the experiment.
     * @param rec the name of the record.
     */
    public static String buildNameWith(Id id, Id exprId, String experimentName, String rec, long time)
    {
        return Result.NAME_FORMAT
                .replace( "$" + FileManager.FIELD_EXT,
                                            FileManager.getFileExtFor( Persistent.TypeId.Result ) )
                .replace( "$" + Id.FILE_NAME_PART, id.toString() )
                .replace( "$" + EXPERIMENT_ID_PART, exprId.toString() )
                .replace( "$" + REC_PART, PlainStringEncoder.get().encode( rec ) )
                .replace( "$" + TIME_PART, Long.toString( time ) )
                .replace( "$" + EXPERIMENT_NAME_PART,
                                            PlainStringEncoder.get().encode( experimentName ) );
    }

    /** @return an str part from the result's name.
      * @param PART_NAME the part to look for.
      */
    private static String parseStrFromName(final String NAME, final String PART_NAME)
    {
        final String[] FILE_PARTS = NAME.split( "\\." );
        final String[] PARTS = FILE_PARTS[ 0 ].split( FileManager.FILE_NAME_PART_SEPARATOR );
        final int PARTS_LENGTH = PARTS.length;
        String toret = "";

        for(int i = 0; i < ( PARTS_LENGTH - 1 ); i += 2) {
            final String PART = PARTS[ i ];

            if ( PART.equals( PART_NAME ) ) {
                toret = PARTS[ i + 1 ];
                break;
            }
        }

        if ( toret.isEmpty() ) {
            throw new Error( "missing " + PART_NAME + " in result name: " + NAME );
        }

        return toret;
    }

    /** @return the result's time - date, reading it from its file name.
      * @param RES_NAME the name of the result to extract the time from.
      * @param PART_NAME the name of the part to extract.
      */
    private static long parseLongFromName(final String RES_NAME, final String PART_NAME)
    {
        String STR_TORET = parseStrFromName( RES_NAME, PART_NAME );
        long toret = 0;

        try {
            toret = Long.parseLong( STR_TORET );
        } catch(NumberFormatException exc)
        {
            throw new Error( "malformed result name looking for long, found: "
                    + STR_TORET + "/" + RES_NAME );
        }

        return toret;
    }

    /** @return the result's experiment id, reading it from its name
      *(NOT the file name, the name inside JSON).
      * @param RES_NAME the name of the result to extract from.
      */
    public static long parseExprIdFromName(final String RES_NAME)
    {
        return parseLongFromName( RES_NAME, EXPERIMENT_ID_PART );
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

    public static Result fromJSON(Reader reader) throws JSONException
    {
        final JsonReader JSON_READER = new JsonReader( reader );
        final List<Event> EVENTS = new ArrayList<>();
        Result toret;
        long durationInMillis = -1L;
        TypeId typeId = null;
        Id id = null;
        String rec = "";
        Id experimentId = null;
        long dateTime = -1L;

        // Load data
        try {
            JSON_READER.setLenient( true );
            JSON_READER.beginObject();
            while ( JSON_READER.hasNext() ) {
                final String nextName = JSON_READER.nextName();

                switch (nextName) {
                    case Ofm.FIELD_DATE -> dateTime = JSON_READER.nextLong();
                    case Ofm.FIELD_TIME -> durationInMillis = JSON_READER.nextLong();
                    case Ofm.FIELD_NAME -> JSON_READER.nextString();
                    case Ofm.FIELD_TYPE_ID -> typeId = readTypeIdFromJson(JSON_READER);
                    case Id.FIELD -> id = readIdFromJSON(JSON_READER);
                    case REC_PART -> rec = JSON_READER.nextString();
                    case EXPERIMENT_ID_PART -> experimentId = readIdFromJSON(JSON_READER);
                    case Ofm.FIELD_EVENTS -> {
                        JSON_READER.beginArray();
                        while (JSON_READER.hasNext()) {
                            EVENTS.add(Event.fromJSON(JSON_READER));
                        }

                        JSON_READER.endArray();
                    }
                    default -> JSON_READER.skipValue();
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
    private final List<Event> events;
}

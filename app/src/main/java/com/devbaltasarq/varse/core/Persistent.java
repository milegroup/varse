package com.devbaltasarq.varse.core;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import org.json.JSONException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;


/** Represents classes that can be stored and retrieved using JSON. */
public abstract class Persistent implements Identifiable {
    private static final String LogTag = Persistent.class.getSimpleName();

    public enum TypeId {
            User, Result, Experiment,
            PictureGroup, VideoGroup, ManualGroup,
            MediaActivity, ManualActivity;

        public static final String FIELD = "type_id";

        @Override
        public String toString()
        {
            return this.name().toLowerCase();
        }

        public static TypeId parse(String strTypeId) throws IllegalArgumentException
        {
            TypeId toret = null;
            final TypeId[] typeIds = TypeId.values();

            strTypeId = strTypeId.trim().toLowerCase();

            // Look for type id
            for (final TypeId typeId : typeIds) {
                if ( strTypeId.equals( typeId.toString() ) ) {
                    toret = typeId;
                    break;
                }
            }

            if ( toret == null ) {
                throw new IllegalArgumentException( "not a type id: " + strTypeId );
            }

            return toret;
        }
    }

    /** Creates a new object that can be stored in the db. */
    public Persistent(Id id)
    {
        this.id = id;
    }

    /** Assigns new ids to this object. Useful when storing the object for the first time. */
    public void updateIds(Orm orm)
    {
        this.id = Id.create();
    }

    /** @return The id of this object. */
    @Override
    public Id getId()
    {
        return this.id;
    }

    /** Writes the common properties of a persistent object to JSON.
     * @param wrt The writer to write to.
     * @throws IOException throws it when there are problems with the stream.
     */
    public void toJSON(Writer wrt) throws JSONException
    {
        final String ErrorMessage = "Writing persistent object to JSON: ";
        final JsonWriter jsonWriter = new JsonWriter( wrt );

        try {
            jsonWriter.beginObject();
            this.writeToJSON( jsonWriter );
            jsonWriter.endObject();
        } catch(IOException exc)
        {
            Log.e( LogTag, ErrorMessage + exc.getMessage() );
            throw new JSONException( exc.getMessage() );
        } finally {
            try {
                jsonWriter.close();
            } catch(IOException exc) {
                Log.e( LogTag, ErrorMessage + exc.getMessage() );
            }
        }

        return;
    }

    /** Returns a an enum value for this object's type.
      * @see TypeId
      */
    public abstract TypeId getTypeId();

    /** Writes the common properties of an Activity to JSON.
     * @param jsonWriter The JSON writer to write to.
     * @throws IOException throws it when there are problems with the stream.
     */
    public abstract void writeToJSON(JsonWriter jsonWriter) throws IOException;

    /** Writes the identification of the object to a json writer. */
    protected void writeIdToJSON(JsonWriter jsonWriter) throws IOException
    {
        jsonWriter.name( Id.FIELD ).value( this.getId().get() );
        jsonWriter.name( TypeId.FIELD ).value( this.getTypeId().toString() );
    }

    public static TypeId readTypeIdFromJson(JsonReader jsonReader) throws IOException, JSONException
    {
        TypeId toret = null;

        try {
            toret = TypeId.parse( jsonReader.nextString() );
        } catch(IllegalArgumentException exc) {
            throw new JSONException( "read type id: " + exc.getMessage() );
        }

        if ( toret == null ) {
            throw new JSONException( "read type id: no valid data" );
        }

        return toret;
    }

    public static Id readIdFromJSON(JsonReader jsonReader) throws IOException, JSONException
    {
        Id toret = null;

        toret = new Id( jsonReader.nextLong() );

        if ( toret == null ) {
            throw new JSONException( "read id: no valid data" );
        }

        return toret;
    }

    public static Persistent fromJSON(TypeId id, Reader reader) throws JSONException
    {
        Persistent toret = null;

        if ( id == TypeId.User ) {
            toret = User.fromJSON( reader );
        }
        else
        if ( id == TypeId.Experiment ) {
            toret = Experiment.fromJSON( reader );
        }
        else
        if ( id == TypeId.Result ) {
            toret = Result.fromJSON( reader );
        }

        return toret;
    }

    private Id id;
}

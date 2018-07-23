package com.devbaltasarq.varse.core;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

/** Represents a user, recipient of experiments. */
public class User extends Persistent {
    private static final String LogTag = User.class.getSimpleName();

    /** Creates a new user, with a given name.
      * @param name The name of the user.
      */
    public User(Id id, String name)
    {
        super( id );
        this.name = name;
    }

    @Override
    public Experiment getExperimentOwner()
    {
        return null;
    }

    @Override
    public TypeId getTypeId()
    {
        return TypeId.User;
    }

    @Override
    public int hashCode()
    {
        return 13 * this.getName().hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        boolean toret = false;

        if ( o instanceof User ) {
            final User uo = (User) o;

            toret = this.getName().equals( uo.getName() );
        }

        return toret;
    }

    /** @return The name of the user. */
    public String getName()
    {
        return this.name;
    }

    @Override
    public void writeToJSON(JsonWriter jsonWriter) throws IOException
    {
        this.writeIdToJSON( jsonWriter );
        jsonWriter.name( Orm.FIELD_NAME ).value( this.getName() );
    }

    /** Creates a new user, from JSON data.
     * @param rd The reader from which to extract data.
     * @throws JSONException if data is not found or invalid.
     */
    public static User fromJSON(Reader rd) throws JSONException
    {
        TypeId typeId = null;
        String name = null;
        Id id = null;
        JsonReader jsonReader = new JsonReader( rd );

        // Load data
        try {
            jsonReader.beginObject();
            while ( jsonReader.hasNext() ) {
                final String nextName = jsonReader.nextName();

                if ( nextName.equals( Orm.FIELD_NAME ) ) {
                    name = jsonReader.nextString();
                }
                else
                if ( nextName.equals( Orm.FIELD_TYPE_ID ) ) {
                    typeId = readTypeIdFromJson( jsonReader );
                }
                else
                if ( nextName.equals( Id.FIELD) ) {
                    id = readIdFromJSON( jsonReader );
                }
            }
        } catch(IOException exc)
        {
            Log.e( LogTag, "Creating user from JSON: " + exc.getMessage() );
            throw new JSONException( exc.getMessage() );
        }

        // Chk
        if ( id == null
          || name == null
          || typeId != TypeId.User )
        {
            final String msg = "Creating user from JSON: invalid or missing data.";

            Log.e( LogTag, msg );
            throw new JSONException( msg );
        }

        return new User( id, name );
    }

    @Override
    public String toString()
    {
        return this.getName() + "(" + this.getId() + ")";
    }

    private String name;
}

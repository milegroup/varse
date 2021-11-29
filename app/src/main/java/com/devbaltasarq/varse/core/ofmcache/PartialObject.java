package com.devbaltasarq.varse.core.ofmcache;


import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Ofm;
import com.devbaltasarq.varse.core.Persistent;

import org.json.JSONException;

import java.io.IOException;
import java.io.Reader;

/** An object with just an id and name.
 *  This is used to partially retrieve only id's and names of complex objects.
 */
public class PartialObject extends Persistent {
    private static final String LOG_TAG = PartialObject.class.getSimpleName();

    public PartialObject(Id id, TypeId typeId, String name)
    {
        super( id );
        this.typeId = typeId;
        this.name = name;
    }

    @Override
    public Experiment getExperimentOwner()
    {
        throw new Error( "getExperimentOwner() invoked in PartialObject!!" );
    }

    @Override
    public TypeId getTypeId()
    {
        return this.typeId;
    }

    /** @return the name of the object. */
    public String getName()
    {
        return this.name;
    }

    @Override
    public int hashCode()
    {
        return ( 11 * this.getId().hashCode() ) + ( 13 * this.getName().hashCode() );
    }

    @Override
    public boolean equals(Object o)
    {
        boolean toret = false;

        if ( o instanceof PartialObject ) {
            PartialObject po = (PartialObject) o;
            toret = this.getId().equals( po.getId() ) && this.getName().equals( po.getName() );
        }

        return toret;
    }

    @Override
    public String toString()
    {
        return this.getId().toString() + ": " + this.getName();
    }

    @Override
    public void writeToJSON(JsonWriter writer) throws IOException
    {
        throw new IOException( "PartialObject.writeToJSON(): should now write this object" );
    }

    public static PartialObject fromJSON(Reader reader) throws JSONException
    {
        Persistent.TypeId typeId = null;
        Id id = null;
        String name = null;
        JsonReader jsonReader;

        try {
            jsonReader = new JsonReader( reader );
            jsonReader.beginObject();
            while( jsonReader.hasNext() ) {
                final String nextName = jsonReader.nextName();

                if ( nextName.equals( Id.FIELD) ) {
                    id = readIdFromJSON( jsonReader );
                }
                else
                if ( nextName.equals( Persistent.TypeId.FIELD) ) {
                    typeId = readTypeIdFromJson( jsonReader );
                }
                else
                if ( nextName.equals( Ofm.FIELD_NAME ) ) {
                    name = jsonReader.nextString();
                }

                if ( id != null
                  && typeId != null
                  && name != null )
                {
                    break;
                }
            }
        } catch(IOException exc) {
            final String msg = "PartialObject.fromJSON(): " + exc.getMessage();
            Log.e(LOG_TAG, msg );

            throw new JSONException( msg );
        }

        return new PartialObject( id, typeId, name );
    }

    private final String name;
    private final TypeId typeId;
}

// Varse (c) 2019/20 - 25 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.core.ofm;


import android.util.JsonReader;
import android.util.Log;

import androidx.annotation.NonNull;

import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Ofm;
import com.devbaltasarq.varse.core.Persistent;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;


/** Identification for a given stored entity: type & id. */
public class EntityId {
    private static final String LOG_TAG = EntityId.class.getSimpleName();
    public static final String NO_NAME = "";

    public EntityId(Persistent.TypeId entityType, Id id, String name)
    {
        // Reference to the file manager.
        if ( fman == null ) {
            fman = FileManager.get();
        }

        // Assign direct fields
        this.entityType = entityType;
        this.id = id;

        // Assign name (optional)
        if ( name == null
          || name.isBlank() )
        {
            this.name = NO_NAME;
        } else {
            this.name = name;
        }
    }

    /** @return the id of the entity. */
    public Id getId()
    {
        return this.id;
    }

    /** @return the entity type of the entity. */
    public Persistent.TypeId getTypeId()
    {
        return this.entityType;
    }

    /** @return the name of the entity. */
    public String getName()
    {
        return this.name;
    }

    @Override
    public int hashCode()
    {
        return ( this.entityType.hashCode() * 11 ) + this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        // TODO: unroll this
        boolean toret = obj instanceof final EntityId ENT_ID
            && this.getId().equals( ENT_ID.getId() );
        return toret;
    }

    @Override @NonNull
    public String toString()
    {
        return "EntityID: " + this.getId()
                + "\n\t" + this.getName()
                + "\n\t" + this.getTypeId();
    }

    /** @return the partial object loaded from file f. */
    public static EntityId fromFile(File f) throws IOException
    {
        EntityId toret;

        if ( f == null ) {
            throw new IOException( "entityIdFromFile(null): cannot do from null file" );
        }

        try ( final Reader READER = FileManager.openReaderFor( f ) ) {
            toret = fromJSON( READER );
        } catch(IOException | JSONException exc)
        {
            final String MSG = "entityIdFromFile(f) reading JSON: " + exc.getMessage();
            Log.e( LOG_TAG, MSG );

            throw new IOException( MSG );
        }

        return toret;
    }

    public static EntityId fromJSON(Reader reader) throws JSONException
    {
        Persistent.TypeId typeId = null;
        Id id = null;
        String name = null;

        try (final JsonReader JSON_READER = new JsonReader( reader )) {
            JSON_READER.setLenient( true );
            JSON_READER.beginObject();

            while( JSON_READER.hasNext() ) {
                final String nextName = JSON_READER.nextName();

                switch (nextName) {
                    case Id.FIELD -> id = Persistent.readIdFromJSON( JSON_READER );
                    case Persistent.TypeId.FIELD ->
                            typeId = Persistent.readTypeIdFromJson( JSON_READER );
                    case Ofm.FIELD_NAME -> name = JSON_READER.nextString();
                    default -> JSON_READER.skipValue();
                }

                if ( id != null
                  && typeId != null
                  && name != null )
                {
                    break;
                }
            }
        } catch(IOException exc) {
            final String msg = "entityIdFromJson(): " + exc.getMessage();
            Log.e( LOG_TAG, msg );

            throw new JSONException( msg );
        }

        return new EntityId( typeId, id, name );
    }

    public static EntityId fromIdType(Persistent.TypeId typeId, Id id) throws IOException
    {
        // TODO: unrroll this
        File f = fman.lookUpFile( typeId, id );
        EntityId toret = fromFile( f );
        return toret;
    }

    private final Id id;
    private final Persistent.TypeId entityType;
    private final String name;
    private static FileManager fman;
}

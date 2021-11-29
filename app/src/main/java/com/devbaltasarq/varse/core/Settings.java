package com.devbaltasarq.varse.core;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.io.Reader;

public class Settings {
    private static final String LogTag = Settings.class.getSimpleName();
    private static final String EMAIL_FIELD = "email";

    private Settings(String email)
    {
        this.email = email;
    }

    /** Stores the e.mail in the data store.
      * @param email The email of the user to store.
      */
    public void setEmail(String email)
    {
        this.email = email;
        this.save();
    }

    /** @return true if there is an email saved in this settings. */
    public boolean isEmailSet()
    {
        return !this.getEmail().isEmpty();
    }

    /** @return the stored email for the user of this device. */
    public String getEmail()
    {
        return this.email;
    }

    public void save()
    {
        final Ofm OFM = Ofm.get();
        final File F = OFM.getSettingsPath();
        Writer wrt = null;

        try {
            wrt = Ofm.openWriterFor( F );
            this.toJSON( wrt );
        } catch(IOException | JSONException exc) {
            Log.e( LogTag, "saving settings: " + exc );
        } finally {
            Ofm.close( wrt );
        }
    }

    /** Writes the common properties of a persistent object to JSON.
     * @param wrt The writer to write to.
     * @throws JSONException throws it when there are problems with the stream.
     */
    public void toJSON(Writer wrt) throws JSONException
    {
        final JsonWriter JSON_WRITER = new JsonWriter( wrt );
        final String MSG_ERROR = "Writing settings to JSON: ";

        try {
            JSON_WRITER.beginObject();
            JSON_WRITER.name( EMAIL_FIELD ).value( this.getEmail() );
            JSON_WRITER.endObject();
        } catch(IOException exc)
        {
            Log.e( LogTag, MSG_ERROR + exc.getMessage() );
            throw new JSONException( exc.getMessage() );
        } finally {
            try {
                JSON_WRITER.close();
            } catch(IOException exc) {
                Log.e( LogTag, MSG_ERROR + exc.getMessage() );
            }
        }

        return;
    }

    private String email;

    public static Settings fromJSON(Reader rd) throws JSONException
    {
        final String MSG_ERROR = "Reading settings from JSON: ";
        final JsonReader JSON_READER = new JsonReader( rd );
        String email = "";

        try {
            JSON_READER.beginObject();

            while( JSON_READER.hasNext() ) {
                final String NAME = JSON_READER.nextName();

                if ( NAME.equals( EMAIL_FIELD ) ) {
                    email = JSON_READER.nextString();
                } else {
                    JSON_READER.skipValue();
                }
            }

            JSON_READER.endObject();
        } catch(IOException exc)
        {
            Log.e( LogTag, MSG_ERROR + exc.getMessage() );
            throw new JSONException( exc.getMessage() );
        } finally {
            try {
                JSON_READER.close();
            } catch(IOException exc) {
                Log.e( LogTag, MSG_ERROR + exc.getMessage() );
            }
        }

        return new Settings( email );
    }

    public static Settings open() throws JSONException
    {
        if ( settings == null ) {
            try {
                settings = fromJSON( Ofm.openReaderFor( Ofm.get().getSettingsPath() ));
            } catch(IOException exc) {
                throw new JSONException( exc.getMessage() );
            }
        }

        return settings;
    }

    public static Settings create()
    {
        if ( settings == null ) {
            settings = new Settings( "" );
        }

        return settings;
    }

    public static Settings get()
    {
        if ( settings == null ) {
            throw new Error( "setttings were not initialized" );
        }

        return settings;
    }

    private static Settings settings;
}

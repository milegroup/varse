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
    private static String LogTag = Settings.class.getSimpleName();
    private static String EMAIL_FIELD = "email";

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
        final Orm ORM = Orm.get();
        final File F = ORM.getSettingsPath();
        Writer wrt = null;

        try {
            wrt = Orm.openWriterFor( F );
            this.toJSON( wrt );
        } catch(IOException | JSONException exc) {
            Log.e( LogTag, "saving settings: " + exc );
        } finally {
            Orm.close( wrt );
        }
    }

    /** Writes the common properties of a persistent object to JSON.
     * @param wrt The writer to write to.
     * @throws IOException throws it when there are problems with the stream.
     */
    public void toJSON(Writer wrt) throws JSONException
    {
        final JsonWriter jsonWriter = new JsonWriter( wrt );
        final String ErrorMessage = "Writing settings to JSON: ";

        try {
            jsonWriter.beginObject();
            jsonWriter.name( EMAIL_FIELD ).value( this.getEmail() );
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

    private String email;

    public static Settings fromJSON(Reader rd) throws JSONException
    {
        final String ErrorMessage = "Reading settings from JSON: ";
        final JsonReader jsonReader = new JsonReader( rd );
        String email = "";

        try {

            jsonReader.beginObject();

            while( jsonReader.hasNext() ) {
                final String NAME = jsonReader.nextName();

                if ( NAME.equals( EMAIL_FIELD ) ) {
                    email = jsonReader.nextString();
                } else {
                    jsonReader.skipValue();
                }
            }

            jsonReader.endObject();
        } catch(IOException exc)
        {
            Log.e( LogTag, ErrorMessage + exc.getMessage() );
            throw new JSONException( exc.getMessage() );
        } finally {
            try {
                jsonReader.close();
            } catch(IOException exc) {
                Log.e( LogTag, ErrorMessage + exc.getMessage() );
            }
        }

        return new Settings( email );
    }

    public static Settings open() throws JSONException
    {
        if ( settings == null ) {
            try {
                settings = fromJSON( Orm.openReaderFor( Orm.get().getSettingsPath() ));
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

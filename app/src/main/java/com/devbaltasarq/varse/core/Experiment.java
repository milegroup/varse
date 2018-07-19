package com.devbaltasarq.varse.core;

import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.core.experiment.MediaGroup;
import com.devbaltasarq.varse.core.experiment.Tag;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;


/** Represents experiments.
 *  They have mainly an id, a duration, and a list of groups. */
public class Experiment extends Persistent {
    public static String LogTag = Experiment.class.getSimpleName();

    /** Creates a new experiment.
      * @param name The name of the experiment.
      */
    public Experiment(Id id, String name)
    {
        this( id, name, false );
    }

    public Experiment(Id id, String name, boolean rnd)
    {
        super( id );

        this.name = name;
        this.groups = new ArrayList<>();
        this.random = rnd;
    }

    @Override
    public int hashCode()
    {
        int toret = 13 * this.getName().hashCode();

        toret += 17 * Boolean.valueOf( this.isRandom() ).hashCode();

        int grpsHashCode = 0;
        for(Group grp: this.groups) {
            grpsHashCode += grp.hashCode();
        }

        return toret + ( 29 * grpsHashCode );
    }

    @Override
    public boolean equals(Object o)
    {
        boolean toret = false;

        if ( o instanceof Experiment ) {
            final Experiment eo = (Experiment) o;
            final Group[] eoGroups = eo.getGroups();

            toret = this.getName().equals( eo.getName() )
                 && this.isRandom() == eo.isRandom()
                 && this.groups.size() == eoGroups.length;


            if ( toret ) {
                for(int i = 0; i < eoGroups.length; ++i) {
                    if ( !this.groups.get( i ).equals( eoGroups[ i ] ) ) {
                        toret = false;
                        break;
                    }
                }
            }
        }

        return toret;
    }

    @Override
    public void updateIds(Orm orm)
    {
        super.updateIds( orm );

        for(Group grp: this.groups) {
            grp.updateIds( orm );
        }

        return;
    }

    @Override
    public TypeId getTypeId()
    {
        return TypeId.Experiment;
    }

    /** @return The media groups involved in the experiment. */
    public Group[] getGroups()
    {
        return this.groups.toArray( new Group[ this.groups.size() ]);
    }

    /** Replaces the media groups of the experiments with new ones. */
    public void replaceGroups(Group[] files)
    {
        this.groups.clear();
        this.groups.addAll( Arrays.asList( files ) );

        for(Group grp: this.groups) {
            grp.setExperiment( this );
        }
    }

    /** Removes a given file from the media list. */
    public void removeGroup(Group g)
    {
        this.groups.remove( g );
    }

    /** Swaps a given media file with the previous one. */
    public void sortGroupUp(Group g)
    {
        final int pos = this.groups.indexOf( g );

        // Nothing to do if not found or is the first one.
        if ( pos >= 1 ) {
            Group backUp = this.groups.get( pos - 1 );
            this.groups.set( pos - 1, this.groups.get( pos ) );
            this.groups.set( pos, backUp );
        }

        return;
    }

    /** Swaps a given media file with the previous one. */
    public void sortGroupDown(Group g)
    {
        final int length = this.groups.size();
        final int pos = this.groups.indexOf( g );

        // Nothing to do if not found or is the last one.
        if ( pos < ( length - 1 ) ) {
            Group backUp = this.groups.get( pos + 1 );
            this.groups.set( pos + 1, this.groups.get( pos ) );
            this.groups.set( pos, backUp );
        }

        return;
    }

    /** @return A MediaGroup instance, given its tag, null if not found. */
    public MediaGroup locateMediaGroup(Tag tag)
    {
        MediaGroup toret = null;

        for(Group g: this.groups) {
            if ( g instanceof MediaGroup ) {
                MediaGroup mg = (MediaGroup) g;

                if ( mg.getTag().equals( tag ) ) {
                    toret = mg;
                    break;
                }
            }
        }

        return toret;
    }

    /** Locates a media activity, given the file name of its media.
      * @param fileName the file name of the media to look for.
      * @return the media activity object corresponding to that media file.
      * @see MediaGroup.MediaActivity
      */
    public MediaGroup.MediaActivity locateMediaActivity(@NonNull String fileName)
    {
        MediaGroup.MediaActivity toret = null;

        SEARCH:
        for(Group g: this.groups) {
            if ( g instanceof MediaGroup ) {
                final MediaGroup mg = (MediaGroup) g;

                for( Group.Activity act: mg.getActivities() ) {
                    final MediaGroup.MediaActivity mact = (MediaGroup.MediaActivity) act;

                    if ( mact.getFile().getName().equals( fileName ) ) {
                        toret = mact;
                        break SEARCH;
                    }
                }
            }
        }

        return toret;
    }

    /** Adds a new group to the groups list. */
    public void addGroup(Group group)
    {
        this.groups.add( group );
        group.setExperiment( this );
    }

    /** Substitute a given group.
     * @param group The group to substitute.
     */
    public void substituteGroup(Group group)
    {
        for(int i = 0; i < this.groups.size(); ++i) {
            if ( this.groups.get( i ).getId().equals( group.getId() ) ) {
                this.groups.set( i, group );
            }
        }

        group.setExperiment( this );
    }

    /** @return The name of the experiment. */
    public String getName()
    {
        return name;
    }

    /** @return Whether the groups in the experiment are played at random. */
    public boolean isRandom()
    {
        return random;
    }

    /** Sets random play order.
      * @param random The new value: true for random play.
      */
    public void setRandom(boolean random)
    {
        this.random = random;
    }

    /** Changes the name of the experiment.
      * @param name The new name of the experiment.
      */
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return this.getName();
    }

    @Override
    public void writeToJSON(JsonWriter jsonWriter) throws IOException
    {
        this.writeIdToJSON( jsonWriter );
        jsonWriter.name( Orm.FIELD_NAME ).value( this.getName() );
        jsonWriter.name( Orm.FIELD_RANDOM ).value( this.isRandom() );

        jsonWriter.name( Orm.FIELD_GROUPS ).beginArray();
        for(Group grp: this.groups) {
            jsonWriter.beginObject();
            grp.writeToJSON( jsonWriter );
            jsonWriter.endObject();
        }
        jsonWriter.endArray();
    }

    public File[] collectMediaFiles()
    {
        ArrayList<File> toret = new ArrayList<>();

        for(Group grp: this.groups) {
            if ( grp instanceof MediaGroup ) {
                final MediaGroup mgrp = (MediaGroup) grp;

                for(MediaGroup.MediaActivity act: mgrp.get()) {
                    toret.add( act.getFile() );
                }
            }
        }

        return toret.toArray( new File[ toret.size() ] );
    }

    /** Creates a new experiment, from JSON data.
     * @param rd The reader from which to extract data.
     * @throws JSONException if data is not found or invalid.
     */
    public static Experiment fromJSON(Reader rd) throws JSONException
    {
        TypeId typeId = null;
        String name = null;
        Id id = null;
        boolean rnd = false;
        JsonReader jsonReader = new JsonReader( rd );
        ArrayList<Group> groups = new ArrayList<>();

        // Load data
        try {
            jsonReader.beginObject();
            while ( jsonReader.hasNext() ) {
                final String token = jsonReader.nextName();

                if ( token.equals( Orm.FIELD_NAME ) ) {
                    name = jsonReader.nextString();
                }
                else
                if ( token.equals( Orm.FIELD_TYPE_ID ) ) {
                    try {
                        typeId = TypeId.parse( jsonReader.nextString() );
                    } catch (IllegalArgumentException exc)
                    {
                        throw new JSONException( "Experiment.fromJSON():" + exc.getMessage() );
                    }
                }
                else
                if ( token.equals( Id.FIELD) ) {
                    id = readIdFromJSON( jsonReader );
                }
                else
                if ( token.equals( Orm.FIELD_RANDOM ) ) {
                    rnd = jsonReader.nextBoolean();
                }
                else
                if ( token.equals( Orm.FIELD_GROUPS ) ) {
                    jsonReader.beginArray();
                    while( jsonReader.hasNext() ) {
                        groups.add( Group.fromJSON( jsonReader ) );
                    }
                    jsonReader.endArray();
                }
            }
        } catch(IOException exc)
        {
            Log.e( LogTag, "Creating experiment from JSON: " + exc.getMessage() );
        }

        // Chk
        if ( typeId == null
          || id == null
          || name == null )
        {
            final String msg = "Creating experiment from JSON: invalid or missing data.";

            Log.e( LogTag, msg );
            throw new JSONException( msg );
        }

        final Experiment toret = new Experiment( id, name, rnd );
        toret.replaceGroups( groups.toArray( new Group[ groups.size() ] ) );
        return toret;
    }

    public Experiment copy()
    {
        return this.copy( this.getId().copy() );
    }

    public Experiment copy(Id id)
    {
        Experiment toret = new Experiment( id, this.getName(), this.isRandom() );

        toret.replaceGroups( this.copyGroups() );
        return toret;
    }

    /** Copies the activities in this group.
     * @return a new vector with copied activities.
     */
    public Group[] copyGroups()
    {
        final Group[] myGroups = this.getGroups();
        final Group[] toret = new Group[ myGroups.length ];

        for(int i = 0; i < toret.length; ++i) {
            toret[ i ] = myGroups[ i ].copy();
        }

        return toret;
    }

    private ArrayList<Group> groups;
    private boolean random;
    private String name;
}

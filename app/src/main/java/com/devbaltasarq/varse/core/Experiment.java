// Varse (c) 2019/20 - 25 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.core;


import androidx.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.core.experiment.ManualGroup;
import com.devbaltasarq.varse.core.experiment.MediaGroup;
import com.devbaltasarq.varse.core.experiment.Tag;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


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

        this.groups = new ArrayList<>();
        this.random = rnd;
        this.name = name;
    }

    @Override
    public Experiment getExperimentOwner()
    {
        return this;
    }

    @Override
    public int hashCode()
    {
        int toret = 13 * this.getName().hashCode();

        toret += 17 * Boolean.valueOf( this.isRandom() ).hashCode();

        int grpsHashCode = 0;
        for(Group<? extends Group.Activity> grp: this.groups) {
            grpsHashCode += grp.hashCode();
        }

        return toret + ( 29 * grpsHashCode );
    }

    @Override
    public boolean equals(Object o)
    {
        boolean toret = false;

        if ( o instanceof final Experiment EXPR) {
            final List<Group<? extends Group.Activity>> EXPR_GROUPS = EXPR.getGroups();

            toret = this.getName().equals( EXPR.getName() )
                 && this.isRandom() == EXPR.isRandom()
                 && this.groups.size() == EXPR_GROUPS.size();


            if ( toret ) {
                for(int i = 0; i < EXPR_GROUPS.size(); ++i) {
                    if ( !this.groups.get( i ).equals( EXPR_GROUPS.get( i ) ) ) {
                        toret = false;
                        break;
                    }
                }
            }
        }

        return toret;
    }

    @Override
    public void updateIds()
    {
        super.updateIds();

        for(Group<? extends Group.Activity> grp: this.groups) {
            grp.updateIds();
        }

        return;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    /** @return the total number of activities in this experiment. */
    public int getNumActivities()
    {
        int toret = 0;

        for(Group<? extends Group.Activity> g: this.groups) {
            toret += g.size();
        }

        return toret;
    }

    @Override
    public TypeId getTypeId()
    {
        return TypeId.Experiment;
    }

    /** @return The media groups involved in the experiment. */
    public List<Group<? extends Group.Activity>> getGroups()
    {
        return new ArrayList<>( this.groups );
    }

    /** Replaces the media groups of the experiments with new ones. */
    public void replaceGroups(final List<Group<? extends Group.Activity>> GRPS)
    {
        this.groups.clear();
        this.groups.addAll( GRPS );
        this.groups.forEach( grp -> grp.setExperiment( this ) );
    }

    /** Removes a given group from the media group list. */
    public void removeGroup(Group<? extends Group.Activity> grp)
    {
        this.groups.remove( grp );
    }

    /** Swaps a given media file with the previous one. */
    public void sortGroupUp(Group<? extends Group.Activity> g)
    {
        final int POS = this.groups.indexOf( g );

        // Nothing to do if not found or is the first one.
        if ( POS >= 1 ) {
            Group<? extends Group.Activity> backUp = this.groups.get( POS - 1 );
            this.groups.set( POS - 1, this.groups.get( POS ) );
            this.groups.set( POS, backUp );
        }

        return;
    }

    /** Swaps a given media file with the previous one. */
    public void sortGroupDown(Group<? extends Group.Activity> g)
    {
        final int LENGTH = this.groups.size();
        final int POS = this.groups.indexOf( g );

        // Nothing to do if not found or is the last one.
        if ( POS < ( LENGTH - 1 ) ) {
            Group<? extends Group.Activity> backUp = this.groups.get( POS + 1 );
            this.groups.set( POS + 1, this.groups.get( POS ) );
            this.groups.set( POS, backUp );
        }

        return;
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
        for(Group<? extends Group.Activity> g: this.groups) {
            if ( g instanceof final MediaGroup MG) {
                for( MediaGroup.MediaActivity act: MG.get() ) {
                    if ( act.getFile().getName().equals( fileName ) ) {
                        toret = act;
                        break SEARCH;
                    }
                }
            }
        }

        return toret;
    }

    /** Adds a new group to the groups list. */
    public void addGroup(Group<? extends Group.Activity> group)
    {
        group.setExperiment( this );
        this.groups.add( group );
    }

    /** Substitute a given group.
     * @param group The group to substitute.
     */
    public void substituteGroup(Group<? extends Group.Activity> group)
    {
        group.setExperiment( this );
        this.removeGroup( group );
        this.addGroup( group );
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

    /** @return the time needed to perform this experiment.
      *  @see Duration
      */
    public Duration calculateTimeNeeded()
    {
        int toret = 0;

        for (Group<? extends Group.Activity> g: this.groups) {
            toret += g.calculateTimeNeeded().getTimeInSeconds();
        }

        return new Duration( toret );
    }

    @Override @NonNull
    public String toString()
    {
        return this.getName();
    }

    @Override
    public List<File> enumerateMediaFiles()
    {
        final List<File> TORET = new ArrayList<>();

        for(Group<? extends Group.Activity> grp: this.getGroups()) {
            TORET.addAll( grp.enumerateMediaFiles() );
        }

        return TORET;
    }

    @Override
    public void writeToJSON(JsonWriter jsonWriter) throws IOException
    {
        this.writeIdToJSON( jsonWriter );
        jsonWriter.name( Ofm.FIELD_NAME ).value( this.getName() );
        jsonWriter.name( Ofm.FIELD_RANDOM ).value( this.isRandom() );

        jsonWriter.name( Ofm.FIELD_GROUPS ).beginArray();
        for(Group<? extends Group.Activity> grp: this.groups) {
            jsonWriter.beginObject();
            grp.writeToJSON( jsonWriter );
            jsonWriter.endObject();
        }
        jsonWriter.endArray();
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
        final JsonReader JSON_READER = new JsonReader( rd );
        final List<Group<? extends Group.Activity>> GROUPS = new ArrayList<>();

        // Load data
        try {
            JSON_READER.setLenient( true );
            JSON_READER.beginObject();
            while ( JSON_READER.hasNext() ) {
                final String TOKEN = JSON_READER.nextName();

                switch ( TOKEN ) {
                    case Ofm.FIELD_NAME -> name = JSON_READER.nextString();
                    case Ofm.FIELD_TYPE_ID -> {
                        try {
                            typeId = TypeId.parse( JSON_READER.nextString() );
                        } catch (IllegalArgumentException exc) {
                            throw new JSONException("Experiment.fromJSON():" + exc.getMessage());
                        }
                    }
                    case Id.FIELD -> id = readIdFromJSON(JSON_READER);
                    case Ofm.FIELD_RANDOM -> rnd = JSON_READER.nextBoolean();
                    case Ofm.FIELD_GROUPS -> {
                        JSON_READER.beginArray();
                        while ( JSON_READER.hasNext() ) {
                            GROUPS.add( Group.fromJSON( JSON_READER ) );
                        }
                        JSON_READER.endArray();
                    }
                    default -> JSON_READER.skipValue();
                }
            }
        } catch(IOException exc) {
            Log.e( LogTag, "Creating experiment from JSON: " + exc.getMessage() );
        }

        // Chk
        if ( typeId == null
          || id == null
          || name == null )
        {
            final String MSG = "Creating experiment from JSON: invalid or missing data.";

            Log.e( LogTag, MSG );
            throw new JSONException( MSG );
        }

        final Experiment TORET = new Experiment( id, name, rnd );
        TORET.replaceGroups( GROUPS );
        return TORET;
    }

    public Experiment copy()
    {
        return this.copy( this.getId().copy() );
    }

    public Experiment copy(Id id)
    {
        Experiment toret = new Experiment( id, this.getName(), this.isRandom() );

        toret.replaceGroups( this.getGroups() );
        return toret;
    }

    /** Creates a new experiment of DT duration with a manual activity.
      * @param DT the duration for the new experiment.
      * @return a new experiment.
      */
    public static Experiment createSimpleExperiment(final Duration DT)
    {
        final String EXPR_NAME = "simple manual " + DT.getTimeInSeconds();
        final Experiment TORET = new Experiment( Id.create(), EXPR_NAME, false );

        final ManualGroup.ManualActivity MANUAL_ACTIVITY =
                new ManualGroup.ManualActivity( Id.create(),
                                                new Tag( "manual " + DT.getTimeInSeconds() ),
                                                DT );
        final ManualGroup MANUAL_GRP =
                new ManualGroup( Id.create(),
                                false,
                                TORET,
                                Collections.singletonList( MANUAL_ACTIVITY ) );

        TORET.groups.add( MANUAL_GRP );
        return TORET;
    }

    private final List<Group<? extends Group.Activity>> groups;
    private boolean random;
    private String name;
}

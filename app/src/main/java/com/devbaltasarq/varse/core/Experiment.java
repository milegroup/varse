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
import java.util.Arrays;
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

        this.name = name;
        this.groups = new ArrayList<>();
        this.random = rnd;
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

        if ( o instanceof Experiment ) {
            final Experiment EXPR = (Experiment) o;
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
    public void replaceGroups(List<Group<? extends Group.Activity>> files)
    {
        for(Group<? extends Group.Activity> grp: this.groups) {
            this.removeGroup( grp );
        }

        this.groups.clear();
        this.groups.addAll( files );

        for(Group<? extends Group.Activity> grp: this.groups) {
            grp.setExperiment( this );
        }
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

    /** @return A MediaGroup instance, given its tag, null if not found. */
    public MediaGroup locateMediaGroup(Tag tag)
    {
        MediaGroup toret = null;

        for(Group<? extends Group.Activity> g: this.groups) {
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
        for(Group<? extends Group.Activity> g: this.groups) {
            if ( g instanceof MediaGroup ) {
                final MediaGroup MG = (MediaGroup) g;

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
        this.groups.add( group );
        group.setExperiment( this );
    }

    /** Substitute a given group.
     * @param group The group to substitute.
     */
    public void substituteGroup(Group<? extends Group.Activity> group)
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
    public File[] enumerateMediaFiles()
    {
        ArrayList<File> toret = new ArrayList<>();

        for(Group<? extends Group.Activity> grp: this.getGroups()) {
            toret.addAll( Arrays.asList( grp.enumerateMediaFiles() ) );
        }

        return toret.toArray( new File[ 0 ] );
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
        JsonReader jsonReader = new JsonReader( rd );
        ArrayList<Group<? extends Group.Activity>> groups = new ArrayList<>();

        // Load data
        try {
            jsonReader.beginObject();
            while ( jsonReader.hasNext() ) {
                final String TOKEN = jsonReader.nextName();

                if ( TOKEN.equals( Ofm.FIELD_NAME ) ) {
                    name = jsonReader.nextString();
                }
                else
                if ( TOKEN.equals( Ofm.FIELD_TYPE_ID ) ) {
                    try {
                        typeId = TypeId.parse( jsonReader.nextString() );
                    } catch (IllegalArgumentException exc)
                    {
                        throw new JSONException( "Experiment.fromJSON():" + exc.getMessage() );
                    }
                }
                else
                if ( TOKEN.equals( Id.FIELD) ) {
                    id = readIdFromJSON( jsonReader );
                }
                else
                if ( TOKEN.equals( Ofm.FIELD_RANDOM ) ) {
                    rnd = jsonReader.nextBoolean();
                }
                else
                if ( TOKEN.equals( Ofm.FIELD_GROUPS ) ) {
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
            final String MSG = "Creating experiment from JSON: invalid or missing data.";

            Log.e( LogTag, MSG );
            throw new JSONException( MSG );
        }

        final Experiment TORET = new Experiment( id, name, rnd );
        TORET.replaceGroups( groups );
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

    private final ArrayList<Group<? extends Group.Activity>> groups;
    private boolean random;
    private String name;
}

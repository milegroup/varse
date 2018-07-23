package com.devbaltasarq.varse.core;


import android.util.JsonWriter;

import org.json.JSONException;

import java.io.File;
import java.io.Reader;

/** Represents the results of a given experiment. */
public class Result extends Persistent {
    public Result(Id id, User usr, Experiment expr)
    {
        super( id );
        this.user = usr;
        this.experiment = expr;
    }

    @Override
    public TypeId getTypeId()
    {
        return TypeId.Result;
    }

    /** @return the user the results belong to. */
    public User getUser()
    {
        return this.user;
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
        return ( 15 * this.getUser().hashCode() ) + ( 17 * this.getExperiment().hashCode() );
    }

    @Override
    public boolean equals(Object o)
    {
        boolean toret = false;

        if ( o instanceof Result ) {
            Result ro = (Result) o;

            if ( this.getUser().equals( ro.getUser() )
              && this.getExperiment().equals( ro.getExperiment() ) )
            {
                toret = true;
            }
        }

        return toret;
    }

    @Override
    public File[] enumerateAssociatedFiles()
    {
        return new File[ 0 ];
    }

    @Override
    public void writeToJSON(JsonWriter jsonWriter)
    {
        throw new Error( "not implemented yet" );
    }

    public static Result fromJSON(Reader reader) throws JSONException
    {
        Result toret = null;

        throw new Error( "not implemented yet" );

        //return toret;
    }

    private User user;
    private Experiment experiment;
}

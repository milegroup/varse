package com.devbaltasarq.varse.core.ofmcache;


import com.devbaltasarq.varse.core.Id;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/** Stores the results for a given experiment. */
public class ResultsPerExperiment {
    public ResultsPerExperiment()
    {
        this.resultsPerExperiment = new HashMap<>();
    }

    /** Adds a results file to a given experiment.
      * The experiment may have or may have not previous results assigned.
      * @param idExpr the id of the experiment.
      * @param f the file of the result.
      * @see com.devbaltasarq.varse.core.Experiment
      * @see com.devbaltasarq.varse.core.Result
      */
    public void add(Id idExpr, File f)
    {
        FileCache exprFileCache = this.resultsPerExperiment.get( idExpr );

        if ( exprFileCache == null ) {
            exprFileCache = new FileCache();
            this.resultsPerExperiment.put( idExpr, exprFileCache );
        }

        exprFileCache.add( f );
    }

    /** @return Gets all result files related to a given experiment.
      * @param idExpr the id of the experiment.
      */
    public File[] get(Id idExpr)
    {
        final FileCache RESULT_FILES = this.resultsPerExperiment.get( idExpr );
        File[] toret;

        if ( RESULT_FILES != null ) {
            toret = RESULT_FILES.getValues();
        } else {
            toret = new File[ 0 ];
        }

        return toret;
    }

    /** Removes a result file from the cache of an experiment.
      * @param idExpr the id of the experiment.
      * @param f the file of the result file.
      */
    public void removeFor(Id idExpr, File f)
    {
        final FileCache RESULT_FILES = this.resultsPerExperiment.get( idExpr );

        if ( RESULT_FILES != null ) {
            RESULT_FILES.remove( f );
        }

        return;
    }

    /** Removes the file cache of results for a given experiment.
      * @param idExpr the id of the experiment.
      */
    public void remove(Id idExpr)
    {
        this.resultsPerExperiment.remove( idExpr );
    }

    /** Clears all entries. */
    public void clear()
    {
        this.resultsPerExperiment.clear();
    }

    private final Map<Id, FileCache> resultsPerExperiment;
}

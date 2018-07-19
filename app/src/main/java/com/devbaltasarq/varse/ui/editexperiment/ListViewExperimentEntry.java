package com.devbaltasarq.varse.ui.editexperiment;

import com.devbaltasarq.varse.core.Experiment;

/** Represents a single entry in the media files list view. */
public class ListViewExperimentEntry {
    public ListViewExperimentEntry(Experiment e)
    {
        this.experiment = e;
    }

    /** @return the experiment in this entry. */
    public Experiment getExperiment()
    {
        return this.experiment;
    }

    /** Changes the experiment in this entry.
      * @param expr the experiment to set.
      */
    public void setExperiment(Experiment expr)
    {
        this.experiment = expr;
    }

    /** @return a shortcut to the experiment description. */
    public String getExperimentDesc()
    {
        return this.experiment.toString();
    }

    private Experiment experiment;
}

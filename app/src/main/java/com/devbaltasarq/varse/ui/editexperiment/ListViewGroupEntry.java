package com.devbaltasarq.varse.ui.editexperiment;

import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.experiment.Group;

/** Represents a single entry in the group files list view. */
public class ListViewGroupEntry {
    public ListViewGroupEntry(Experiment e, Group g)
    {
        this.experiment = e;
        this.group = g;
    }

    public Experiment getExperiment()
    {
        return this.experiment;
    }

    public Group getGroup()
    {
        return this.group;
    }

    public String getGroupDesc()
    {
        return this.group.toString();
    }

    private Experiment experiment;
    private Group group;
}

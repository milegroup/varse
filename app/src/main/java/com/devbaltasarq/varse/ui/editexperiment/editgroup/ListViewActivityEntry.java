package com.devbaltasarq.varse.ui.editexperiment.editgroup;

import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.core.experiment.MediaGroup;

/** Represents a single entry in the group files list view. */
public class ListViewActivityEntry {
    public ListViewActivityEntry(Group.Activity act)
    {
        this.activity = act;
    }

    public Group getGroup()
    {
        return this.activity.getGroup();
    }

    public Group.Activity getActivity()
    {
        return this.activity;
    }

    public String getFileName()
    {
        String toret = this.activity.toString();

        if ( this.activity instanceof MediaGroup.MediaActivity ) {
            MediaGroup.MediaActivity mact = (MediaGroup.MediaActivity) this.activity;

            toret = mact.getFile().getName();
        }

        return toret;
    }

    private Group.Activity activity;
}

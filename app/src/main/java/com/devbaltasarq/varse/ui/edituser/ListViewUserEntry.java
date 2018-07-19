package com.devbaltasarq.varse.ui.edituser;

import com.devbaltasarq.varse.core.User;

/** Represents a single entry in the media files list view. */
public class ListViewUserEntry {
    public ListViewUserEntry(User e)
    {
        this.user = e;
    }

    public User getUser()
    {
        return this.user;
    }

    public String getUserDesc()
    {
        return this.user.getName();
    }

    private User user;
}

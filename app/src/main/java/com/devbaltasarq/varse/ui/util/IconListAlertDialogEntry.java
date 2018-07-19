package com.devbaltasarq.varse.ui.util;

import android.graphics.drawable.Drawable;


/** Represents a single entry in the group files list view. */
public class IconListAlertDialogEntry {
    public IconListAlertDialogEntry(Drawable icon, String choice)
    {
        this.icon = icon;
        this.choice = choice;
    }

    /** @return The icon of this entry. */
    public Drawable getIcon()
    {
        return this.icon;
    }

    /** @return The string of this choice. */
    public String getChoice()
    {
        return this.choice;
    }

    private Drawable icon;
    private String choice;
}

package com.devbaltasarq.varse.ui.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Represents an adapter of the special items for the ListView of media files. */
public class IconListAlertDialogEntryArrayAdapter extends ArrayAdapter<IconListAlertDialogEntry> {
    public IconListAlertDialogEntryArrayAdapter(Context cntxt, IconListAlertDialogEntry[] entries)
    {
        super( cntxt, 0, entries );
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final Context cntxt = this.getContext();
        final IconListAlertDialogEntry entry = this.getItem( position );

        // Create view
        LinearLayout hrzLayout = new LinearLayout( cntxt );
        hrzLayout.setPadding( 5, 5, 5, 5 );
        hrzLayout.setOrientation( LinearLayout.HORIZONTAL );
        hrzLayout.setLayoutParams( new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT ) );

        final TextView lblChoice = new TextView( cntxt );
        final ImageView ivIcon = new ImageView( cntxt );

        lblChoice.setLayoutParams( new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        0.2f
        ));

        lblChoice.setPadding( 5, 5, 5, 5 );

        ivIcon.setLayoutParams( new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.8f
        ));
        ivIcon.setPadding( 5, 5, 5, 5 );

        hrzLayout.addView( ivIcon );
        hrzLayout.addView( lblChoice );
        convertView = hrzLayout;

        // Assign values
        if ( entry != null ) {
            lblChoice.setText( entry.getChoice() );
            ivIcon.setImageDrawable( entry.getIcon() );
        }

        return convertView;
    }
}

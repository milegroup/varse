package com.devbaltasarq.varse.ui.util;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.widget.AdapterView;
import android.widget.ListView;


/** An AlertDialog with a list of choices, each one with an icon. */
public class IconListAlertDialog extends AlertDialog {
    /** Creates a basic IconListAlertDialog. */
    public IconListAlertDialog(Context cntxt, int[] choiceIds, int[] drawableIds)
    {
        this( cntxt,
                android.R.drawable.ic_dialog_alert, android.R.string.dialog_alert_title,
                choiceIds, drawableIds );
    }

    /** Creates a complete IconListAlertDialog. */
    public IconListAlertDialog(Context cntxt, int iconResId, int titleResId,
                               int[] drawableIds, int[] choiceIds)
    {
        super( cntxt, true, null );

        final int NUM_CHOICES = choiceIds.length;

        assert NUM_CHOICES == drawableIds.length:
                "IconListAlertDialog: different number of choices and icons.";

        // Initial touches
        this.setTitle( titleResId );
        this.setIcon( AppCompatResources.getDrawable( cntxt, iconResId ) );
        this.setButton( BUTTON_NEGATIVE, "Cancel", (OnClickListener) null );

        // Set view
        this.lvChoices = new ListView( cntxt );
        this.setView( lvChoices );

        // Setup
        this.entries = new IconListAlertDialogEntry[ NUM_CHOICES ];

        for(int i = 0; i < NUM_CHOICES; ++i) {
            int icon = drawableIds[ i ];
            int choice = choiceIds[ i ];

            if ( icon < 0
              && choice < 0 )
            {

                continue;
            }

            if ( icon < 0 ) {
                icon = android.R.drawable.btn_dialog;
            }

            if ( choice < 0 ) {
                choice = android.R.string.ok;
            }

            this.entries[ i ] = new IconListAlertDialogEntry(
                                        AppCompatResources.getDrawable( cntxt, icon ),
                                        cntxt.getString( choice ) );
        }

        // Setup list contents
        lvChoices.setAdapter( new IconListAlertDialogEntryArrayAdapter( cntxt, this.entries ) );
    }

    /** Sets the listener for selected items
      * @param selection The listener.
      * @see AdapterView<?>.OnItemClickListener
      */
    public void setItemClickListener(AdapterView.OnItemClickListener selection)
    {
        this.lvChoices.setOnItemClickListener( selection );
    }


    private IconListAlertDialogEntry[] entries;
    private ListView lvChoices;
}

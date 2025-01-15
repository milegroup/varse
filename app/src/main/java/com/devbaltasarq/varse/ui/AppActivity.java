package com.devbaltasarq.varse.ui;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.Spinner;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.experiment.Tag;

/** This activity has the capability of showing status messages through snackbars. */
public abstract class AppActivity extends AppCompatActivity {
    public static final String LOG_TAG = AppActivity.class.getSimpleName();
    public static final int RSC_SAVE_DATA = 0x5a7eda7a;
    public static final int RSC_DISMISS_DATA = 0x10dda7a;

    /** Interface for methods that handle the answer of a dialog. */
    public interface DialogAnswerHandler {
        /** Answers the result of a Yes/No dialog.
          * @param i The actions to take when the yes or no buttons are tapped.
          *                      (i) -> {...} i can be:
          *                          - DialogInterface.BUTTON_POSITIVE
          *                          - DialogInterface.BUTTON_NEGATIVE
          */
        void answer(int i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );

        this.setTitle( "" );
    }

    /** Shows an info status on screen. */
    protected void showStatus(String LogTag, String msg)
    {
        Log.i( LogTag, msg );

        AppActivity.this.runOnUiThread( () -> {
            Snackbar.make(
                    findViewById( android.R.id.content ), msg, Snackbar.LENGTH_SHORT )
                    .show();
        });
    }

    /** Builds a dialog with the "Are you sure?" message, and yes or no buttons.
      * The method to manage the answer of the dialog is answerAreYouSureDialog(int i).
      * @return The created AlertDialog, ready to be shown.
      */
    protected AlertDialog buildYouSureDialog(String explanation)
    {
        return this.buildYouSureDialog( explanation, (int i) -> this.answerAreYouSureDialog( i ) );
    }

    /** Builds a dialog with the "Are you sure?" message, and yes or no buttons.
      * @param dlgAnswerHandleMth The actions to take when the yes or no buttons are tapped.
      *                      (i) -> {...} i can be:
      *                          - DialogInterface.BUTTON_POSITIVE
      *                          - DialogInterface.BUTTON_NEGATIVE
      * @return The created AlertDialog, ready to be shown.
      * @see AlertDialog
      */
    protected AlertDialog buildYouSureDialog(String explanation, DialogAnswerHandler dlgAnswerHandleMth)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );

        builder.setTitle( R.string.msgAreYouSure );
        builder.setMessage( explanation );
        builder.setPositiveButton( R.string.lblYes, (dlg, i) -> dlgAnswerHandleMth.answer( i ) );
        builder.setNegativeButton( R.string.lblNo, (dlg, i) -> dlgAnswerHandleMth.answer( i ) );

        return builder.create();
    }

    /** Contains defaults answers for the Are you sure? Dialog.
      * @param i can be:
      *         - DialogInterface.BUTTON_POSITIVE
      *         - DialogInterface.BUTTON_NEGATIVE
      */
    protected void answerAreYouSureDialog(int i)
    {
        if ( i == DialogInterface.BUTTON_POSITIVE ) {
            super.finish();
        }

        return;
    }

    /** Determines whether this activity should ask about being sure to leave or not.
      * @return true if it should ask before leaving, false otherwise.
      */
    public boolean askBeforeLeaving()
    {
        return true;
    }

    protected void finishWithResultCode(int resultCode)
    {
        this.setResult( resultCode );

        if ( resultCode == RSC_SAVE_DATA ) {
            super.finish();
        } else {
            this.finish();
        }

        return;
    }

    @Override
    public void finish()
    {
        if ( this.askBeforeLeaving() ) {
            final AlertDialog exitDlg = this.buildYouSureDialog( this.getString( R.string.msgLoseData ) );

            exitDlg.show();
        } else {
            super.finish();
        }

        return;
    }

    /** Given a duration, sets the time in the spinner (units) and textview (amount).
     * @param d the duration to represent.
     * @param spUnit The spinner in which to set minutes or seconds.
     * @param edTime The textview in which to set the amount of time.
     */
    protected static void fillInDurationInUI(Duration d, Spinner spUnit, EditText edTime)
    {
        int amount = d.getSeconds();

        // Set the time units (seconds by default)
        spUnit.setSelection( Duration.TimeUnit.Seconds.ordinal() );

        if ( d.getMinutes() > 0 ) {
            // Set the time in minutes
            spUnit.setSelection( Duration.TimeUnit.Minutes.ordinal() );
            amount = d.getMinutes();
        }

        // Put the amount of time (in seconds or minutes)
        edTime.setText( Integer.toString( amount ) );
    }

    protected static void fillInDurationInObj(Duration d, Spinner spUnit, EditText edTime)
    {
        try {
            d.parse( spUnit.getSelectedItemPosition(),  edTime.getText().toString() );
        } catch(NumberFormatException exc)
        {
            Log.e(LOG_TAG, "converting duration: " + exc.getMessage() );
        }

        return;
    }

    protected static void fillInTagObj(@NonNull Tag tag, @NonNull String newTag)
    {
        if ( newTag != null
          && !newTag.isEmpty() )
        {
            tag.set( newTag );
        }
    }
}

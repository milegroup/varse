package com.devbaltasarq.varse.ui.showresult;

import android.content.Context;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Result;

import java.util.Calendar;

/** Represents a single entry in the media files list view. */
public class ListViewResultEntry {
    public ListViewResultEntry(Result res)
    {
        this.result = res;
    }

    /** @return the result in this entry. */
    public Result getResult()
    {
        return this.result;
    }

    /** Changes the result in this entry.
      * @param res the result to set.
      */
    public void setResult(Result res)
    {
        this.result = res;
    }

    /** @return a shortcut to the result description. */
    public String getResultDesc(Context context)
    {
        final Result result = this.getResult();
        final Calendar localDate = Calendar.getInstance();

        localDate.setTimeInMillis( result.getTime() );
        return String.format(   "%04d-%02d-%02d %02d:%02d:%02d "
                        + " " + context.getString( R.string.lblRecord )
                        + ": " + result.getUser().getName(),
                localDate.get( Calendar.YEAR ),
                localDate.get( Calendar.MONTH ) + 1,
                localDate.get( Calendar.DAY_OF_MONTH ),
                localDate.get( Calendar.HOUR_OF_DAY ),
                localDate.get( Calendar.MINUTE ),
                localDate.get( Calendar.SECOND )
        );
    }

    private Result result;
}

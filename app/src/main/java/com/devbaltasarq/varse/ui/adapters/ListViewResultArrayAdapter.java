package com.devbaltasarq.varse.ui.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Result;
import com.devbaltasarq.varse.ui.ResultsActivity;

import java.util.Calendar;
import java.util.Locale;

/** Represents an adapter of the special items for the ListView of media files. */
public class ListViewResultArrayAdapter extends ArrayAdapter<Result> {
    public ListViewResultArrayAdapter(Context cntxt, Result[] entries)
    {
        super( cntxt, 0, entries );
    }

    @Override
    public @NonNull View getView(int position, View rowView, @NonNull ViewGroup parent)
    {
        final ResultsActivity cntxt = (ResultsActivity) this.getContext();
        final LayoutInflater layoutInflater = LayoutInflater.from( this.getContext() );
        final Result res = this.getItem( position );

        if ( res == null ) {
            throw new Error( "Result adapter: result is null" );
        }

        if ( rowView == null ) {
            rowView = layoutInflater.inflate( R.layout.listview_result_entry, null );
        }

        final ImageButton btExportResult = rowView.findViewById( R.id.btExportResult );
        final ImageButton btDeleteResult = rowView.findViewById( R.id.btDeleteResult );
        final ImageButton btUploadResult = rowView.findViewById( R.id.btUploadResult );
        final ImageButton btShowResults = rowView.findViewById( R.id.btShowResult );
        final TextView lblExperimentDesc = rowView.findViewById( R.id.lblResultDesc );

        lblExperimentDesc.setText( this.getResultDesc( cntxt, res ) );

        btDeleteResult.setOnClickListener( (v) -> cntxt.deleteResult( res ) );
        btExportResult.setOnClickListener( (v) -> cntxt.exportResult( res ) );
        btUploadResult.setOnClickListener( (v) -> cntxt.uploadResult( res ) );
        btShowResults.setOnClickListener( (v) -> cntxt.showResults( res ) );

        return rowView;
    }

    /** @return a shortcut to the result description. */
    private String getResultDesc(Context context, Result result)
    {
        final Calendar localDate = Calendar.getInstance();

        localDate.setTimeInMillis( result.getTime() );
        return String.format( Locale.getDefault(),
                        "%04d-%02d-%02d %02d:%02d:%02d "
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
}

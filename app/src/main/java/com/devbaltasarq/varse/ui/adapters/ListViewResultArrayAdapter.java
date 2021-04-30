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

/** Represents an adapter of the special items for the ListView of Result objects. */
public class ListViewResultArrayAdapter extends ArrayAdapter<Result> {
    public ListViewResultArrayAdapter(Context cntxt, Result[] entries)
    {
        super( cntxt, 0, entries );
    }

    @Override
    public @NonNull View getView(int position, View rowView, @NonNull ViewGroup parent)
    {
        final ResultsActivity CONTEXT = (ResultsActivity) this.getContext();
        final LayoutInflater LAYOUT_INFLATER = LayoutInflater.from( this.getContext() );
        final Result RES = this.getItem( position );

        if ( RES == null ) {
            throw new Error( "Result adapter: result is null" );
        }

        if ( rowView == null ) {
            rowView = LAYOUT_INFLATER.inflate( R.layout.listview_result_entry, null );
        }

        final ImageButton BT_EXPORT_RESULT = rowView.findViewById( R.id.btExportResult );
        final ImageButton BT_DELETE_RESULT = rowView.findViewById( R.id.btDeleteResult );
        final ImageButton BT_UPLOAD_RESULT = rowView.findViewById( R.id.btUploadResult );
        final ImageButton BT_SHOW_RESULTS = rowView.findViewById( R.id.btShowResult );
        final TextView LBL_EXPERIMENT_DESC = rowView.findViewById( R.id.lblResultDesc );

        LBL_EXPERIMENT_DESC.setText( this.getResultDesc( CONTEXT, RES ) );

        BT_DELETE_RESULT.setOnClickListener( (v) -> CONTEXT.deleteResult( RES ) );
        BT_EXPORT_RESULT.setOnClickListener( (v) -> CONTEXT.exportResult( RES ) );
        BT_UPLOAD_RESULT.setOnClickListener( (v) -> CONTEXT.uploadResult( RES ) );
        BT_SHOW_RESULTS.setOnClickListener( (v) -> CONTEXT.showResults( RES ) );

        return rowView;
    }

    /** @return a shortcut to the result description. */
    private String getResultDesc(Context context, Result result)
    {
        final Calendar LOCAL_DATE = Calendar.getInstance();

        LOCAL_DATE.setTimeInMillis( result.getTime() );
        return String.format( Locale.getDefault(),
                        "%04d-%02d-%02d %02d:%02d:%02d "
                        + " " + context.getString( R.string.lblRecord )
                        + ": " + result.getUser().getName(),
                        LOCAL_DATE.get( Calendar.YEAR ),
                        LOCAL_DATE.get( Calendar.MONTH ) + 1,
                        LOCAL_DATE.get( Calendar.DAY_OF_MONTH ),
                        LOCAL_DATE.get( Calendar.HOUR_OF_DAY ),
                        LOCAL_DATE.get( Calendar.MINUTE ),
                        LOCAL_DATE.get( Calendar.SECOND )
        );
    }
}

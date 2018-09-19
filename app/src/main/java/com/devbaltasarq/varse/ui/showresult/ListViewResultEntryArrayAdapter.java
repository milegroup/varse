package com.devbaltasarq.varse.ui.showresult;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Result;
import com.devbaltasarq.varse.ui.ResultsActivity;

/** Represents an adapter of the special items for the ListView of media files. */
public class ListViewResultEntryArrayAdapter extends ArrayAdapter<ListViewResultEntry> {
    public ListViewResultEntryArrayAdapter(Context cntxt, ListViewResultEntry[] entries)
    {
        super( cntxt, 0, entries );
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final ResultsActivity cntxt = (ResultsActivity) this.getContext();
        final LayoutInflater layoutInflater = LayoutInflater.from( this.getContext() );
        final ListViewResultEntry entry = this.getItem( position );
        final Result res = entry.getResult();

        if ( convertView == null ) {
            convertView = layoutInflater.inflate( R.layout.listview_result_entry, null );
        }

        final ImageButton btExportResult = convertView.findViewById( R.id.btExportResult );
        final ImageButton btDeleteResult = convertView.findViewById( R.id.btDeleteResult );
        final ImageButton btShowResults = convertView.findViewById( R.id.btShowResult);
        final TextView lblExperimentDesc = convertView.findViewById( R.id.lblResultDesc );

        lblExperimentDesc.setText( entry.getResultDesc( cntxt ) );

        btDeleteResult.setOnClickListener( (v) -> cntxt.deleteResult( res ) );
        btExportResult.setOnClickListener( (v) -> cntxt.exportResult( res ) );
        btShowResults.setOnClickListener( (v) -> cntxt.showResults( res ) );

        return convertView;
    }
}

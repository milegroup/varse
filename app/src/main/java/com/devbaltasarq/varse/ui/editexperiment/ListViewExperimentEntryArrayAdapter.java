package com.devbaltasarq.varse.ui.editexperiment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.ui.ExperimentsActivity;

import java.util.ArrayList;

/** Represents an adapter of the special items for the ListView of media files. */
public class ListViewExperimentEntryArrayAdapter extends ArrayAdapter<ListViewExperimentEntry> {
    public ListViewExperimentEntryArrayAdapter(Context cntxt, ArrayList<ListViewExperimentEntry> entries)
    {
        super( cntxt, 0, entries );
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final LayoutInflater layoutInflater = LayoutInflater.from( this.getContext() );
        final ListViewExperimentEntry entry = this.getItem( position );
        final Experiment expr = entry.getExperiment();

        if ( convertView == null ) {
            convertView = layoutInflater.inflate( R.layout.listview_experiment_entry, null );
        }

        final ImageButton btLaunch = convertView.findViewById( R.id.btLaunchExperiment );
        final ImageButton btModify = convertView.findViewById( R.id.btEditUser);
        final ImageButton btDelete = convertView.findViewById( R.id.btDeleteExperiment );
        final ImageButton btExport = convertView.findViewById( R.id.btExportExperiment );
        final TextView lblExperimentDesc = convertView.findViewById( R.id.lblExperimentDesc );
        final ExperimentsActivity cntxt = (ExperimentsActivity) ListViewExperimentEntryArrayAdapter.this.getContext();

        lblExperimentDesc.setText( entry.getExperimentDesc() );

        btLaunch.setOnClickListener( (v) -> cntxt.launchExperiment( expr ) );
        btModify.setOnClickListener( (v) -> cntxt.editExperiment( expr ) );
        btDelete.setOnClickListener( (v) -> cntxt.deleteExperiment( position, expr ) );
        btExport.setOnClickListener( (v) -> cntxt.exportExperiment( expr ) );

        return convertView;
    }
}

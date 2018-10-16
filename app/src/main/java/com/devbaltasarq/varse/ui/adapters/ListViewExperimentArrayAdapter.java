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
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.ui.ExperimentsActivity;

import java.util.ArrayList;

/** Represents an adapter of the special items for the ListView of media files. */
public class ListViewExperimentArrayAdapter extends ArrayAdapter<Experiment> {
    public ListViewExperimentArrayAdapter(Context cntxt, ArrayList<Experiment> entries)
    {
        super( cntxt, 0, entries );
    }

    @Override
    public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent)
    {
        final LayoutInflater layoutInflater = LayoutInflater.from( this.getContext() );
        final Experiment expr = this.getItem( position );

        if ( expr == null ) {
            throw new Error( "Experiment adapter, experiment is null" );
        }

        if ( convertView == null ) {
            convertView = layoutInflater.inflate( R.layout.listview_experiment_entry, null );
        }

        final ImageButton btLaunch = convertView.findViewById( R.id.btLaunchExperiment );
        final ImageButton btModify = convertView.findViewById( R.id.btEditUser);
        final ImageButton btDelete = convertView.findViewById( R.id.btDeleteExperiment );
        final ImageButton btExport = convertView.findViewById( R.id.btExportExperiment );
        final ImageButton btShowResults = convertView.findViewById( R.id.btShowResultsExperiment );
        final TextView lblExperimentDesc = convertView.findViewById( R.id.lblExperimentDesc );
        final ExperimentsActivity cntxt = (ExperimentsActivity) ListViewExperimentArrayAdapter.this.getContext();

        lblExperimentDesc.setText( expr.toString() );

        btLaunch.setOnClickListener( (v) -> cntxt.launchExperiment( expr ) );
        btModify.setOnClickListener( (v) -> cntxt.editExperiment( expr ) );
        btDelete.setOnClickListener( (v) -> cntxt.deleteExperiment( position, expr ) );
        btExport.setOnClickListener( (v) -> cntxt.exportExperiment( expr ) );
        btShowResults.setOnClickListener( (v) -> cntxt.launchExperimentResults( expr ) );

        return convertView;
    }
}

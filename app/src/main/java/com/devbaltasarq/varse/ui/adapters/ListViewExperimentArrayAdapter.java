package com.devbaltasarq.varse.ui.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
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
        final LayoutInflater LAYOUT_INFLATER = LayoutInflater.from( this.getContext() );
        final Experiment EXPR = this.getItem( position );

        if ( EXPR == null ) {
            throw new Error( "Experiment adapter, experiment is null" );
        }

        if ( convertView == null ) {
            convertView = LAYOUT_INFLATER.inflate( R.layout.listview_experiment_entry, null );
        }

        final ImageButton BT_LAUNCH = convertView.findViewById( R.id.btLaunchExperiment );
        final ImageButton BT_MODIFY = convertView.findViewById( R.id.btEditUser);
        final ImageButton BT_DELETE = convertView.findViewById( R.id.btDeleteExperiment );
        final ImageButton BT_EXPORT = convertView.findViewById( R.id.btExportExperiment );
        final ImageButton BT_SHOW_RESULTS = convertView.findViewById( R.id.btShowResultsExperiment );
        final TextView LBL_EXPERIMENT_DESC = convertView.findViewById( R.id.lblExperimentDesc );
        final ExperimentsActivity CONTEXT = (ExperimentsActivity) ListViewExperimentArrayAdapter.this.getContext();

        LBL_EXPERIMENT_DESC.setText( EXPR.toString() );

        BT_LAUNCH.setOnClickListener( (v) -> CONTEXT.launchExperiment( EXPR ) );
        BT_MODIFY.setOnClickListener( (v) -> CONTEXT.editExperiment( EXPR ) );
        BT_DELETE.setOnClickListener( (v) -> CONTEXT.deleteExperiment( position, EXPR ) );
        BT_EXPORT.setOnClickListener( (v) -> CONTEXT.exportExperiment( EXPR ) );
        BT_SHOW_RESULTS.setOnClickListener( (v) -> CONTEXT.launchExperimentResults( EXPR ) );

        return convertView;
    }
}

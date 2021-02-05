package com.devbaltasarq.varse.ui.editexperiment.editgroup;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.experiment.ManualGroup;
import com.devbaltasarq.varse.ui.AppActivity;

public class EditManualEntryActivity extends AppActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView( R.layout.activity_edit_manual_entry );

        final Toolbar TOOLBAR = this.findViewById( R.id.toolbar );
        this.setSupportActionBar( TOOLBAR );

        // Widgets
        final ImageButton BT_BACK = this.findViewById( R.id.btCloseEditManualEntry );
        final FloatingActionButton BT_SAVE = this.findViewById( R.id.fbSaveManualEntry );
        final EditText ED_TAG = this.findViewById( R.id.edTag );
        final EditText ED_DURATION = this.findViewById( R.id.edActDuration );
        final Spinner CB_TIME_UNIT = this.findViewById( R.id.cbTimeUnit );

        // Spinner for time units
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource( this,
                R.array.vTimeUnitChoices, android.R.layout.simple_spinner_item );
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
        CB_TIME_UNIT.setAdapter( adapter );

        BT_BACK.setOnClickListener( (v) -> this.finishWithResultCode( RSC_DISMISS_DATA ) );
        BT_SAVE.setOnClickListener( (v) -> this.finishWithResultCode( RSC_SAVE_DATA ) );

        // Answer to events
        ED_TAG.addTextChangedListener( new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                fillInTagObj( manualActivity.getTag(), editable.toString() );
            }
        });

        CB_TIME_UNIT.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                EditManualEntryActivity.this.writeDurationToManualActivity();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                CB_TIME_UNIT.setSelection( 0 );
            }
        });

        ED_DURATION.addTextChangedListener( new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                EditManualEntryActivity.this.writeDurationToManualActivity();
            }
        });

        this.setTitle( "" );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        final EditText ED_TAG = this.findViewById( R.id.edTag );
        final EditText ED_DURATION = this.findViewById( R.id.edActDuration );
        final Spinner CB_TIME_UNIT = this.findViewById( R.id.cbTimeUnit );

        assert manualActivity != null: "manual activity to edit can't be null";

        ED_TAG.setText( manualActivity.getTag().toString() );
        fillInDurationInUI( manualActivity.getTime(), CB_TIME_UNIT, ED_DURATION );
    }

    /** Sets the duration time. */
    private void writeDurationToManualActivity()
    {
        final EditText ED_DURATION = this.findViewById( R.id.edActDuration );
        final Spinner CB_TIME_UNIT = this.findViewById( R.id.cbTimeUnit );

        fillInDurationInObj( EditManualEntryActivity.manualActivity.getTime(), CB_TIME_UNIT, ED_DURATION );
    }

    public static ManualGroup.ManualActivity manualActivity;
}

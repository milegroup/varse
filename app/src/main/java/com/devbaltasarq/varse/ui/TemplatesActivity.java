package com.devbaltasarq.varse.ui;


import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Ofm;
import com.devbaltasarq.varse.core.Template;


public class TemplatesActivity extends AppActivity {
    public static final String LOG_TAG = TemplatesActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_templates );

        final Toolbar TOOLBAR = this.findViewById( R.id.toolbar );
        this.setSupportActionBar( TOOLBAR );

        final ImageButton BT_CLOSE_TEMPLATES = this.findViewById( R.id.btCloseTemplates );
        final ListView LV_TEMPLATES = this.findViewById( R.id.lvTemplates );

        LV_TEMPLATES.setOnItemClickListener( (adapter, view, pos, t) ->
                TemplatesActivity.this.setTemplate( pos )
        );

        BT_CLOSE_TEMPLATES.setOnClickListener( (v) -> this.finish() );

        this.setTitle( "" );

        this.tags = new Template.TemplateStrings();
        this.tags.set( Template.TemplateStrings.Id.TAG_NEUTRAL,
                       this.getString( R.string.lblNeutral ) );
        this.tags.set( Template.TemplateStrings.Id.TAG_PLEASANT,
                       this.getString( R.string.lblPleasant ) );
        this.tags.set( Template.TemplateStrings.Id.TAG_UNPLEASANT,
                       this.getString( R.string.lblUnpleasant ) );
        this.tags.set( Template.TemplateStrings.Id.MSG_RUFFIUS_PHASE1,
                       this.getString( R.string.msgTemplateRuffiusPhase1 ) );
        this.tags.set( Template.TemplateStrings.Id.MSG_RUFFIUS_PHASE2,
                       this.getString( R.string.msgTemplateRuffiusPhase2 ) );
        this.tags.set( Template.TemplateStrings.Id.MSG_RUFFIUS_PHASE3,
                       this.getString( R.string.msgTemplateRuffiusPhase3 ) );
        this.tags.set( Template.TemplateStrings.Id.MSG_DEFAULT_PHASE1,
                       this.getString( R.string.msgTemplateDefaultPhase1 ) );
        this.tags.set( Template.TemplateStrings.Id.MSG_DEFAULT_PHASE2,
                       this.getString( R.string.msgTemplateDefaultPhase2 ) );
        this.tags.set( Template.TemplateStrings.Id.MSG_DEFAULT_PHASE3,
                       this.getString( R.string.msgTemplateDefaultPhase3 ) );
        this.tags.set( Template.TemplateStrings.Id.MSG_QUICK,
                       this.getString( R.string.msgTemplateQuick ) );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        this.showTemplates();
    }

    private void setTemplate(int pos)
    {
        selectedTemplate = Template.Templates.values()[ pos ].create(
                                                    this,
                                                    Ofm.get(),
                                                    this.tags );

        this.finish();
    }

    private void showTemplates()
    {
        final TextView LBL_NO_ENTRIES = this.findViewById( R.id.lblNoEntries );
        final ListView LV_TEMPLATES = this.findViewById( R.id.lvTemplates );
        final String[] ENTRIES = Template.Templates.toStringArray();

        // Prepare the list view
        LV_TEMPLATES.setAdapter(
                new ArrayAdapter<>(this,
                                    android.R.layout.simple_list_item_1,
                                    ENTRIES )
        );

        // Show the experiments list (or maybe not).
        if ( ENTRIES.length > 0 ) {
            LBL_NO_ENTRIES.setVisibility( View.GONE );
            LV_TEMPLATES.setVisibility( View.VISIBLE );
        } else {
            LBL_NO_ENTRIES.setVisibility( View.VISIBLE );
            LV_TEMPLATES.setVisibility( View.GONE );
        }

        return;
    }

    @Override
    public boolean askBeforeLeaving()
    {
        return false;
    }

    static Template selectedTemplate;
    private Template.TemplateStrings tags;
}

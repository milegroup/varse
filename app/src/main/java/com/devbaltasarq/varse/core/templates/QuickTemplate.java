// Varse (c) 2019/20 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.core.templates;


import android.content.Context;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.Template;
import com.devbaltasarq.varse.core.experiment.ManualGroup;
import com.devbaltasarq.varse.core.experiment.MediaGroup;
import com.devbaltasarq.varse.core.experiment.PictureGroup;
import com.devbaltasarq.varse.core.experiment.Tag;

import java.io.IOException;


public class QuickTemplate extends Template {
    public QuickTemplate(Context cntxt, Orm db, String name, TemplateStrings tags)
    {
        super( cntxt, db, name, tags );
    }

    public Experiment create() throws IOException
    {
        final Orm DB = this.getDb();
        final Experiment EXPR = this.getExperiment();
        final ManualGroup GRP = this.createManualGroup();

        final ManualGroup.ManualActivity ACT = new ManualGroup.ManualActivity(
                Id.create(),
                new Tag( this.getStrs().get( TemplateStrings.MSG_QUICK ) ),
                new Duration( 3, 0 ) );

        GRP.add( ACT );
        EXPR.addGroup( GRP );

        DB.store( EXPR );
        return EXPR;
    }
}

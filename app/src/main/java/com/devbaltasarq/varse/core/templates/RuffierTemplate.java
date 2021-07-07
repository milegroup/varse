// Varse (c) 2019/20 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.core.templates;


import android.content.Context;

import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.Template;
import com.devbaltasarq.varse.core.experiment.ManualGroup;
import com.devbaltasarq.varse.core.experiment.Tag;

import java.io.IOException;


public class RuffierTemplate extends Template {
    public RuffierTemplate(Context cntxt, Orm db, String name, TemplateStrings tags)
    {
        super( cntxt, db, name, tags );
    }

    public Experiment create() throws IOException
    {
        final Orm DB = this.getDb();
        final Experiment EXPR = this.getExperiment();
        final ManualGroup GRP_PREV = this.createManualGroup();
        final ManualGroup GRP_ACTIVITY = this.createManualGroup();
        final ManualGroup GRP_POST = this.createManualGroup();

        final ManualGroup.ManualActivity ACT_PREV = new ManualGroup.ManualActivity(
                Id.create(),
                new Tag( "Stay seated." ),
                new Duration( 1, 0 ) );

        final ManualGroup.ManualActivity ACT_MAIN = new ManualGroup.ManualActivity(
                Id.create(),
                new Tag( "Twenty push ups in one minute." ),
                new Duration( 1, 0 ) );

        final ManualGroup.ManualActivity ACT_POST = new ManualGroup.ManualActivity(
                Id.create(),
                new Tag( "Go back to sitting position." ),
                new Duration( 1, 0 ) );


        GRP_PREV.add( ACT_PREV );
        GRP_ACTIVITY.add( ACT_MAIN );
        GRP_POST.add( ACT_POST );
        EXPR.addGroup( GRP_PREV );
        EXPR.addGroup( GRP_ACTIVITY );
        EXPR.addGroup( GRP_POST );

        DB.store( EXPR );
        return EXPR;
    }
}

// Varse (c) 2019/20 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.core.templates;


import android.content.Context;
import java.io.IOException;

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


public class DefaultTemplate extends Template {
    public DefaultTemplate(Context cntxt, Orm db, String name)
    {
        super( cntxt, db, name );
    }

    public Experiment create() throws IOException
    {
        final Orm DB = this.getDb();
        final Experiment EXPR = this.getExperiment();
        final ManualGroup GRP_POST = this.createManualGroup();
        final ManualGroup GRP_PREV = this.createManualGroup();
        final PictureGroup GRP_MAIN = this.createPictureGroup( "relax",
                                                                new Duration( 3, 0 ) );

        final ManualGroup.ManualActivity ACT_PREV = new ManualGroup.ManualActivity(
                Id.create(),
                new Tag( "Collection will start momentarily." ),
                new Duration( 1, 0 ) );

        final MediaGroup.MediaActivity ACT_MAIN = new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_default_relaxing_image, "relaxing_image.jpg" ) );

        final ManualGroup.ManualActivity ACT_POST = new ManualGroup.ManualActivity(
                Id.create(),
                new Tag( "Please stand by..." ),
                new Duration( 1, 0 ) );

        GRP_PREV.add( ACT_PREV );
        GRP_MAIN.add( ACT_MAIN );
        GRP_POST.add( ACT_POST );

        EXPR.addGroup( GRP_PREV );
        EXPR.addGroup( GRP_MAIN );
        EXPR.addGroup( GRP_POST );

        DB.store( EXPR );
        return EXPR;
    }
}

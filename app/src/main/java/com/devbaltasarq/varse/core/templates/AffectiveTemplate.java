// Varse (c) 2019/20 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.core.templates;


import android.content.Context;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.Template;
import com.devbaltasarq.varse.core.experiment.MediaGroup;
import com.devbaltasarq.varse.core.experiment.PictureGroup;

import java.io.IOException;


public class AffectiveTemplate extends Template {
    public AffectiveTemplate(Context cntxt, Orm db, String name, TemplateStrings tags)
    {
        super( cntxt, db, name, tags );
    }

    public Experiment create()
            throws IOException
    {
        final Orm DB = this.getDb();
        final Experiment EXPR = this.getExperiment();
        final PictureGroup GRP_NEUTRAL = this.createPictureGroup(
                this.getStrs().get( TemplateStrings.TAG_NEUTRAL ),
                new Duration( 9 ) );
        final PictureGroup GRP_NASTY = this.createPictureGroup(
                this.getStrs().get( TemplateStrings.TAG_UNPLEASANT ),
                new Duration( 9 ) );
        final PictureGroup GRP_NICE = this.createPictureGroup(
                this.getStrs().get( TemplateStrings.TAG_PLEASANT ),
                new Duration( 9 ) );

        this.fillNeutralGroup( GRP_NEUTRAL );
        this.fillNiceGroup( GRP_NICE );
        this.fillNastyGroup( GRP_NASTY );

        GRP_NEUTRAL.setRandom( true );
        GRP_NASTY.setRandom( true );
        GRP_NICE.setRandom( true );

        EXPR.addGroup( GRP_NEUTRAL );
        EXPR.addGroup( GRP_NASTY );
        EXPR.addGroup( GRP_NICE );
        EXPR.setRandom( true );

        DB.store( EXPR );
        return EXPR;
    }

    private void fillNeutralGroup(PictureGroup GRP) throws IOException
    {
        final MediaGroup.MediaActivity[] PICS_NEUTRAL = {
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_neutral_01, "affective_neutral_01.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_neutral_02, "affective_neutral_02.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_neutral_03, "affective_neutral_03.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_neutral_04, "affective_neutral_04.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_neutral_05, "affective_neutral_05.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_neutral_06, "affective_neutral_06.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_neutral_07, "affective_neutral_07.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_neutral_08, "affective_neutral_08.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_neutral_09, "affective_neutral_09.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_neutral_10, "affective_neutral_10.jpg" ) )
        };

        for(final MediaGroup.MediaActivity PIC: PICS_NEUTRAL) {
            GRP.add( PIC );
        }

        return;
    }

    private void fillNastyGroup(PictureGroup GRP) throws IOException
    {
        final MediaGroup.MediaActivity[] PICS_NASTY = {
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nasty_01, "affective_nasty_01.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nasty_02, "affective_nasty_02.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nasty_03, "affective_nasty_03.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nasty_04, "affective_nasty_04.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nasty_05, "affective_nasty_05.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nasty_06, "affective_nasty_06.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nasty_07, "affective_nasty_07.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nasty_08, "affective_nasty_08.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nasty_09, "affective_nasty_09.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nasty_10, "affective_nasty_10.jpg" ) )
        };

        for(final MediaGroup.MediaActivity PIC: PICS_NASTY) {
            GRP.add( PIC );
        }

        return;
    }

    private void fillNiceGroup(PictureGroup GRP) throws IOException
    {
        final MediaGroup.MediaActivity[] PICS_NICE = {
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nice_01, "affective_nice_01.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nice_02, "affective_nice_02.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nice_03, "affective_nice_03.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nice_04, "affective_nice_04.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nice_05, "affective_nice_05.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nice_06, "affective_nice_06.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nice_07, "affective_nice_07.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nice_08, "affective_nice_08.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nice_09, "affective_nice_09.jpg" ) )
            ,
            new MediaGroup.MediaActivity(
                Id.create(),
                this.storeFileFromAssets( R.drawable.template_affective_nice_10, "affective_nice_10.jpg" ) )
        };

        for(final MediaGroup.MediaActivity PIC: PICS_NICE) {
            GRP.add( PIC );
        }

        return;
    }
}

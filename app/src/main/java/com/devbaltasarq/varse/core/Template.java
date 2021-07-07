// Varse (c) 2019/20 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.core;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import com.devbaltasarq.varse.core.experiment.ManualGroup;
import com.devbaltasarq.varse.core.experiment.PictureGroup;
import com.devbaltasarq.varse.core.experiment.Tag;
import com.devbaltasarq.varse.core.templates.AffectiveTemplate;
import com.devbaltasarq.varse.core.templates.QuickTemplate;
import com.devbaltasarq.varse.core.templates.DefaultTemplate;
import com.devbaltasarq.varse.core.templates.RuffierTemplate;


public abstract class Template {
    public enum Templates {
        Quick,
        Default,
        Affective,
        Ruffier;

        public Template create(Context cntxt, Orm db, TemplateStrings tags)
        {
            Template toret = null;
            final String TEMPLATE_NAME = this.toString().toLowerCase();

            switch ( this ) {
                case Quick:
                    toret = new QuickTemplate( cntxt, db, TEMPLATE_NAME, tags );
                    break;
                case Default:
                    toret = new DefaultTemplate( cntxt, db, TEMPLATE_NAME, tags );
                    break;
                case Affective:
                    toret = new AffectiveTemplate( cntxt, db, TEMPLATE_NAME, tags );
                    break;
                case Ruffier:
                    toret = new RuffierTemplate( cntxt, db, TEMPLATE_NAME, tags );
                    break;
                default:
                    throw new Error( "Template.createTemplate(): No class found corresponding to enum" );
            }

            return toret;
        }

        public static String[] toStringArray()
        {
            final Templates[] VALUES = values();
            final String[] TORET = new String[ VALUES.length ];

            for(int i = 0; i < TORET.length; ++i) {
                TORET[ i ] = VALUES[ i ].toString();
            }

            return TORET;
        }
    }

    public static class TemplateStrings {
        public static final int TAG_NEUTRAL = 0;
        public static final int TAG_PLEASANT = 1;
        public static final int TAG_UNPLEASANT = 2;

        public TemplateStrings()
        {
            this.strings = new HashMap<Integer, String>( 5 );
        }

        public void set(int id, String tag)
        {
            this.strings.put( id, tag );
        }

        public String get(int id)
        {
            return this.strings.get( id );
        }

        private final HashMap<Integer, String> strings;
    }


    public Template(Context cntxt, Orm db, String name, TemplateStrings tags)
    {
        this.tags = tags;
        this.db = db;
        this.context = cntxt;
        this.experiment = new Experiment( Id.create(), name );
    }

    public Experiment getExperiment()
    {
        return this.experiment;
    }

    public Orm getDb()
    {
        return this.db;
    }

    public TemplateStrings getTags()
    {
        return this.tags;
    }

    public Context getContext()
    {
        return this.context;
    }

    public abstract Experiment create() throws IOException;

    /** Gets a file available in the assets and stores as part of the experiment.
      * @param imageId the id identifying the file resource in the assets.
      * @param fileName the desired basic name for the file in the store.
      * @return a file in the app's store.
      * @throws IOException if an error occurs storing the asset.
     */
    protected File storeFileFromAssets(int imageId, String fileName) throws IOException
    {
        final Drawable IMG = ContextCompat.getDrawable( this.getContext(), imageId );
        final Bitmap BITMAP = ( (BitmapDrawable) IMG ).getBitmap();
        final ByteArrayOutputStream BIT_OUT_STREAM = new ByteArrayOutputStream();
        final String FILE_NAME = Orm.buildMediaFileNameForDbFromMediaFileName( fileName );

        BITMAP.compress( Bitmap.CompressFormat.JPEG, 100, BIT_OUT_STREAM );
        final ByteArrayInputStream BIT_IN_STREAM = new ByteArrayInputStream( BIT_OUT_STREAM.toByteArray() );

        return this.getDb().storeMedia( this.getExperiment(), FILE_NAME, BIT_IN_STREAM );
    }

    /** Just a convenience method.
      * @param strId the id of the string in the assets.
      * @return the string identified by the parameter.
      */
    protected String getString(int strId)
    {
        return this.getContext().getString( strId );
    }

    /** @return a newly created manual group **/
    protected ManualGroup createManualGroup()
    {
        return new ManualGroup( Id.create(), this.getExperiment() );
    }

    /** Creates a new picture group.
      * @param tag the string tag for this group.
      * @param duration the time each picture will be exposed.
      * @return a newly created picture group
      */
    protected PictureGroup createPictureGroup(String tag, Duration duration)
    {
        return new PictureGroup( Id.create(), new Tag( tag ), duration, this.getExperiment() );
    }

    private final TemplateStrings tags;
    private final Experiment experiment;
    private final Context context;
    private final Orm db;
}

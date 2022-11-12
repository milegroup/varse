package com.devbaltasarq.varse.core.ofmcache;


import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.core.PlainStringEncoder;

import java.io.File;


/** Entities cache: dividing the files according the corresponding 'types'. */
public class EntitiesCache {
    public enum Entity { Experiments, Results }
    private static final String FIELD_EXT = "ext";
    public static final String FIELD_EXPERIMENT_ID = "experiment_id";
    public static final String FIELD_EXPERIMENT = "experiment";
    public static final String FIELD_REC = "rec";

    public EntitiesCache()
    {
        final int NUM_CACHES = Entity.values().length;

        this.caches = new FileCache[ NUM_CACHES ];

        for(int i = 0; i < NUM_CACHES; ++i) {
            this.caches[ i ] = new FileCache();
        }
    }

    /** Removes all entries in all caches. */
    public void clear()
    {
        for (FileCache fileCache : this.caches) {
            fileCache.clear();
        }

        return;
    }

    /** @return a given cache
      * @param type a value determining the cache to obtain.
      * @see Persistent::TypeId
      */
    public FileCache get(Persistent.TypeId type)
    {
        return this.get( EntityFromType( type ) );
    }

    /** @return a given cache
      * @param entity a value determining the cache to obtain.
      */
    public FileCache get(Entity entity)
    {
        return this.caches[ entity.ordinal() ];
    }

    /** @return the experiments cache. */
    public FileCache getExperiments()
    {
        return this.get( Entity.Experiments );
    }

    /** @return the results cache. */
    public FileCache getResults()
    {
        return this.get( Entity.Results );
    }

    /** @return gets an entity from a persistent type.
      * @param type a value of the persistent type, to be converted.
      * @see Persistent::TypeId
      */
    public static Entity EntityFromType(Persistent.TypeId type)
    {
        Entity toret;

        switch ( type ) {
            case Experiment:
                toret = Entity.Experiments;
                break;
            case Result:
                toret = Entity.Results;
                break;
            default:
                throw new IllegalArgumentException( "no cache entity for type: " + type );
        }

        return toret;
    }

    /** Decide the type for a file, following the extension.
      * @param f the file to decide the type for.
      * @return the typeid for that file.
      * @see Persistent::TypeId
      */
    public static Persistent.TypeId getTypeIdForExt(File f)
    {
        final String EXT_EXPERIMENT = getFileExtFor( Persistent.TypeId.Experiment );
        final String EXT_RESULT = getFileExtFor( Persistent.TypeId.Result );
        Persistent.TypeId toret = null;

        if ( f.getName().endsWith( EXT_EXPERIMENT ) ) {
            toret = Persistent.TypeId.Experiment;
        }
        else
        if ( f.getName().endsWith( EXT_RESULT ) ) {
            toret = Persistent.TypeId.Result;
        }

        return toret;
    }

    /** Returns the extension for the corresponding type.
      * Note that it will be of three chars, lowercase.
      *
      * @param typeId the typeId id of the object.
      * @return the corresponding extension, as a string.
      * @see Persistent
      */
    public static String getFileExtFor(Persistent.TypeId typeId)
    {
        return typeId.toString().substring( 0, 3 ).toLowerCase();
    }

    private static final String REGULAR_FILE_FORMAT =
            Id.FILE_NAME_PART
                    + FileCache.FILE_NAME_PART_SEPARATOR + "$" + Id.FILE_NAME_PART
                    + ".$" + FIELD_EXT;

    /** @return the file name for the an entity, given its id and type.
      * @param id The id object representing the if of the file.
      * @param typeId the type of the object.
      */
    public static String buildFileNameFor(Id id, Persistent.TypeId typeId)
    {
        return REGULAR_FILE_FORMAT
                .replace( "$" + Id.FILE_NAME_PART, id.toString() )
                .replace( "$" + FIELD_EXT, EntitiesCache.getFileExtFor( typeId ) );
    }

    private static final String RESULT_FILE_FORMAT =
            Id.FILE_NAME_PART
                    + FileCache.FILE_NAME_PART_SEPARATOR + "$" + Id.FILE_NAME_PART
                    + FileCache.FILE_NAME_PART_SEPARATOR + FIELD_EXPERIMENT
                    + FileCache.FILE_NAME_PART_SEPARATOR + "$" + FIELD_EXPERIMENT
                    + FileCache.FILE_NAME_PART_SEPARATOR + FIELD_REC
                    + FileCache.FILE_NAME_PART_SEPARATOR + "$" + FIELD_REC
                    + FileCache.FILE_NAME_PART_SEPARATOR + FIELD_EXPERIMENT_ID
                    + FileCache.FILE_NAME_PART_SEPARATOR + "$" + FIELD_EXPERIMENT_ID
                    + ".$" + FIELD_EXT;

    /** @return the name for a the corresponding result file.
      * @param id The id of the result file.
      * @param exprId The experiment id for the result data set file.
      * @param experimentName the name of the experiment.
      * @param rec the name of the record.
      */
    public static String buildFileNameForResult(Id id, Id exprId, String experimentName, String rec)
    {
        return RESULT_FILE_FORMAT
                .replace( "$" + FIELD_EXT, EntitiesCache.getFileExtFor( Persistent.TypeId.Result ) )
                .replace( "$" + Id.FILE_NAME_PART, id.toString() )
                .replace( "$" + FIELD_EXPERIMENT_ID, exprId.toString() )
                .replace( "$" + FIELD_REC, rec )
                .replace( "$" + FIELD_EXPERIMENT, PlainStringEncoder.get().encode( experimentName ) );
    }

    /** Any result file name contains the user's and experiment's ids.
     * Format: id-xxx-user_id-yyy-experiment_id-zzz.res
     * @param f The file to extract the experiment id from.
     * @return A long number corresponding to the id.
     */
    public static long parseExperimentIdFromResultFileName(File f)
    {
        return FileCache.parseIdFromFileNamePart( f, FIELD_EXPERIMENT_ID );
    }

    private final FileCache[] caches;
}

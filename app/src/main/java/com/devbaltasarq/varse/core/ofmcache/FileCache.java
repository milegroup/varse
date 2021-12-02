package com.devbaltasarq.varse.core.ofmcache;


import com.devbaltasarq.varse.core.Id;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/** A generic cache for files and the Id's in their names. */
public class FileCache {
    public static final String FILE_NAME_PART_SEPARATOR = "-";
    public static final String FILE_NAME_PART_MEDIA = "media";

    public FileCache()
    {
        this.fileIdMap = new HashMap<>();
    }

    /** Adds a new file to the cache. This works by retrieving the file name
      * and parsing its id.
      * @param f the file to add.
      * @see FileCache::parseIdFromFile
      */
    public void add(File f)
    {
        if ( f != null ) {
            final Id ID = new Id( parseIdFromFileName( f ) );

            if ( this.fileIdMap.get( ID ) == null ) {
                this.fileIdMap.put( ID, f );
            }
        }

        return;
    }

    /** Gets the file from the cache related to a given Id.
      * @param id The id to retrieve the file from.
      * @return A file object, or null if not found.
      */
    public File get(Id id)
    {
        return this.fileIdMap.get( id );
    }

    /** @return all the files in this cache. */
    public File[] getValues()
    {
        return this.fileIdMap.values().toArray( new File[ 0 ] );
    }

    /** @return the number of files stored. */
    public int count()
    {
        return this.fileIdMap.size();
    }

    /** Removes all entries. */
    public void clear()
    {
        this.fileIdMap.clear();
    }

    /** Removes a given entry. */
    public void remove(File f)
    {
        this.fileIdMap.remove( new Id( parseIdFromFileName( f ) ) );
    }

    /** @return whether a file is stored in the cache. */
    public boolean contains(File f)
    {
        return this.fileIdMap.containsKey( new Id( parseIdFromFileName( f ) ) );
    }

    /** Extracts the id from the root media dir for a given experiment.
     * Whatever this file is, the id between the '.' for the ext and the '-' separator.
     * @param file the file to extract the id from.
     * @return A long with the id.
     * @throws Error if the file name does not contain an id.
     */
    public static long parseIdFromMediaDir(File file)
    {
        final String FILE_NAME = removeFileNameExt( file.getName() );
        long toret;

        // Divide by separator
        final String[] PARTS = FILE_NAME.split( FILE_NAME_PART_SEPARATOR );

        // Chk basic formatting
        if ( PARTS.length >= 2
          && PARTS[ 0 ].equals( FILE_NAME_PART_MEDIA ) )
        {
            // Convert second part to long for the id.
            try {
                toret = Long.parseLong( PARTS[ 1 ] );
            } catch(NumberFormatException exc) {
                throw new Error( "parseIdFromFile: malformed id: " + PARTS[ 1 ] );
            }
        } else {
            throw new Error( "parseIdFromMediaDir: wrong file name formatting: " + FILE_NAME );
        }

        return toret;
    }

    /** Parses a file name for a given time.
      * Format: FIELD-xxxx, it is 'xxxx' what is returned.
      * @param f the file to extract the experiment id from.
      * @param FIELD a string containing the field name.
      * @return A string containing the value of the field.
      */
    public static String parsePartFromFileName(File f, final String FIELD)
    {
        return parsePartFromFileName( removeFileNameExt( f.getName() ), FIELD );
    }

    /** Parses a file name for a given time.
      * Format: FIELD-xxxx, it is 'xxxx' what is returned.
      * @param FILE_NAME the file to extract the experiment id from.
      * @param FIELD a string containing the field name.
      * @return A string containing the value of the field.
      */
    public static String parsePartFromFileName(final String FILE_NAME, final String FIELD)
    {
        int startPos = FILE_NAME.indexOf( FIELD );
        String toret = "";

        if ( startPos >= 0 ) {
            startPos += 1 + FIELD.length();
            int endPos = FILE_NAME.indexOf( FILE_NAME_PART_SEPARATOR, startPos );

            if ( endPos < 0 ) {
                endPos = FILE_NAME.length();
            }

            toret = FILE_NAME.substring( startPos, endPos );
        } else {
            throw new Error( "parsePartFromFileName: wrong formatting for file name: " + FILE_NAME );
        }

        return toret;
    }

    /** Parses a file name for a given id.
      * Format: FIELD_ID-xxxx, it is 'xxxx' what is returned.
      * @param f the file to extract the experiment id from.
      * @param PART_ID a string containing the field name.
      * @return A long containing the id.
      */
    public static long parseIdFromFileNamePart(File f, final String PART_ID)
    {
        return parseIdFromFileNamePart( FileCache.removeFileNameExt( f.getName() ), PART_ID );
    }

    /** Parses a file name for a given id.
      * Format: FIELD_ID-xxxx, it is 'xxxx' what is returned.
      * @param FILE_NAME the file name to extract the experiment id from.
      * @param PART_ID a string containing the field name.
      * @return A long containing the id.
      */
    public static long parseIdFromFileNamePart(final String FILE_NAME, final String PART_ID)
    {
        final String STR_ID = parsePartFromFileName( FILE_NAME, PART_ID );
        long toret;

        try {
            toret = Long.parseLong( STR_ID );
        } catch(NumberFormatException exc) {
            throw new Error( "parseIdFromFileNamePart: malformed id(" + PART_ID
                              + "): " + STR_ID );
        }

        return toret;
    }

    public static long parseIdFromFileName(File f)
    {
        return parseIdFromFileNamePart( f, Id.FILE_NAME_PART );
    }

    /** @return the given file name, with its extension removed.
      * @param FILE_NAME a file name, with or without extension. */
    public static String removeFileNameExt(final String FILE_NAME)
    {
        int extSeparatorPos = FILE_NAME.lastIndexOf( '.' );

        if ( extSeparatorPos < 0 ) {
            extSeparatorPos = FILE_NAME.length();
        }

        return FILE_NAME.substring( 0, extSeparatorPos );
    }

    private final Map<Id, File> fileIdMap;
}

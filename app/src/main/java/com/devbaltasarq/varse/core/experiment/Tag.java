package com.devbaltasarq.varse.core.experiment;

import com.devbaltasarq.varse.core.PlainStringEncoder;
import com.devbaltasarq.varse.core.ofmcache.FileCache;

/** Represents a Tag.
  * Depends on FileNameAdapter, in order to adapt tags.
  * @see PlainStringEncoder
  */
public class Tag {
    public static final String FIELD = "tag";
    public static Tag NO_TAG = new Tag( "Neutral" );

    /** Creates a new tag. */
    public Tag(String tag)
    {
        if ( plainStringEncoder == null ) {
            plainStringEncoder = PlainStringEncoder.get();
        }

        this.tag = encode( tag );
    }

    @Override
    public int hashCode()
    {
        return this.tag.hashCode();
    }

    public boolean equals(Object o)
    {
        boolean toret = false;

        if ( o instanceof Tag ) {
            Tag other = (Tag) o;

            toret = this.tag.equals( other.tag );
        }

        return toret;
    }

    /** Changes the contents for this Tag.
      * @param newTag A string containing the new contents of the Tag.
      */
    public void set(String newTag)
    {
        this.tag = encode( newTag );
    }

    /** @return a string formatted as a tag needs to accomplish with standards.
      * Note that the '_' must not be allowed, it is used as a separator
      * for file parts identifying different id's.
      * @see PlainStringEncoder , which converts the tag appropriately. */
    public String encode(String tag)
    {
        return plainStringEncoder.encode( tag ).replace(
                                        FileCache.FILE_NAME_PART_SEPARATOR,
                                        "_" );
    }

    /** @return the tag in an user-readable format. */
    public String getHumanReadable()
    {
        String toret = this.tag.replace( '_', ' ' );

        return Character.toUpperCase( toret.charAt( 0 ) ) + toret.substring( 1 );
    }

    /** @return a copy of the tag text, in a different object. */
    public Tag copy()
    {
        return new Tag( this.tag );
    }

    /** @return the tag in a NON user-readable format. */
    @Override
    public String toString()
    {
        return this.tag;
    }

    private String tag;
    private static PlainStringEncoder plainStringEncoder;
}

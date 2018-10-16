package com.devbaltasarq.varse.core.experiment;

import com.devbaltasarq.varse.core.FileNameAdapter;

/** Represents a Tag.
  * Depends on FileNameAdapter, in order to adapt tags.
  * @see FileNameAdapter
  */
public class Tag {
    public static final String FIELD = "tag";
    public static Tag NO_TAG = new Tag( "Neutral" );

    /** Creates a new tag. */
    public Tag(String tag)
    {
        if ( fileNameAdapter == null ) {
            fileNameAdapter = FileNameAdapter.get();
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
      * @see FileNameAdapter, which converts the tag appropriately. */
    public String encode(String tag)
    {
        return fileNameAdapter.encode( tag );
    }

    /** @return a copy of the tag text, in a different object. */
    public Tag copy()
    {
        return new Tag( this.tag );
    }

    /** Gets the tag in an user-readable format. */
    @Override
    public String toString()
    {
        return this.tag;
    }

    private String tag;
    private static FileNameAdapter fileNameAdapter;
}

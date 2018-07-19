package com.devbaltasarq.varse.core.experiment;

/** Represents a Tag. */
public class Tag {
    public static final String FIELD = "tag";
    public static Tag NO_TAG = new Tag( "Neutral" );

    /** Creates a new tag. */
    public Tag(String tag)
    {
        this.tag = this.encode( tag );
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

    /** Formats a tag to accomplish with standards. */
    private String encode(String tag)
    {
        if ( tag.trim().isEmpty() ) {
            throw new Error( "empty tag" );
        }

        return tag.trim().toLowerCase().replace( ' ', '_' );
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
        String toret = this.tag;

        if ( toret.length() > 1 ) {
            toret = Character.toUpperCase( this.tag.charAt( 0 ) ) + this.tag.substring( 1 );
        }

        return toret.replace( '_', ' ' );
    }

    private String tag;
}

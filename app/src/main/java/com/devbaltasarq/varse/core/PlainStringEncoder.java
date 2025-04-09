// Varse (c) 2019/20 - 25 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.core;


/** Converts tags so fits our needs: no spaces, lowercase,
  * only numbers, letters, and underscores ('_').
  */
public class PlainStringEncoder {
    private PlainStringEncoder()
    {
    }

    /** Appropriately encodes a tag name.
      * - No spaces.
      * - Lowercase.
      * - No hyphens, since this is used in the datastore's file names.
      * @param initialTag the initial contents for the tag.
      * @return the final version of the tag.
      * @throws Error if the tag is empty or does only contain spaces.
     */
    public String encode(String initialTag)
    {
        final String BASIC_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789_";
        final StringBuilder TORET = new StringBuilder( initialTag.length() );

        if ( initialTag.trim().isEmpty() ) {
            throw new Error( "empty tag??" );
        }

        for(char ch: initialTag.trim().toLowerCase().toCharArray()) {
            if ( BASIC_ALPHABET.indexOf( ch ) >= 0 ) {
                TORET.append( ch );
            }
            else {
                TORET.append( '_' );
            }
        }

        return TORET.toString();
    }

    /** @return the only instance of this class. */
    public static PlainStringEncoder get()
    {
        if ( instance == null ) {
            instance = new PlainStringEncoder();
        }

        return instance;
    }

    private static PlainStringEncoder instance;
}

package com.devbaltasarq.varse.core;

/** This class is needed since Android 5.0 does not support lambdas.
  * Converts tags so fits our needs: no spaces, lowercase,
  * only numbers, letters, and hyphens ('-' and '_').
  */
public class PlainStringEncoder {
    private PlainStringEncoder()
    {
    }

    /** Appropiately encodes a tag name.
      * - No spaces.
      * - Lowercase.
      * - Hyphens allowed ('_' and '-').
      * @param initialTag the initial contents for the tag.
      * @return the final version of the tag.
      * @throws Error if the tag is empty or does only contain spaces.
     */
    public String encode(String initialTag)
    {
        final String BASIC_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789-_";
        final StringBuilder TORET = new StringBuilder();

        if ( initialTag.trim().isEmpty() ) {
            throw new Error( "empty tag" );
        }

        initialTag = initialTag.trim().toLowerCase();

        for(char ch: initialTag.toCharArray()) {
            if ( BASIC_ALPHABET.indexOf( ch ) >= 0 ) {
                TORET.append( ch );
            }
            else
            if ( ch == ' ' ) {
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

package com.devbaltasarq.varse.core;

/** This class is needed since Android 5.0 does not support lambdas.
  * Converts tags so fits our needs: no spaces, lowercase.
  */
public class FileNameAdapter {
    private FileNameAdapter()
    {
    }

    /** Appropiately encodes a tag name.
      * - No spaces.
      * - Lowercase.
      * @param initialTag the initial contents for the tag.
      * @return the final version of the tag.
      * @throws Error if the tag is empty or does only contain spaces.
     */
    public String encode(String initialTag)
    {
        if ( initialTag.trim().isEmpty() ) {
            throw new Error( "empty tag" );
        }

        return initialTag.trim().toLowerCase().replace( ' ', '_' );
    }

    /** @return the only instance of this class. */
    public static FileNameAdapter get()
    {
        if ( instance == null ) {
            instance = new FileNameAdapter();
        }

        return instance;
    }

    private static FileNameAdapter instance;
}

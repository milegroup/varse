package com.devbaltasarq.varse.core;


/** Gives an id and its infrastructure to any other class. */
public class Id {
    public static final int START_CREATED_IDS = -100;
    public static final String FIELD = "_id";

    /** Creates a new id. */
    public Id(long id)
    {
        this.id = id;
    }

    /** @return the id itself. */
    public long get()
    {
        return this.id;
    }

    /** @return the hashcode. */
    @Override
    public int hashCode()
    {
        return Long.valueOf( this.get() ).hashCode();
    }

    /** Determine whether this is equal to another id or not. */
    @Override
    public boolean equals(Object o)
    {
        boolean toret = false;

        if ( o instanceof Id ) {
            return this.get() == ( (Id) o ).get();
        }

        return toret;
    }

    /** @return true if the identifier is valid, false otherwise. */
    public boolean isValid()
    {
        return ( !this.isFake() );
    }

    /** Copies the id into a different object.
      * @return A new object with the same id of this one.*/
    public Id copy()
    {
        return new Id( this.id );
    }

    @Override
    public String toString()
    {
        return Long.toString( this.get() );
    }

    /** @return true if this id is fake, created for the occasion, or false if retrieved from db. */
    public boolean isFake()
    {
        return ( this.get() < 0 );
    }

    /** Creates a random id. */
    public static Id createFake()
    {
        return new Id( nextFakeId() );
    }

    /** @return The next id, valid for storing. */
    public static Id create()
    {
        ++nextId;
        return new Id( nextId );
    }

    /** Generates the next Id. Generates negative high Id's to avoid clashes with real data. */
    private static long nextFakeId()
    {
        return ( nextFakeId -= 1 );
    }

    private long id;
    private static long nextFakeId = START_CREATED_IDS + 1;
    private static long nextId = System.currentTimeMillis();
}

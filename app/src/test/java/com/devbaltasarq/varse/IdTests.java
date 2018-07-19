package com.devbaltasarq.varse;

import com.devbaltasarq.varse.core.Id;

import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/** Tests the correctness of the Id library */
public class IdTests {
    @BeforeClass
    public static void init()
    {
        id1 = Id.createFake();
        id2 = Id.createFake();
        id3 = Id.createFake();
        id4 = Id.createFake();
        id42 = new Id( 42 );

        assertEquals( Id.START_CREATED_IDS, id1.get() );
        assertEquals( Id.START_CREATED_IDS - 1, id2.get() );
        assertEquals( Id.START_CREATED_IDS - 2, id3.get() );
        assertEquals( Id.START_CREATED_IDS - 3, id4.get() );
        assertEquals( 42, id42.get() );
    }

    @Test
    public void testIdCreation()
    {
        assertTrue( id1.isFake() ); assertFalse( id1.isValid() );
        assertTrue( id2.isFake() ); assertFalse( id2.isValid() );
        assertTrue( id3.isFake() ); assertFalse( id3.isValid() );
        assertTrue( id4.isFake() ); assertFalse( id4.isValid() );
        assertFalse( id42.isFake() ); assertTrue( id42.isValid() );
    }

    @Test
    public void testIdToString()
    {
        assertEquals( Long.toString( id1.get() ), id1.toString() );
        assertEquals( Long.toString( id2.get() ), id2.toString() );
        assertEquals( Long.toString( id3.get() ), id3.toString() );
        assertEquals( Long.toString( id4.get() ), id4.toString() );
        assertEquals( Long.toString( id42.get() ), id42.toString() );
    }

    private static Id id1;
    private static Id id2;
    private static Id id3;
    private static Id id4;
    private static Id id42;
}

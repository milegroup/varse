package com.devbaltasarq.varse;

import com.devbaltasarq.varse.core.Id;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


/** Tests the correctness of the Id class */
public class IdTests {
    @BeforeClass
    public static void init()
    {
        id1 = Id.createFake();
        id2 = Id.createFake();
        id3 = Id.createFake();
        id4 = Id.createFake();
        id42 = new Id( 42 );

        Assert.assertEquals( Id.START_CREATED_IDS, id1.get() );
        Assert.assertEquals( Id.START_CREATED_IDS - 1, id2.get() );
        Assert.assertEquals( Id.START_CREATED_IDS - 2, id3.get() );
        Assert.assertEquals( Id.START_CREATED_IDS - 3, id4.get() );
        Assert.assertEquals( 42, id42.get() );
    }

    @Test
    public void testIdCreation()
    {
        Assert.assertTrue( id1.isFake() ); Assert.assertFalse( id1.isValid() );
        Assert.assertTrue( id2.isFake() ); Assert.assertFalse( id2.isValid() );
        Assert.assertTrue( id3.isFake() ); Assert.assertFalse( id3.isValid() );
        Assert.assertTrue( id4.isFake() ); Assert.assertFalse( id4.isValid() );
        Assert.assertFalse( id42.isFake() ); Assert.assertTrue( id42.isValid() );
    }

    @Test
    public void testIdToString()
    {
        Assert.assertEquals( Long.toString( id1.get() ), id1.toString() );
        Assert.assertEquals( Long.toString( id2.get() ), id2.toString() );
        Assert.assertEquals( Long.toString( id3.get() ), id3.toString() );
        Assert.assertEquals( Long.toString( id4.get() ), id4.toString() );
        Assert.assertEquals( Long.toString( id42.get() ), id42.toString() );
    }

    private static Id id1;
    private static Id id2;
    private static Id id3;
    private static Id id4;
    private static Id id42;
}

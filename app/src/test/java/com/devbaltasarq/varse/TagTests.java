package com.devbaltasarq.varse;

import com.devbaltasarq.varse.core.experiment.Tag;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class TagTests {
    private static final String TagHello = "hello";
    private static final String TagHelloWorld = "hello world";
    private static final String TagABraveNewWorld = "a brave new world";

    private static final String TagCodedHello = "hello";
    private static final String TagCodedHelloWorld = "hello_world";
    private static final String TagCodedABraveNewWorld = "a_brave_new_world";

    private static final String TagReadableHello = "Hello";
    private static final String TagReadableHelloWorld = "Hello world";
    private static final String TagReadableABraveNewWorld = "A brave new world";

    @BeforeClass
    public static void init()
    {
        tag1 = new Tag( TagHello );
        tag2 = new Tag( TagHelloWorld );
        tag3 = new Tag( TagABraveNewWorld );
    }

    @Test
    public void testEmptyTag()
    {
        try {
            new Tag( "" );
            Assert.fail( "No 'empty tag' exception launched" );
        } catch(Error err)
        {
            Assert.assertTrue( "empty tag".contains( err.getMessage() ) );
        }
    }

    @Test
    public void testSimpleTag()
    {
        Assert.assertEquals( TagCodedHello, tag1.toString() );
        Assert.assertEquals( TagReadableHello, tag1.getHumanReadable() );
        Assert.assertNotEquals( tag1.getHumanReadable(), tag1.toString() );
    }

    @Test
    public void testCompoundTag()
    {
        Assert.assertEquals( TagCodedHelloWorld, tag2.toString() );
        Assert.assertEquals( TagReadableHelloWorld, tag2.getHumanReadable() );
        Assert.assertNotEquals( tag2.getHumanReadable(), tag2.toString() );
    }

    @Test
    public void testComplexTag()
    {
        Assert.assertEquals( TagCodedABraveNewWorld, tag3.toString() );
        Assert.assertEquals( TagReadableABraveNewWorld, tag3.getHumanReadable() );
        Assert.assertNotEquals( tag3.getHumanReadable(), tag3.toString() );
    }

    private static Tag tag1;
    private static Tag tag2;
    private static Tag tag3;
}

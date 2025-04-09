package com.devbaltasarq.varse;


import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.core.ofm.FileManager;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;


/** Test the correctness of file name parsing. */
public class FileNameParsingTest {
    private static final long EXP_ID = 111222;
    private static final long RES_ID = 5656;

    @BeforeClass
    public static void init()
    {
        expFile = new File( FileManager.buildFileNameFor(
                                Persistent.TypeId.Experiment,
                                new Id( EXP_ID )

        ));

        resFile = new File( FileManager.buildFileNameFor(
                                    Persistent.TypeId.Result,
                                    new Id( RES_ID ) ) );

        dirFile = new File( "media-" + EXP_ID );
    }

    @Test
    public void testFileExtensions()
    {
        Assert.assertTrue(
                expFile.getName().endsWith(
                        FileManager.getFileExtFor( Persistent.TypeId.Experiment ) ) );

        Assert.assertTrue(
                resFile.getName().endsWith(
                        FileManager.getFileExtFor( Persistent.TypeId.Result ) ) );
    }

    @Test
    public void testMoreFiletExtensions()
    {
        Assert.assertEquals( Persistent.TypeId.Experiment,
                        FileManager.getTypeIdForExt( expFile ) );

        Assert.assertEquals( Persistent.TypeId.Result,
                        FileManager.getTypeIdForExt( resFile ) );
    }

    @Test
    public void testParseName()
    {
        Assert.assertEquals( EXP_ID, FileManager.parseIdFromMediaDir( dirFile ) );
    }

    private static File expFile;
    private static File resFile;
    private static File dirFile;
}

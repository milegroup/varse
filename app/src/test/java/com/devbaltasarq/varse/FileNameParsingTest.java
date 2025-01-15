package com.devbaltasarq.varse;


import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.core.ofmcache.FileCache;
import com.devbaltasarq.varse.core.ofmcache.EntitiesCache;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;


/** Test the correctness of file name parsing. */
public class FileNameParsingTest {
    private static final long EXP_ID = 111222;
    private static final long USR_ID = 34;
    private static final long RES_ID = 5656;

    @BeforeClass
    public static void init()
    {
        expFile = new File( EntitiesCache.buildFileNameFor(
                                new Id( EXP_ID ),
                                Persistent.TypeId.Experiment
        ));

        resFile = new File( EntitiesCache.buildFileNameForResult(
                                new Id( RES_ID ),
                                new Id( EXP_ID ),
                                "usr1",
                                "r0"
        ));

        dirFile = new File( "media-" + EXP_ID );
    }

    @Test
    public void testFileExtensions()
    {
        Assert.assertTrue(
                expFile.getName().endsWith(
                        EntitiesCache.getFileExtFor( Persistent.TypeId.Experiment ) ) );

        Assert.assertTrue(
                resFile.getName().endsWith(
                        EntitiesCache.getFileExtFor( Persistent.TypeId.Result ) ) );
    }

    @Test
    public void testMoreFiletExtensions()
    {
        Assert.assertEquals( Persistent.TypeId.Experiment,
                             EntitiesCache.getTypeIdForExt( expFile ) );

        Assert.assertEquals( Persistent.TypeId.Result,
                            EntitiesCache.getTypeIdForExt( resFile ) );
    }

    @Test
    public void testParseName()
    {
        Assert.assertEquals( EXP_ID, FileCache.parseIdFromFileName( expFile ) );
        Assert.assertEquals( USR_ID, FileCache.parseIdFromFileName( usrFile ) );
        Assert.assertEquals( RES_ID, FileCache.parseIdFromFileName( resFile ) );
        Assert.assertEquals( EXP_ID, FileCache.parseIdFromMediaDir( dirFile ) );
    }

    @Test
    public void testResultParseParts()
    {
        Assert.assertEquals(
                EXP_ID,
                EntitiesCache.parseExperimentIdFromResultFileName( resFile )
        );

    }

    private static File expFile;
    private static File usrFile;
    private static File resFile;
    private static File dirFile;
}
